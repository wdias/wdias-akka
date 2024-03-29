package org.wdias.`import`.json

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify}
import akka.http.scaladsl.model.StatusCodes.{NotFound}
import akka.pattern.pipe
import akka.util.Timeout
import org.wdias.adapters.metadata_adapter.MetadataAdapter._
import org.wdias.adapters.scalar_adapter.ScalarAdapter.{StoreTimeSeries, StoreTimeseriesResponse}
import org.wdias.constant._
import akka.pattern.ask

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object ImportJSON {

  case class ImportJSONDataWithId(timeseriesId: String, data: List[DataPoint])

  case class ImportJSONDataWithMetadata(metadataObj: MetadataObj, data: List[DataPoint])

  case class ImportJSONDataWithMetadataIds(metadataIdsObj: MetadataIdsObj, data: List[DataPoint])

}

class ImportJSON extends Actor with ActorLogging {

  import ImportJSON._

  implicit val timeout: Timeout = Timeout(15 seconds)

  var scalarAdapterRef: ActorRef = _
  context.actorSelection("/user/scalarAdapter") ! Identify(None)
  var vectorAdapterRef: ActorRef = _
  context.actorSelection("/user/vectorAdapter") ! Identify(None)
  var metadataAdapterRef: ActorRef = _
  context.actorSelection("/user/metadataAdapter") ! Identify(None)

  def receive: Receive = {
    case ImportJSONDataWithId(timeseriesId: String, data: List[DataPoint]) =>
      log.info("Forwarding ImportJSONData With Id > {}", timeseriesId)
      val response: Future[Option[MetadataIdsObj]] = (metadataAdapterRef ? GetTimeseriesById(timeseriesId)).mapTo[Option[MetadataIdsObj]]
      val ss = sender()
      response map { metadataIdsObj: Option[MetadataIdsObj] =>
        log.info("Found Timeseries > {}", metadataIdsObj.getOrElse("None"))
        if (metadataIdsObj.isDefined) {
          pipe((scalarAdapterRef ? StoreTimeSeries(TimeSeries(metadataIdsObj.get.timeSeriesId, metadataIdsObj.get, data))).mapTo[StoreTimeseriesResponse] map { storeTS => storeTS}) to ss
        } else {
          ss ! StoreTimeseriesResponse(NotFound, message = Option("Unable to find timeseries"))
        }
      }
    case ImportJSONDataWithMetadata(metadataObj: MetadataObj, data: List[DataPoint]) =>
      log.info("Forwarding ImportJSONData With Metadata > {}", scalarAdapterRef)
      val response: Future[Seq[MetadataIdsObj]] = (metadataAdapterRef ? GetTimeseries(
        moduleId = Option(metadataObj.moduleId),
        valueType = Option(metadataObj.valueType.toString),
        parameterId = Option(metadataObj.parameter.parameterId),
        locationId = Option(metadataObj.location.locationId),
        timeSeriesType = Option(metadataObj.timeSeriesType),
        timeStepId = Option(metadataObj.timeStep.timeStepId)
      )).mapTo[Seq[MetadataIdsObj]]
      val ss = sender()
      response map { metadataIdsObjs: Seq[MetadataIdsObj] =>
        if (metadataIdsObjs.nonEmpty) {
          val metadataIdsObj = metadataIdsObjs.head
          pipe((scalarAdapterRef ? StoreTimeSeries(TimeSeries(metadataIdsObj.timeSeriesId, metadataIdsObj, data))).mapTo[StoreTimeseriesResponse] map { storeTS => storeTS}) to ss
        } else {
          val createTS = (metadataAdapterRef ? CreateTimeseries(metadataObj)).mapTo[Int]
          createTS map { isCreated: Int =>
            if(isCreated > 0) {
              pipe((scalarAdapterRef ? StoreTimeSeries(TimeSeries(metadataObj.timeSeriesId, metadataObj.toMetadataIds.toMetadataIdsObj, data))).mapTo[StoreTimeseriesResponse] map { storeTS => storeTS}) to ss
            } else {
              ss ! StoreTimeseriesResponse(NotFound, message = Option("Unable to find timeseries"))
            }
          }
        }
      }
    case ImportJSONDataWithMetadataIds(metadataIdsObj: MetadataIdsObj, data: List[DataPoint]) =>
      log.debug("Forwarding ImportJSONData With MetadataIds > {}", scalarAdapterRef)
      val response: Future[Seq[MetadataIdsObj]] = (metadataAdapterRef ? GetTimeseries(
        moduleId = Option(metadataIdsObj.moduleId),
        valueType = Option(metadataIdsObj.valueType.toString),
        parameterId = Option(metadataIdsObj.parameterId),
        locationId = Option(metadataIdsObj.locationId),
        timeSeriesType = Option(metadataIdsObj.timeSeriesType),
        timeStepId = Option(metadataIdsObj.timeStepId)
      )).mapTo[Seq[MetadataIdsObj]]
      val ss = sender()
      response map { metadataIdsObjs: Seq[MetadataIdsObj] =>
        if (metadataIdsObjs.nonEmpty) {
          val metadataIdsObjExist = metadataIdsObjs.head
          pipe((scalarAdapterRef ? StoreTimeSeries(TimeSeries(metadataIdsObjExist.timeSeriesId, metadataIdsObjExist, data))).mapTo[StoreTimeseriesResponse] map { storeTS => storeTS}) to ss
        } else {
          val createTS = (metadataAdapterRef ? CreateTimeseriesWithIds(metadataIdsObj)).mapTo[Int]
          createTS map { isCreated: Int =>
            if(isCreated > 0) {
              pipe((scalarAdapterRef ? StoreTimeSeries(TimeSeries(metadataIdsObj.timeSeriesId, metadataIdsObj.toMetadataIds.toMetadataIdsObj, data))).mapTo[StoreTimeseriesResponse] map { storeTS => storeTS}) to ss
            } else {
              ss ! StoreTimeseriesResponse(NotFound, message = Option("Unable to find timeseries"))
            }
          }
        }
      }

    case ActorIdentity(_, Some(ref)) =>
      log.info("Set Actor (ImportJSON): {}", ref.path.name)
      ref.path.name match {
        case "metadataAdapter" => metadataAdapterRef = ref
        case "scalarAdapter" => scalarAdapterRef = ref
        case "vectorAdapter" => vectorAdapterRef = ref
        case default => log.warning("Unknown Actor Identity in ImportJSON: {}", default)
      }
    case ActorIdentity(_, None) =>
      context.stop(self)
  }
}