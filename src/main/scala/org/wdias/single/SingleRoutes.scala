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
import org.wdias.adapters.scalar_adapter.ScalarAdapter.StoreSuccess
import org.wdias.constant.{MetaData, Protocols, TimeSeriesEnvelop}
import org.wdias.export.ExportRoutes

import scala.concurrent.Future
import scala.concurrent.duration._

// Input Routes class
trait SingleRoutes extends ImportRoutes with ExportRoutes {
  // abstract system value will be provide by app
  implicit def system: ActorSystem
  implicit def materializer: ActorMaterializer

  // --- All Input Routes ---
  lazy val singleRoutes: Route = {
    concat(
      importRoutes,
      exportRoutes
    )
  }
}