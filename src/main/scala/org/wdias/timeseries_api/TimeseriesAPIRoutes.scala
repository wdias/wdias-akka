package org.wdias.timeseries_api

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

// REST API Routes class
trait TimeseriesAPIRoutes extends LocationRoutes with ParameterRoutes with TimeStepRoutes {
  // abstract system value will be provide by app
  implicit def system: ActorSystem
  implicit def materializer: ActorMaterializer

  // logging for InputRoutes
  lazy val logTimeseriesAPIRoutes = Logging(system, classOf[TimeseriesAPIRoutes])

  // --- All Input Routes ---
  lazy val timeseriesAPIRoutes: Route = {
    concat(
      locationRoutes,
      parameterRoutes,
      timeStepRoutes
    )
  }
}