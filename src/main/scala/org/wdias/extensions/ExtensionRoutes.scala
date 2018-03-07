package org.wdias.extensions

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import org.wdias.extensions.interpolation.InterpolationRoutes
import org.wdias.extensions.transformation.TransformationRoutes
import org.wdias.extensions.validation.ValidationRoutes

// Extension REST API Routes class
trait ExtensionRoutes extends InterpolationRoutes with TransformationRoutes with ValidationRoutes {
  // abstract system value will be provide by app
  implicit def system: ActorSystem

  implicit def materializer: ActorMaterializer

  // logging for InputRoutes
  lazy val logExtensionRoutes = Logging(system, classOf[ExtensionRoutes])

  // --- All Input Routes ---
  lazy val extensionRoutes: Route = {
    concat(
      interpolationRoutes,
      transformationRoutes,
      validationRoutes
    )
  }
}