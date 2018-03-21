package org.wdias.`import`.json

import java.io.File
import java.net.URL

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes.{Created, NotFound}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Framing}
import akka.util.{ByteString, Timeout}
import org.wdias.`import`.csv.ImportCSV.ImportCSVFile
import org.wdias.`import`.json.ImportJSON.{ImportJSONDataWithId, ImportJSONDataWithMetadata, ImportJSONDataWithMetadataIds}
import org.wdias.adapters.scalar_adapter.ScalarAdapter.{StoreSuccess, StoreTimeseriesResponse}
import org.wdias.constant._

import scala.concurrent.Future
import scala.concurrent.duration._

// Input Routes class
trait ImportJSONRoutes extends Protocols {
  // abstract system value will be provide by app
  implicit def system: ActorSystem
  implicit def materializer: ActorMaterializer

  // logging for InputRoutes
  lazy val logImportJSONRoutes = Logging(system, classOf[ImportJSONRoutes])

  // Other dependencies required by InputRoutes
  def importJSONRef: ActorRef
  def importCSVRef: ActorRef

  // Required by the `ask` (?) method below
  implicit lazy val timeoutImportJSONRoutes: Timeout = Timeout(20.seconds) // TODO: Obtain from config

  // --- All Input Routes ---
  lazy val importJSONRoutes: Route = {
    concat(
      // POST: `raw` Import
      pathPrefix("import" / "json" / "raw") {
        concat(
          // With TimeSeries Id
          (post & pathPrefix(Segment)) { timeseriesId: String =>
            entity(as[List[DataPoint]]) { timeseries: List[DataPoint] =>
              logImportJSONRoutes.info("/import/json/raw/XXX POST request: > {}, {}", timeseriesId, timeseries)
              val response: Future[StoreTimeseriesResponse] = (importJSONRef ? ImportJSONDataWithId(timeseriesId, timeseries)).mapTo[StoreTimeseriesResponse]
              onSuccess(response) { result =>
                if(result.metadataIds.isDefined) {
                  complete(result.statusCode -> result.metadataIds.get)
                } else {
                  complete(result.statusCode -> result.message.get)
                }
              }
            }
          },
          // With TimeSeries Metadata in Body
          (post & entity(as[TimeSeriesWithMetadata])) { timeSeriesWithMetadata =>
            logImportJSONRoutes.info("/import/json/raw POST request: > {}", timeSeriesWithMetadata)
            if(timeSeriesWithMetadata.metadata.isDefined) {
              val response: Future[StoreTimeseriesResponse] = (importJSONRef ? ImportJSONDataWithMetadata(timeSeriesWithMetadata.metadata.get.toMetadataObj, timeSeriesWithMetadata.timeSeries)).mapTo[StoreTimeseriesResponse]
              onSuccess(response) { result =>
                if(result.metadataIds.isDefined) {
                  complete(result.statusCode -> result.metadataIds.get)
                } else {
                  complete(result.statusCode -> result.message.get)
                }
              }
            } else if(timeSeriesWithMetadata.metadataIds.isDefined) {
              val response: Future[StoreTimeseriesResponse] = (importJSONRef ? ImportJSONDataWithMetadataIds(timeSeriesWithMetadata.metadataIds.get.toMetadataIdsObj, timeSeriesWithMetadata.timeSeries)).mapTo[StoreTimeseriesResponse]
              onSuccess(response) { result =>
                if(result.metadataIds.isDefined) {
                  complete(result.statusCode -> result.metadataIds.get)
                } else {
                  complete(result.statusCode -> result.message.get)
                }
              }
            } else {
              complete(NotFound -> "Can not find timeseries metadata.")
            }
          }
        )
      },
      path("import" / "json" / "upload") {
        formField('metadata.as[Metadata]) { metaData =>
          fileUpload("json") {
            case (_, byteSource) =>
              val splitLines = Framing.delimiter(ByteString("\n"), 1024, allowTruncation = true)
              var lines: Array[String] = Array()
              logImportJSONRoutes.debug("Got file request")
              val done: Future[Done] = byteSource
                .via(splitLines)
                .map(_.utf8String)
                .runForeach(line => {
                  lines = lines :+ line
                })
              onSuccess(done) { _ =>
                val response: Future[StoreSuccess] = (importCSVRef ? ImportCSVFile(metaData, lines)).mapTo[StoreSuccess]
                onSuccess(response) { result =>
                  complete(Created -> result.metadata)
                }
              }
          }
        }
      },
      (post & path("import" / "json" / "binary")) {
        withoutSizeLimit {
          extractDataBytes { bytes =>
            val finishedWriting = bytes.runWith(FileIO.toPath(new File("/Users/gihan/example.mp4").toPath))

            // we only want to respond once the incoming data has been handled:
            onComplete(finishedWriting) { ioResult =>
              complete("Finished writing data: " + ioResult)
            }
          }
        }
      },
      path("import" / "json" / "fetch") {
        (post & entity(as[TimeSeriesWithMetadata])) { fetchInfo =>
          // TODO: Working for large files
          // new URL("https://www.unidata.ucar.edu/software/netcdf/examples/test_echam_spectral-deflated.nc") #> new File("/tmp/test_echam_spectral-deflated.nc") !!
          val response: Future[StoreSuccess] = (importJSONRef ? ImportJSONDataWithId(fetchInfo.metadataIds.get.timeSeriesId.get, fetchInfo.timeSeries)).mapTo[StoreSuccess]
          logImportJSONRoutes.info("Fetched test_echam_spectral-deflated.nc")
          onSuccess(response) { result =>
            complete(Created -> result.metadata)
          }
        }
      }
    )
  }
}