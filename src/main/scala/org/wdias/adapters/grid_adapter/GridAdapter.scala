package org.wdias.adapters.grid_adapter

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify}
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes.{Created, InternalServerError}
import akka.pattern.pipe
import akka.util.Timeout
import com.paulgoldbaum.influxdbclient.Parameter.Precision
import org.wdias.adapters.grid_adapter.GridAdapter._
import org.wdias.adapters.scalar_adapter.ScalarAdapter.{GetTimeSeries, StoreTimeSeries, StoreTimeseriesResponse}
import org.wdias.extensions.ExtensionHandler.ExtensionHandlerData
import ucar.ma2.DataType
import ucar.nc2.{Attribute, Dimension}

import scala.concurrent.Future
// Check to Run in Actor Context
import java.nio.file.{Files, Paths}
import java.util

import com.paulgoldbaum.influxdbclient.{InfluxDB, Point, QueryResult, Record}
import org.wdias.constant._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object GridAdapter {

  case class StoreTimeSeries(timeSeries: TimeSeries)

  case class GetTimeSeries(metadataObj: MetadataObj)

  case class StoreTimeseriesResponse(statusCode: StatusCode, metadataIds: Option[MetadataIds] = Option(null), message: Option[String] = Option(null))

  case class GetTimeseriesResponse(statusCode: StatusCode, timeseries: Option[TimeSeries] = Option(null), message: Option[String] = Option(null))

  // TODO: To be removed
  case class StoreValidatedTimeSeries(timeSeriesEnvelop: TimeSeries)

  case class StoreSuccess(metadata: Metadata)

  case class Result(timeSeriesEnvelop: TimeSeries)

  case class StoreFailure()

}

class GridAdapter extends Actor with ActorLogging {


  implicit val timeout: Timeout = Timeout(15 seconds)

  var metadataAdapterRef: ActorRef = _
  context.actorSelection("/user/metadataAdapter") ! Identify(None)
  var statusHandlerRef: ActorRef = _
  context.actorSelection("/user/statusHandler") ! Identify(None)

  def createResponse(query: MetadataObj, result: QueryResult): TimeSeries = {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    var points: List[DataPoint] = List()
    val timeSeries: TimeSeries = TimeSeries(query.timeSeriesId, query.toMetadataIds.toMetadataIdsObj)
    if(result.series.nonEmpty) {
      val records: List[Record] = result.series.head.records
      val valueIndex = result.series.head.columns.indexOf("value")
      records.foreach { record =>
        log.info(record.allValues.toString())
        val dateTimeStr: String = record.allValues(0).toString.split('Z')(0)
        val dateTime = LocalDateTime.parse(dateTimeStr)
        val value: Double = record.allValues(valueIndex).toString.toDouble
        timeSeries.addDataPoint(DataPoint(dateTime.format(formatter)).addValue(value))
      }
    }
    println("Created Response TimeSeries")
    timeSeries
  }

  def createNetcdfFile(): Unit = {
    val location = "/tmp/testWrite.nc"

    if(Files.exists(Paths.get(location))) {
      return null
    }
    println("File does not exists. Create new ", location)
    import ucar.nc2.NetcdfFileWriter
    val writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, location, null)

    // add dimensions
    val latDim = writer.addDimension(null, "lat", 64)
    val lonDim = writer.addDimension(null, "lon", 128)

    // add Variable double temperature(lat,lon)
    val dims = new util.ArrayList[Dimension]
    dims.add(latDim)
    dims.add(lonDim)

    val t = writer.addVariable(null, "temperature", DataType.DOUBLE, dims)
    t.addAttribute(new Attribute("units", "K")) // add a 1D attribute of length 3
    import ucar.ma2.{Array => NetcdfArray}
    val data = NetcdfArray.factory(classOf[Int], Array[Int](3), Array[Int](1, 2, 3))
    t.addAttribute(new Attribute("scale", data))

    // add a string-valued variable: char svar(80)
    val svar_len: Dimension = writer.addDimension(null, "svar_len", 80)
    writer.addVariable(null, "svar", DataType.CHAR, "svar_len")

