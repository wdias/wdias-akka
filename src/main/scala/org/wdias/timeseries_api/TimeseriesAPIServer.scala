package org.wdias.timeseries_api

import akka.actor.{ActorRef, ActorSystem, DeadLetter, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.wdias.adapters.metadata_adapter.MetadataAdapter
import org.wdias.export.json.ExportJSON
import org.wdias.single.SingleServer.system
import org.wdias.util.DeadLetterMonitorActor

import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn

// REST API Main Class
object TimeseriesAPIServer extends App with TimeseriesAPIRoutes {
  // Load config
  val config = ConfigFactory.load()
  // Bootstrapping Server
  implicit val system: ActorSystem = ActorSystem("RESTAPIServer", config)
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  // Needed for the Future and its methods flatMap/onComplete in the end
  implicit val executionContext: ExecutionContext = system.dispatcher

  // Create Actors
  val exportJSONRef: ActorRef = system.actorOf(Props[ExportJSON], "exportJSON")
  implicit val metadataAdapterRef: ActorRef = system.actorOf(Props[MetadataAdapter], "metadataAdapter")
  // Util Actors
  val deadLetterMonitorActor = system.actorOf(Props[DeadLetterMonitorActor], name = "deadLetterMonitorActor")
  //subscribe to system wide event bus 'DeadLetter'
  system.eventStream.subscribe(deadLetterMonitorActor, classOf[DeadLetter])

  // From InputRoutes trait
  lazy val routes: Route = timeseriesAPIRoutes

  val serverBindingFuture: Future[Http.ServerBinding] = Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port-api"))

  logTimeseriesAPIRoutes.info("Server online at http://{}:{}/\nPress RETURN to stop...", config.getString("http.interface"), config.getInt("http.port-api"))

  StdIn.readLine()

  serverBindingFuture
    .flatMap(_.unbind())
    .onComplete { done =>
      done.failed.map { ex => logTimeseriesAPIRoutes.error(ex, "Failed unbinding") }
      system.terminate()
    }
}