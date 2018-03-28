package org.wdias.adapters.metadata_adapter

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify}
import akka.pattern.pipe
import akka.util.Timeout
import org.wdias.adapters.metadata_adapter.MetadataAdapter._
import org.wdias.adapters.metadata_adapter.models._
import org.wdias.constant.TimeSeriesType.TimeSeriesType
import org.wdias.constant._
import org.wdias.extensions.ExtensionHandler.OnChangeTimeseries

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object MetadataAdapter {

  // Location Point
  case class GetLocationById(locationId: String)

  case class GetLocations(locationId: String = "", name: String = "")

  case class CreateLocation(location: Location)

  case class ReplaceLocation(locationIdx: String, location: Location)

  case class UpdateLocation(locationIdx: String, locationId: String = "", variable: String = "", unit: String = "", parameterType: String = "")

  case class DeleteLocationById(locationId: String)

  // Parameter
  case class GetParameterById(parameterId: String)

  case class GetParameters(parameterId: String = "", variable: String = "", unit: String = "", parameterType: String = "")

  case class CreateParameter(parameterObj: ParameterObj)

  case class ReplaceParameter(parameterIdx: String, parameter: Parameter)

  case class UpdateParameters(parameterIdx: String, parameterId: String = "", variable: String = "", unit: String = "", parameterType: String = "")

  case class DeleteParameterById(parameterId: String)

  // TimeStep
  case class GetTimeStepById(timeStepId: String)

  case class GetTimeSteps(timeStepId: String = "", unit: String = "", multiplier: Int = 0, divider: Int = 0)

  case class CreateTimeStep(timeStepObj: TimeStepObj)

  case class ReplaceTimeStep(timeStepIdx: String, timeStep: TimeStep)

  case class UpdateTimeSteps(timeStepIdx: String, timeStepId: String = "", unit: String = "", multiplier: Int = 0, divider: Int = 0)

  case class DeleteTimeStepById(timeStepId: String)

  // Timeseries
  case class GetTimeseriesById(timeseriesId: String)

  case class GetTimeseries(timeseriesId: Option[String] = Option(null), moduleId: Option[String] = Option(null), valueType: Option[String] = Option(null), parameterId: Option[String] = Option(null), locationId: Option[String] = Option(null), timeSeriesType: Option[TimeSeriesType] = Option(null), timeStepId: Option[String] = Option(null))

  case class CreateTimeseries(metadataObj: MetadataObj)

  case class CreateTimeseriesWithIds(metadataIdsObj: MetadataIdsObj)

  case class ReplaceTimeseries(timeseriesIdx: String, metadata: Metadata)

  case class ReplaceTimeseriesWithIds(timeseriesIdx: String, metadataIds: MetadataIds)

  case class UpdateTimeseries(timeseriesIdx: String, moduleId: String, valueType: String, parameter: Parameter, location: Location, timeSeriesType: String, timeStep: TimeStep, tags: Array[String])

  case class UpdateTimeseriesWithIds(timeseriesIdx: String, moduleId: String, valueType: String, parameterId: String, locationId: String, timeSeriesType: String, timeStepId: String, tags: Array[String])

  case class DeleteTimeseriesById(timeseriesId: String)

  // Timeseries Events
  case class OnTimeseriesStore(timeSeries: TimeSeries)

  case class IdentifyExtensionHandler(extensionHandlerRef: ActorRef)

}

class MetadataAdapter extends Actor with ActorLogging {

  implicit val timeout: Timeout = Timeout(15 seconds)

  var extensionHandlerRef: ActorRef = _

