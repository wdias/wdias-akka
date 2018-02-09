package org.wdias.timeseries_api

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes.Created
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.wdias.adapters.metadata_adapter.MetadataAdapter._
import org.wdias.constant.{Protocols, _}

import scala.concurrent.Future
import scala.concurrent.duration._

// Location REST API Routes class
trait LocationRoutes extends Protocols {
  // abstract system value will be provide by app
  implicit def system: ActorSystem
  implicit def materializer: ActorMaterializer

  // logging for InputRoutes
  lazy val logLocationRoutes = Logging(system, classOf[LocationRoutes])

  // Actor dependencies required for LocationRoutes
  def metadataAdapterRef: ActorRef

  // Required by the `ask` (?) method below
  implicit lazy val timeoutLocationRoutes: Timeout = Timeout(15.seconds) // TODO: Obtain from config

  // --- All Input Routes ---
  lazy val locationRoutes: Route = {
    concat(
      pathPrefix("location" / "point") {
        concat(
          // GET: Get Point
          (get & pathPrefix(Segment)) { locationId: String =>
            logLocationRoutes.info("/location/point GET request: > {}", locationId)
            val response = (metadataAdapterRef ? GetLocationById(locationId)).mapTo[Option[Location]]
            onSuccess(response) { location =>
              complete(Created -> location)
            }
          },
          // GET: Query on Points
          (get & pathEnd & parameters('locationId.as[String].?, 'name.as[String].?)) { (locationId, name) =>
            logLocationRoutes.info("/location/point GET request: List")
            val response: Future[Seq[Location]] = (metadataAdapterRef ? GetLocations(locationId.getOrElse(""), name.getOrElse(""))).mapTo[Seq[Location]]
            onSuccess(response) { locations: Seq[Location] =>
              complete(Created -> locations)
            }
          },

          // POST: Create Point
          (post & entity(as[Location])) { location: Location =>
            logLocationRoutes.info("/location/point POST request: > {}", location)
            val response: Future[Location] = (metadataAdapterRef ? CreateLocation(location)).mapTo[Location]
            onSuccess(response) { location: Location =>
              complete(Created -> location)
            }
          },
          // PUT: Replace Point
          (put & pathPrefix(Segment)) { locationId: String =>
            entity(as[Location]) { location: Location =>
            logLocationRoutes.info("/location/point GET request: Replace {} : {}", locationId, location)
            complete(Created -> "Replace")
          },
          // PATCH: Update Point
          (patch & pathPrefix(Segment)) { locationId: String =>
            // TODO: Read fields separately
            entity(as[Location]) { location: Location =>
            logLocationRoutes.info("/location/point GET request: Update {} : {}", locationId, location)
            complete(Created -> "Update")
          },
          // DELETE: Delete Point
          (delete & pathPrefix(Segment)) { locationId: String =>
            logLocationRoutes.info("/location/point DELETE request: > {}", locationId)
            val response: Future[Int] = (metadataAdapterRef ? DeleteLocationById(locationId)).mapTo[Int]
            onSuccess(response) { isDeleted: Int =>
              complete(Created -> isDeleted.toString)
            }
          }
        )
      }
    )
  }
}