package org.wdias.export.csv

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
import org.wdias.adapters.scalar_adapter.ScalarAdapter.Result
import org.wdias.constant.{MetaData, Protocols}
import org.wdias.export.json.ExportJSON.ExportJSONData

import scala.concurrent.Future
import scala.concurrent.duration._

// REST API Routes class
trait ExportCSVRoutes extends Protocols {
  // abstract system value will be provide by app
  implicit def system: ActorSystem
  implicit def materializer: ActorMaterializer

  // logging for InputRoutes
  lazy val logExportCSVRoutes = Logging(system, classOf[ExportCSVRoutes])

  // Other dependencies required by InputRoutes
  def exportJSONRef: ActorRef
  def exportCSVRef: ActorRef

  // Required by the `ask` (?) method below
  implicit lazy val timeoutExportCSVRoutes: Timeout = Timeout(15.seconds) // TODO: Obtain from config

  // --- All Input Routes ---
  lazy val exportCSVRoutes: Route = {
    concat(
      path("export" / "csv" / "raw") {
        (post & entity(as[MetaData])) { metaData: MetaData =>
          logExportCSVRoutes.info("/export GET request: > {}", metaData)
          val response: Future[Result] = (exportJSONRef ? ExportJSONData(metaData)).mapTo[Result]
          onSuccess(response) { result =>
            complete(Created -> result.timeSeriesEnvelop)
          }
        }
      }
    )
  }
}