  def receive: Receive = {
    // Handle Location -> Point MSGs
    case GetLocationById(locationId) =>
      log.info("GET Location By Id: {}", locationId)
      pipe(LocationsDAO.findById(locationId).mapTo[Option[Location]] map { result: Option[Location] =>
        result
      }) to sender()
    case GetLocations(locationId, name) =>
      log.info("GET Query Locations: {} {}", locationId, name)
      pipe(LocationsDAO.find(locationId, name).mapTo[Seq[Location]] map { result: Seq[Location] =>
        result
      }) to sender()
    case CreateLocation(location) =>
      log.info("POST Location: {}", location)
      val isCreated = LocationsDAO.create(location)
      pipe(isCreated.mapTo[Int] map { result: Int =>
        if (result > 0) location else 0
      }) to sender()
    case DeleteLocationById(locationId) =>
      log.info("DELETE Location By Id: {}", locationId)
      val isDeleted = LocationsDAO.deleteById(locationId)
      pipe(isDeleted.mapTo[Int] map { result: Int =>
        result
      }) to sender()

    // Handle Parameter MSGs
    case GetParameterById(parameterId) =>
      log.info("GET Parameter By Id: {}", parameterId)
      pipe(ParametersDAO.findById(parameterId).mapTo[Option[ParameterObj]] map { result: Option[ParameterObj] =>
        result
      }) to sender()
    case GetParameters(parameterId, variable, unit, parameterType) =>
      log.info("GET Query Parameters: {} {} {} {}", parameterId, variable, unit, parameterType)
      pipe(ParametersDAO.find(parameterId, variable, unit, parameterType).mapTo[Seq[ParameterObj]] map { result: Seq[ParameterObj] =>
        result
      }) to sender()
    case CreateParameter(parameterObj) =>
      log.info("POST Parameter: {}", parameterObj)
      val isCreated = ParametersDAO.create(parameterObj)
      pipe(isCreated.mapTo[Int] map { result: Int =>
        result
      }) to sender()
    case DeleteParameterById(parameterId) =>
      log.info("DELETE Parameter By Id: {}", parameterId)
      val isDeleted = ParametersDAO.deleteById(parameterId)
      pipe(isDeleted.mapTo[Int] map { result: Int =>
        result
      }) to sender()

    // Handle TimeStep MSGs
    case GetTimeStepById(timeStepId) =>
      log.info("GET TimeStep By Id: {}", timeStepId)
      pipe(TimeStepsDAO.findById(timeStepId).mapTo[Option[TimeStepObj]] map { result: Option[TimeStepObj] =>
        result
      }) to sender()
    case GetTimeSteps(timeStepId, unit, multiplier, divider) =>
      log.info("GET Query TimeSteps: {} {} {} {}", timeStepId, unit, multiplier, divider)
      pipe(TimeStepsDAO.find(timeStepId, unit, multiplier, divider).mapTo[Seq[TimeStepObj]] map { result: Seq[TimeStepObj] =>
        result
      }) to sender()
    case CreateTimeStep(timeStepObj) =>
      log.info("POST TimeStep: {}", timeStepObj)
      val isCreated = TimeStepsDAO.create(timeStepObj)
      pipe(isCreated.mapTo[Int] map { result: Int =>
        result
      }) to sender()
    case DeleteTimeStepById(timeStepId) =>
      log.info("DELETE TimeStep By Id: {}", timeStepId)
      val isDeleted = TimeStepsDAO.deleteById(timeStepId)
      pipe(isDeleted.mapTo[Int] map { result: Int =>
        result
      }) to sender()

    // Handle Timeseries MSGs
    case GetTimeseriesById(timeseriesId) =>
      log.info("GET Timeseries By Id: {}", timeseriesId)
      pipe(TimeSeriesMetadataDAO.findById(timeseriesId).mapTo[Option[MetadataIdsObj]] map { result: Option[MetadataIdsObj] =>
        result
      }) to sender()
    case GetTimeseries(timeSeriesId, moduleId, valueType, parameterId, locationId, timeSeriesType: Option[TimeSeriesType], timeStepId) =>
      log.info("GET Query Timeseries: {} {} {} {} ", timeSeriesId, moduleId, valueType, parameterId)
      log.info("2 GET Query Timeseries: {} {} {} ", locationId, timeSeriesType, timeStepId)
      pipe(TimeSeriesMetadataDAO.find(timeSeriesId, moduleId, valueType, parameterId, locationId, timeSeriesType, timeStepId).
        mapTo[Seq[MetadataIdsObj]] map { result: Seq[MetadataIdsObj] =>
        result
      }) to sender()
    case CreateTimeseries(m: MetadataObj) =>
      val ss = sender()
      log.info("POST Timeseries: {}, {}", ss, m)
      val createLocation = LocationsDAO.upsert(m.location).mapTo[Int]
      createLocation map { isLocationCreated: Int =>
        if(isLocationCreated > 0) {
          val createParameter = ParametersDAO.upsert(m.parameter).mapTo[Int]
          createParameter map { isParameterCreated: Int =>
            if(isParameterCreated > 0) {
              val createTimeStep = TimeStepsDAO.upsert(m.timeStep).mapTo[Int]
              createTimeStep map { isTimeStepCreated: Int =>
                if(isTimeStepCreated > 0) {
                  val metadataIdsObj = MetadataIdsObj(null, m.moduleId, m.valueType, m.parameter.parameterId, m.location.locationId, m.timeSeriesType, m.timeStep.timeStepId)
                  val isCreated = TimeSeriesMetadataDAO.create(metadataIdsObj)
                  pipe(isCreated.mapTo[Int] map { result: Int =>
                    result
                  }) to ss
                } else {
                  ss ! isTimeStepCreated
                }
              }
            } else {
              ss ! isParameterCreated
            }
          }
        } else {
          ss ! isLocationCreated
        }
      }
    case CreateTimeseriesWithIds(metadataIds: MetadataIdsObj) =>
      val ss = sender()
      log.info("POST Timeseries: {}", metadataIds)
      val getLocation = LocationsDAO.findById(metadataIds.locationId).mapTo[Option[Location]]
      getLocation map { location: Option[Location] =>
        if(location.isDefined) {
          val getParameter = ParametersDAO.findById(metadataIds.parameterId).mapTo[Option[ParameterObj]]
          getParameter map { parameterObj: Option[ParameterObj] =>
            if(parameterObj.isDefined) {
              val getTimeStep = TimeStepsDAO.findById(metadataIds.timeStepId).mapTo[Option[TimeStepObj]]
              getTimeStep map { timeStepObj: Option[TimeStepObj] =>
                if(timeStepObj.isDefined) {
                  val isCreated = TimeSeriesMetadataDAO.create(metadataIds)
                  pipe(isCreated.mapTo[Int] map { result: Int =>
                    result
                  }) to sender()
                } else {
                  ss ! 0
                }
              }
            } else {
              ss ! 0
            }
          }
        } else {
          ss ! 0
        }
      }
    case DeleteTimeseriesById(timeseriesId) =>
      log.info("DELETE Timeseries By Id: {}", timeseriesId)
      val isDeleted = TimeSeriesMetadataDAO.deleteById(timeseriesId)
      pipe(isDeleted.mapTo[Int] map { result: Int =>
        result
      }) to sender()

    // Handle Timeseries Event MSGs
    case OnTimeseriesStore(timeSeries: TimeSeries) =>
      extensionHandlerRef forward OnChangeTimeseries(timeSeries)

    case IdentifyExtensionHandler(actorRef: ActorRef) =>
      extensionHandlerRef = actorRef
      context.actorSelection("/user/extensionHandler") ! Identify(None)
    case ActorIdentity(_, Some(ref)) =>
      log.info("Set Actor (MetadataAdapter): {}", ref.path.name)
      ref.path.name match {
        case "extensionHandler" => extensionHandlerRef = ref
        case default => log.warning("Unknown Actor Identity in MetadataAdapter: {}", default)
      }
    case ActorIdentity(_, None) =>
      context.stop(self)
  }
}
