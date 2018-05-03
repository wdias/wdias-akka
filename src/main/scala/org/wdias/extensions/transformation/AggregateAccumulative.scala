package org.wdias.extensions.transformation

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify}
import akka.util.Timeout
import org.wdias.adapters.scalar_adapter.ScalarAdapter.{GetTimeSeries, GetTimeseriesResponse, StoreSuccess, StoreTimeSeries}
import org.wdias.constant._

import scala.concurrent.Future
import scala.concurrent.duration._
import akka.pattern.{ask, pipe}
import org.wdias.extensions._
import org.wdias.extensions.transformation.Transformation.{SetAdapterRef, TriggerTransformation}

import scala.concurrent.ExecutionContext.Implicits.global

object AggregateAccumulative {
}

class AggregateAccumulative extends Actor with ActorLogging {
  implicit val timeout: Timeout = Timeout(15 seconds)

  var scalarAdapterRef: ActorRef = _
  var vectorAdapterRef: ActorRef = _
  var gridAdapterRef: ActorRef = _

  def run(x1: Variable, y1: Variable, input: TimeSeries): TimeSeries = {
    log.info("Run x1:{} y1:{}", x1, y1)
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    /** Variables will always store with all metadata, in order to increase performance on Extension execution */
    val y1TimeStep = y1.timeSeries.get.timeStep
    log.info("Run >>> {}", y1TimeStep.toTimeStepObj.unit)
    var dataPoints: List[DataPoint] = List()

    y1TimeStep.toTimeStepObj.unit match {
      case TimeStepUnit.Hour =>
        log.info("hour case {}", input.data.head.time)
        var sum: Double = 0
        var prev: LocalDateTime = LocalDateTime.parse(input.data.head.time, formatter)
        log.info("prev {}", prev)
        input.data.foreach { tt: DataPoint =>
          log.info("DataPoint {}", tt)
          val dateTime = LocalDateTime.parse(tt.time, formatter)
          if(prev.getHour.equals(dateTime.getHour)) {
            log.info("match {} {}", prev.getHour, dateTime.getHour)
            sum += tt.value.get
          } else {
            val newDateTime = dateTime
            newDateTime.withMinute(0).withSecond(0)
            log.info("Not match {}", newDateTime)
            val p: DataPoint = DataPoint(newDateTime.format(formatter)).addValue(sum)
            dataPoints = dataPoints :+ p
            sum = 0
            prev = newDateTime
          }
        }
        val dateTime = LocalDateTime.parse(input.data.last.time, formatter)
        /** If last sum is not counted */
        if(prev != dateTime) {
          dateTime.withMinute(0).withSecond(0)
          val p: DataPoint = DataPoint(dateTime.format(formatter)).addValue(sum)
          dataPoints = dataPoints :+ p
        }
      case _ =>
        log.info("Not found")
    }
    log.info("Output {}", dataPoints)
    TimeSeries(null, y1.timeSeries.get.toMetadataIds.toMetadataIdsObj, dataPoints)
  }

  def receive: Receive = {
    case TriggerTransformation(extensionObj: ExtensionObj, transformationExtensionObj: TransformationExtensionObj) =>
      log.info("TriggerExtension > {}, {}", extensionObj, transformationExtensionObj)
      val transformationExtension = transformationExtensionObj.toTransformationExtensionWithExtensionObj(extensionObj)
      if(transformationExtension.inputVariables.nonEmpty) {
        val x1 = transformationExtension.inputVariables.head
        val x1Variable: Option[Variable] = transformationExtension.variables.find(_.variableId == x1)
        if(x1Variable.isDefined) {
          if(transformationExtension.outputVariables.nonEmpty) {
            val y1 = transformationExtension.outputVariables.head
            val y1Variable: Option[Variable] = transformationExtension.variables.find(_.variableId == y1)
            if(y1Variable.isDefined) {
              // TODO: Check for TimeSeries existence before trying to retrieve
              if(x1Variable.get.timeSeriesWithIds.isDefined) {
                (scalarAdapterRef ? GetTimeSeries(x1Variable.get.timeSeriesWithIds.get.toMetadataIdsObj, TimeSeries)).mapTo[TimeSeries] map { timeSeries: TimeSeries =>

                }
              } else if(x1Variable.get.timeSeries.isDefined) {
                (scalarAdapterRef ? GetTimeSeries(x1Variable.get.timeSeries.get.toMetadataIds.toMetadataIdsObj, TimeSeries)).mapTo[TimeSeries] map { timeSeries =>
                  (scalarAdapterRef ? StoreTimeSeries(run(x1Variable.get, y1Variable.get, timeSeries), MetadataIdsObj)).mapTo[MetadataIdsObj] map { metadataIdsObj =>
                    log.info("AggregateAccumulative done. Stopping self.")
                    context stop self
                  }
                }
              } else if(x1Variable.get.timeSeriesId.isDefined) {
                // TODO: Get TimeSeries Metadata before
                (scalarAdapterRef ? GetTimeSeries(x1Variable.get.timeSeriesWithIds.get.toMetadataIdsObj, TimeSeries)).mapTo[TimeSeries] map { timeSeries =>

                }
              } else {
                log.warning("Unable to find timeseries metadata.")
              }
            } else {
              log.warning("Unable to find output variable")
            }
          }
        } else {
          log.warning("Unable to find input variable")
        }
      }

    case SetAdapterRef(scalarAdapter: ActorRef, vectorAdapter: ActorRef, gridAdapter: ActorRef) =>
      log.info("SetAdapterRefs: {}, {}, {}", scalarAdapter, vectorAdapter, gridAdapter)
      scalarAdapterRef = scalarAdapter
      vectorAdapterRef = vectorAdapter
      gridAdapterRef = gridAdapter

    case ActorIdentity(_, None) =>
      context.stop(self)
  }
}