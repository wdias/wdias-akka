package org.wdias.adapters.metadata_adapter

import akka.actor.{Actor, ActorLogging}
import akka.pattern.pipe
import akka.util.Timeout
import org.wdias.adapters.metadata_adapter.MetadataAdapter._
import org.wdias.adapters.metadata_adapter.models._
import org.wdias.constant._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object MetadataAdapter {
  // Location Point
  case class GetLocationById(locationId: String)
  case class GetLocations(locationId: String = "", name: String = "")
  case class CreateLocation(location: Location)
  case class ReplaceLocation(locationIdx: String, location: Location)
  case class UpdateLocation(locationIdx: String, locationId:String = "", variable: String="", unit: String="", parameterType: String="")
  case class DeleteLocation(locationId: String)
  // Parameter
  case class GetParameterById(parameterId: String)
  case class GetParameters(parameterId:String = "", variable: String="", unit: String="", parameterType: String="")
  case class CreateParameter(parameter: Parameter)
  case class ReplaceParameter(parameterIdx: String, parameter: Parameter)
  case class UpdateParameters(parameterIdx: String, parameterId:String = "", variable: String="", unit: String="", parameterType: String="")
  case class DeleteParameterById(parameterId: String)
  // TimeStep
  case class GetTimeStepById(timeStepId: String)
  case class GetTimeSteps(timeStepId:String = "", unit: String="", multiplier: Int=0, divider: Int=0)
  case class CreateTimeStep(timeStep: TimeStep)
  case class ReplaceTimeStep(timeStepIdx: String, timeStep: TimeStep)
  case class UpdateTimeSteps(timeStepIdx: String, timeStepId:String = "", unit: String="", multiplier: Int=0, divider: Int=0)
  case class DeleteTimeStepById(timeStepId: String)
  // Timeseries
  case class GetTimeseriesById(timeseriesId: String)
  case class GetTimeseries(timeseriesId:String = "", moduleId: String="", valueType: String="", parameter: Parameter, location: Location, timeSeriesType: String, timeStep: TimeStep, tags: Array[String])
  case class CreateTimeseries(metaData: Metadata)
  case class CreateTimeseriesWithIds(metadataIds: MetadataIds)
  case class ReplaceTimeseries(timeseriesIdx: String, metadata: Metadata)
  case class ReplaceTimeseriesWithIds(timeseriesIdx: String, metadataIds: MetadataIds)
  case class UpdateTimeseries(timeseriesIdx: String, moduleId: String, valueType: String, parameter: Parameter, location: Location, timeSeriesType: String, timeStep: TimeStep, tags: Array[String])
  case class UpdateTimeseriesWithIds(timeseriesIdx: String, moduleId: String, valueType: String, parameterId: String, locationId: String, timeSeriesType: String, timeStepId: String, tags: Array[String])
  case class DeleteTimeseriesById(timeseriesId: String)
}

class MetadataAdapter extends Actor with ActorLogging {

  implicit val timeout: Timeout = Timeout(15 seconds)

  def receive: Receive = {
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
        if(result > 0) location else 0
      }) to sender()
    case DeleteLocation(locationId) =>
      log.info("DELETE Location By Id: {}", locationId)
      val isDeleted = LocationsDAO.deleteById(locationId)
      pipe(isDeleted.mapTo[Int] map { result: Int =>
        result
      }) to sender()
  }
}
