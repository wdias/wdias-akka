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
trait ParamterRoutes extends Protocols {
  // abstract system value will be provide by app
  implicit def system: ActorSystem
  implicit def materializer: ActorMaterializer

  // logging for InputRoutes
  lazy val logParameterRoutes = Logging(system, classOf[ParamterRoutes])

  // Actor dependencies required for LocationRoutes
  def metadataAdapterRef: ActorRef

  // Required by the `ask` (?) method below
  implicit lazy val timeoutLocationRoutes: Timeout = Timeout(15.seconds) // TODO: Obtain from config

  // --- All Input Routes ---
  lazy val locationRoutes: Route = {
    concat(
      pathPrefix("parameter") {
        concat(
          // GET: Get Point
          (get & pathPrefix(Segment)) { parameterId: String =>
            logParameterRoutes.info("/parameter GET request: > {}", parameterId)
            val response = (metadataAdapterRef ? GetLocationById(parameterId)).mapTo[Option[Location]]
            onSuccess(response) { location =>
              complete(Created -> location)
            }
          },
          // GET: Query on Points
          (get & pathEnd & parameters('parameterId.as[String].?, 'name.as[String].?)) { (parameterId, name) =>
            logParameterRoutes.info("/parameter GET request: List")
            val response: Future[Seq[Location]] = (metadataAdapterRef ? GetLocations(parameterId.getOrElse(""), name.getOrElse(""))).mapTo[Seq[Location]]
            onSuccess(response) { locations: Seq[Location] =>
              complete(Created -> locations)
            }
          },

          // POST: Create Point
          (post & entity(as[Location])) { location: Location =>
            logParameterRoutes.info("/parameter POST request: > {}", location)
            val response: Future[Location] = (metadataAdapterRef ? CreateLocation(location)).mapTo[Location]
            onSuccess(response) { location: Location =>
              complete(Created -> location)
            }
          },
          // PUT: Replace Point
          (put & entity(as[Location])) { location: Location =>
            logParameterRoutes.info("/parameter GET request: Query:")
            complete(Created -> "Query")
          },
          // PATCH: Update Point
          (patch & entity(as[Location])) { location: Location =>
            logParameterRoutes.info("/parameter GET request: Query:")
            complete(Created -> "Query")
          },
          // DELETE: Delete Point
          (delete & parameters('parameterId)) { (parameterId) =>
            logParameterRoutes.info("/parameter DELETE request: > {}", parameterId)
            val response: Future[Int] = (metadataAdapterRef ? DeleteLocation(parameterId)).mapTo[Int]
            onSuccess(response) { isDeleted: Int =>
              complete(Created -> isDeleted.toString)
            }
          }
        )
      }
    )
  }
}