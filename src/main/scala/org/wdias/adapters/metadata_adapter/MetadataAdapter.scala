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
  case class GetLocationById(locationId: String)
  case class GetLocations(locationId: String = "", name: String = "")
  case class CreateLocation(location: Location)
  case class DeleteLocation(locationId: String)
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
