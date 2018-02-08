package org.wdias.`import`

import akka.event.Logging
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.wdias.`import`.csv.ImportCSVRoutes
import org.wdias.`import`.json.ImportJSONRoutes

// Input Routes class
trait ImportRoutes extends ImportJSONRoutes with ImportCSVRoutes {
  // logging for InputRoutes
  lazy val logImportRoutes = Logging(system, classOf[ImportRoutes])
  // --- All Input Routes ---
  lazy val importRoutes: Route = {
    concat(
      importJSONRoutes,
      importCSVRoutes
    )
  }
}