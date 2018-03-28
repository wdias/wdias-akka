package org.wdias.export.json

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify}
import akka.http.scaladsl.model.StatusCodes.{NotFound}
import akka.util.Timeout
import akka.pattern.pipe
import org.wdias.adapters.metadata_adapter.MetadataAdapter.{GetTimeseriesById, GetTimeseries}
import org.wdias.adapters.scalar_adapter.ScalarAdapter.{GetTimeSeries, GetTimeseriesResponse}
import org.wdias.constant.{MetadataIdsObj, MetadataObj}
import akka.pattern.ask

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object ExportJSON {

  case class ExportJSONDataWithId(timeseriesId: String)

  case class ExportJSONDataWithMetadata(metadataObj: MetadataObj)

  case class ExportJSONDataWithMetadataIds(metadataIdsObj: MetadataIdsObj)

}

class ExportJSON extends Actor with ActorLogging {

  import ExportJSON._

  implicit val timeout: Timeout = Timeout(15 seconds)

  var scalarAdapterRef: ActorRef = _
  context.actorSelection("/user/scalarAdapter") ! Identify(None)
  var vectorAdapterRef: ActorRef = _
  context.actorSelection("/user/vectorAdapter") ! Identify(None)
  var metadataAdapterRef: ActorRef = _
  context.actorSelection("/user/metadataAdapter") ! Identify(None)

  def receive: Receive = {
    case ExportJSONDataWithId(timeseriesId: String) =>
      log.info("Forwarding ExportJSONData With Id > {}", timeseriesId)
      val response: Future[Option[MetadataIdsObj]] = (metadataAdapterRef ? GetTimeseriesById(timeseriesId)).mapTo[Option[MetadataIdsObj]]
      val ss = sender()
      response map { metadataIdsObj: Option[MetadataIdsObj] =>
        log.info("Found Timeseries > {}", metadataIdsObj.getOrElse("None"))
        if (metadataIdsObj.isDefined) {
          pipe((scalarAdapterRef ? GetTimeSeries(metadataIdsObj.get)).mapTo[GetTimeseriesResponse] map { storeTS => storeTS}) to ss
        } else {
          ss ! GetTimeseriesResponse(NotFound, message = Option("Unable to find timeseries"))
        }
      }
    case ExportJSONDataWithMetadata(metadataObj: MetadataObj) =>
      log.info("Forwarding ExportJSONData With Metadata > {}", scalarAdapterRef)
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
          pipe((scalarAdapterRef ? GetTimeSeries(metadataIdsObj)).mapTo[GetTimeseriesResponse] map { storeTS => storeTS}) to ss
        } else {
          ss ! GetTimeseriesResponse(NotFound, message = Option("Unable to find timeseries"))
        }
      }
    case ExportJSONDataWithMetadataIds(metadataIdsObj: MetadataIdsObj) =>
      log.debug("Forwarding ExportJSONData With MetadataIds > {}", scalarAdapterRef)
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
          pipe((scalarAdapterRef ? GetTimeSeries(metadataIdsObjExist)).mapTo[GetTimeseriesResponse] map { storeTS => storeTS}) to ss
        } else {
          ss ! GetTimeseriesResponse(NotFound, message = Option("Unable to find timeseries"))
        }
      }

    case ActorIdentity(_, Some(ref)) =>
      log.info("Set Actor (ExportJSON): {}", ref.path.name)
      ref.path.name match {
        case "metadataAdapter" => metadataAdapterRef = ref
        case "scalarAdapter" => scalarAdapterRef = ref
        case "vectorAdapter" => vectorAdapterRef = ref
        case default => log.warning("Unknown Actor Identity in ExportJSON: {}", default)
      }
    case ActorIdentity(_, None) =>
      context.stop(self)
  }
}