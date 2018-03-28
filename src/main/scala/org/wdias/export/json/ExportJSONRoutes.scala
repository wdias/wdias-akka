package org.wdias.export.json

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.wdias.adapters.scalar_adapter.ScalarAdapter.GetTimeseriesResponse
import org.wdias.constant.{Metadata, MetadataIds, Protocols}
import org.wdias.export.json.ExportJSON.{ExportJSONDataWithId, ExportJSONDataWithMetadata, ExportJSONDataWithMetadataIds}

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
      pathPrefix("export" / "json" / "raw") {
        concat(
          // GET: Get Timeseries with Id
          (get & pathPrefix(Segment)) { timeseriesId: String =>
            logExportJSONRoutes.info("/export/json/raw/XXX GET request: > {}, {}", timeseriesId)
            val response: Future[GetTimeseriesResponse] = (exportJSONRef ? ExportJSONDataWithId(timeseriesId)).mapTo[GetTimeseriesResponse]
            onSuccess(response) { result =>
              if(result.timeseries.isDefined) {
                complete(result.statusCode -> result.timeseries.get.data)
              } else {
                complete(result.statusCode -> result.message.get)
              }
            }
          },
          // POST: Get Timeseries with MetadataIds
          (post & path("ids") & entity(as[MetadataIds])) { metadataIds: MetadataIds =>
            logExportJSONRoutes.info("/export/json/raw/ids POST request: > {}", metadataIds)
            val response: Future[GetTimeseriesResponse] = (exportJSONRef ? ExportJSONDataWithMetadataIds(metadataIds.toMetadataIdsObj)).mapTo[GetTimeseriesResponse]
            onSuccess(response) { result =>
              if(result.timeseries.isDefined) {
                complete(result.statusCode -> result.timeseries.get.data)
              } else {
                complete(result.statusCode -> result.message.get)
              }
            }
          },
          // POST: Get Timeseries with Metadata
          (post & entity(as[Metadata])) { metadata: Metadata =>
            logExportJSONRoutes.info("/export/json/raw POST request: > {}", metadata)
            val response: Future[GetTimeseriesResponse] = (exportJSONRef ? ExportJSONDataWithMetadata(metadata.toMetadataObj)).mapTo[GetTimeseriesResponse]
            onSuccess(response) { result =>
              if(result.timeseries.isDefined) {
                complete(result.statusCode -> result.timeseries.get.data)
              } else {
                complete(result.statusCode -> result.message.get)
              }
            }
          }
        )
      }
    )
  }
}