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
      log.debug("Forwarding ImportJSONData With Id > ", scalarAdapterRef)
      val response: Future[Option[MetadataIdsObj]] = (metadataAdapterRef ? GetTimeseriesById(timeseriesId)).mapTo[Option[MetadataIdsObj]]
      response map { metadataIdsObj: Option[MetadataIdsObj] =>
        if(metadataIdsObj.isDefined) {
          scalarAdapterRef forward StoreTimeSeries(TimeSeries(timeseriesId, metadataIdsObj.get, data))
        } else {
          StoreTimeseriesResponse(NotFound, message = Option("Unable to find timeseries"))
        }
      }
    case ImportJSONDataWithMetadata(metadataObj: MetadataObj, data: List[DataPoint]) =>
      log.debug("Forwarding ImportJSONData With Metadata > ", scalarAdapterRef)
      val response: Future[Seq[MetadataIdsObj]] = (metadataAdapterRef ? GetTimeseries(
        moduleId = Option(metadataObj.moduleId),
        valueType = Option(metadataObj.valueType.toString),
        parameterId = Option(metadataObj.parameter.parameterId),
        locationId = Option(metadataObj.location.locationId),
        timeSeriesType = Option(metadataObj.timeSeriesType.toString),
        timeStepId = Option(metadataObj.timeStep.timeStepId)
      )).mapTo[Seq[MetadataIdsObj]]
       response map { metadataIdsObjs: Seq[MetadataIdsObj] =>
        if(metadataIdsObjs.nonEmpty) {
          val metadataIdsObj = metadataIdsObjs.head
          scalarAdapterRef forward StoreTimeSeries(TimeSeries(metadataIdsObj.timeSeriesId, metadataIdsObj, data))
        } else {
          StoreTimeseriesResponse(NotFound, message = Option("Unable to find timeseries"))
        }
      }
    case ImportJSONDataWithMetadataIds(metadataIdsObj: MetadataIdsObj, data: List[DataPoint]) =>
      log.debug("Forwarding ImportJSONData With MetadataIds > ", scalarAdapterRef)
      val response: Future[Seq[MetadataIdsObj]] = (metadataAdapterRef ? GetTimeseries(
        moduleId = Option(metadataIdsObj.moduleId),
        valueType = Option(metadataIdsObj.valueType.toString),
        parameterId = Option(metadataIdsObj.parameterId),
        locationId = Option(metadataIdsObj.locationId),
        timeSeriesType = Option(metadataIdsObj.timeSeriesType.toString),
        timeStepId = Option(metadataIdsObj.timeStepId)
      )).mapTo[Seq[MetadataIdsObj]]
      response map { metadataIdsObjs: Seq[MetadataIdsObj] =>
        if(metadataIdsObjs.nonEmpty) {
          val metadataIdsObj = metadataIdsObjs.head
          scalarAdapterRef forward StoreTimeSeries(TimeSeries(metadataIdsObj.timeSeriesId, metadataIdsObj, data))
        } else {
          StoreTimeseriesResponse(NotFound, message = Option("Unable to find timeseries"))
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