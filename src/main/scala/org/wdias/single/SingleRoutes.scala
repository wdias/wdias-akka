package org.wdias.`import`

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import org.wdias.export.ExportRoutes
import org.wdias.extensions.ExtensionRoutes
import org.wdias.timeseries_api.TimeseriesAPIRoutes

// Input Routes class
trait SingleRoutes extends ImportRoutes with ExportRoutes with TimeseriesAPIRoutes with ExtensionRoutes {
  // abstract system value will be provide by app
  implicit def system: ActorSystem
  implicit def materializer: ActorMaterializer

  // logging for InputRoutes
  lazy val logSingleRoutes = Logging(system, classOf[SingleRoutes])

  // --- All Input Routes ---
  lazy val singleRoutes: Route = {
    concat(
      importRoutes,
      exportRoutes,
      timeseriesAPIRoutes,
      extensionRoutes
    )
  }
}