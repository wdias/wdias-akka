package org.wdias.export

import akka.event.Logging
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.wdias.export.csv.ExportCSVRoutes
import org.wdias.export.json.ExportJSONRoutes

// REST API Routes class
trait ExportRoutes extends ExportJSONRoutes with ExportCSVRoutes {
  // logging for InputRoutes
  lazy val logExportRoutes = Logging(system, classOf[ExportRoutes])
  // --- All Input Routes ---
  lazy val exportRoutes: Route = {
    concat(
      exportJSONRoutes,
      exportCSVRoutes
    )
  }
}