    // add a 2D string-valued variable: char names(names, 80)
    val names: Dimension = writer.addDimension(null, "names", 3)
    writer.addVariable(null, "names", DataType.CHAR, "names svar_len")

    // add a scalar variable
    writer.addVariable(null, "scalar", DataType.DOUBLE, new util.ArrayList[Dimension])

    // add global attributes
    writer.addGroupAttribute(null, new Attribute("yo", "face"))
    writer.addGroupAttribute(null, new Attribute("versionD", 1.2))
    writer.addGroupAttribute(null, new Attribute("versionF", 1.2.toFloat))
    writer.addGroupAttribute(null, new Attribute("versionI", 1))
    writer.addGroupAttribute(null, new Attribute("versionS", 2.toShort))
    writer.addGroupAttribute(null, new Attribute("versionB", 3.toByte))

    // create the file
    try {
      writer.create()
    } catch {
      case e: Exception => System.err.printf("ERROR creating file %s%n%s", location, e.getMessage);
    }
    writer.close()
    null
  }

  def receive: Receive = {
    case StoreTimeSeries(timeSeries: TimeSeries) =>
      log.info("StoringTimeSeries... {}", sender())
      val influxdb = InfluxDB.connect("localhost", 8086)
      val database = influxdb.selectDatabase("wdias")
      val metadataIdsObj: MetadataIdsObj = timeSeries.metadataIdsObj
      var points: List[Point] = List()
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
      val zoneId = ZoneId.systemDefault
      timeSeries.data.foreach { tt: DataPoint =>
        val dateTime: LocalDateTime = LocalDateTime.parse(tt.time, formatter)
        val p = Point("observed", dateTime.atZone(zoneId).toEpochSecond())
          // Tags
          .addTag("moduleId", metadataIdsObj.moduleId)
          .addTag("valueType", metadataIdsObj.valueType.toString)
          .addTag("parameterId", metadataIdsObj.parameterId)
          .addTag("locationId", metadataIdsObj.locationId)
          .addTag("timeSeriesType", metadataIdsObj.timeSeriesType.toString)
          .addTag("timeStepId", metadataIdsObj.timeStepId)
          // Values
          .addField("value", tt.value.get)

        points = points :+ p
      }
      log.info("Created points {}", points)

      pipe(database.bulkWrite(points, precision = Precision.SECONDS).mapTo[Boolean] map { isWritten =>
        println("Written to the DB: " + isWritten)
        if (isWritten) {
          log.info("Data written to DB Success.")
          log.info("Send Data to Extension Handler: {}", metadataAdapterRef)
          StoreTimeseriesResponse(Created,  Option(metadataIdsObj.toMetadataIds))
        } else {
          StoreTimeseriesResponse(InternalServerError, message = Option("Error while storing data."))
        }
      }) to sender()

    case GetTimeSeries(query: MetadataObj) =>
      val influxdb = InfluxDB.connect("localhost", 8086)
      val database = influxdb.selectDatabase("wdias")

      val influxQuery = s"SELECT * FROM observed WHERE " +
        s"moduleId = '${query.moduleId}' " +
        s"AND valueType = '${query.valueType}' " +
        s"AND parameterId = '${query.parameter.parameterId}' " +
        s"AND locationId = '${query.location.locationId}' " +
        s"AND timeSeriesType = '${query.timeSeriesType}' " +
        s"AND timeStepId = '${query.timeStep.timeStepId}'"
      log.info("Influx Query: {}", influxQuery)
      val queryResult: Future[QueryResult] = database.query(influxQuery)

      pipe(queryResult.mapTo[QueryResult] map { result =>
        createResponse(query, result)
      }) to sender()

    case ActorIdentity(_, Some(ref)) =>
      log.info("Set Actor (ScalarAdapter): {}", ref.path.name)
      ref.path.name match {
        case "metadataAdapter" => metadataAdapterRef = ref
        case "statusHandler" => statusHandlerRef = ref
        case default => log.warning("Unknown Actor Identity in ScalarAdapter: {}", default)
      }
    case ActorIdentity(_, None) =>
      context.stop(self)
    case _ =>
      log.info("Unknown message")
  }
}