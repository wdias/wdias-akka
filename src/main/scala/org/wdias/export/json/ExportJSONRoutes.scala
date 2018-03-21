package org.wdias.export.json

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
import org.wdias.constant.{Metadata, Protocols}
import org.wdias.export.json.ExportJSON.ExportJSONData

import scala.concurrent.Future
import scala.concurrent.duration._

// REST API Routes class
trait ExportJSONRoutes extends Protocols {
  // abstract system value will be provide by app
  implicit def system: ActorSystem
  implicit def materializer: ActorMaterializer

  // logging for InputRoutes
  lazy val logExportJSONRoutes = Logging(system, classOf[ExportJSONRoutes])

  // Other dependencies required by InputRoutes
  def exportJSONRef: ActorRef
  def exportCSVRef: ActorRef

  // Required by the `ask` (?) method below
  implicit lazy val timeoutExportJSONRoutes: Timeout = Timeout(15.seconds) // TODO: Obtain from config

  // --- All Input Routes ---
  lazy val exportJSONRoutes: Route = {
    concat(
      path("export" / "json" / "raw") {
        (post & entity(as[Metadata])) { metaData: Metadata =>
          logExportJSONRoutes.info("/export GET request: > {}", metaData)
            complete(Created)
        }
      }
    )
  }
}