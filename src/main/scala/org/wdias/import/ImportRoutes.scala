package org.wdias.`import`

import java.io.File
import java.net.URL

import sys.process._
import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes.Created
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Framing}
import akka.util.{ByteString, Timeout}
import org.wdias.`import`.csv.ImportCSV.ImportCSVFile
import org.wdias.`import`.json.ImportJSON.ImportJSONData
import org.wdias.adapter.scalar_adapter.ScalarAdapter.StoreSuccess
import org.wdias.constant.{MetaData, Protocols, TimeSeriesEnvelop}

import scala.concurrent.Future
import scala.concurrent.duration._

// Input Routes class
trait ImportRoutes extends Protocols {
  // abstract system value will be provide by app
  implicit def system: ActorSystem
  implicit def materializer: ActorMaterializer

  // logging for InputRoutes
  lazy val log = Logging(system, classOf[ImportRoutes])

  // Other dependencies required by InputRoutes
  def importJSONRef: ActorRef
  def importCSVRef: ActorRef

  // Required by the `ask` (?) method below
  implicit lazy val timeout: Timeout = Timeout(5.seconds) // TODO: Obtain from config

  // --- All Input Routes ---
  lazy val iportRoutes: Route = {
    concat(
      pathPrefix("observed") {
        (post & entity(as[TimeSeriesEnvelop])) { timeSeriesEnvelop =>
          val response: Future[StoreSuccess] = (importJSONRef ? ImportJSONData(timeSeriesEnvelop)).mapTo[StoreSuccess]
          onSuccess(response) { result =>
            complete(Created -> result.metadata)
          }
        }
      },
      pathPrefix("file") {
        formField('metaData.as[MetaData]) { metaData =>
          fileUpload("csv") {
            case (_, byteSource) =>
              val splitLines = Framing.delimiter(ByteString("\n"), 1024, allowTruncation = true)
              var lines: Array[String] = Array()
              log.debug("Got file request")
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
      (post & pathPrefix("file_test")) {
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
      pathPrefix("fetch") {
        (post & entity(as[TimeSeriesEnvelop])) { fetchInfo =>
          // TODO: Working for large files
          // new URL("https://www.unidata.ucar.edu/software/netcdf/examples/test_echam_spectral-deflated.nc") #> new File("/tmp/test_echam_spectral-deflated.nc") !!
          val response: Future[StoreSuccess] = (importJSONRef ? ImportJSONData(fetchInfo)).mapTo[StoreSuccess]
          log.info("Fetched test_echam_spectral-deflated.nc")
          onSuccess(response) { result =>
            complete(Created -> result.metadata)
          }
        }
      }
    )
  }
}