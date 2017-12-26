package org.wdias.api

import akka.actor.{ActorRef, ActorSystem, DeadLetter, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.wdias.adapter.{Adapter, ExtensionAdapter}
import org.wdias.export.{ExportCSV, ExportJSON}
import org.wdias.extensions.ExtensionHandler
import org.wdias.input.InputServer.system
import org.wdias.util.DeadLetterMonitorActor

import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn

// REST API Main Class
object APIServer extends App with RESTAPIRoutes {
  // Load config
  val config = ConfigFactory.load()
  // Bootstrapping Server
  implicit val system: ActorSystem = ActorSystem("RESTAPIServer", config)
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  // Needed for the Future and its methods flatMap/onComplete in the end
  implicit val executionContext: ExecutionContext = system.dispatcher

  // Create Actors
  implicit val extensionAdapterRef: ActorRef = system.actorOf(Props[ExtensionAdapter], "extensionAdapter")
  implicit val extensionHandlerRef: ActorRef = system.actorOf(Props[ExtensionHandler], "extensionHandler")
  implicit val adapter: ActorRef = system.actorOf(Props[Adapter], "adapter")
  val exportJSONRef: ActorRef = system.actorOf(Props[ExportJSON], "exportJSON")
  val exportCSVRef: ActorRef = system.actorOf(Props[ExportCSV], "exportCSV")
  // Util Actors
  val deadLetterMonitorActor = system.actorOf(Props[DeadLetterMonitorActor], name = "deadLetterMonitorActor")
  //subscribe to system wide event bus 'DeadLetter'
  system.eventStream.subscribe(deadLetterMonitorActor, classOf[DeadLetter])

  // From InputRoutes trait
  lazy val routes: Route = restAPIRoutes

  val serverBindingFuture: Future[Http.ServerBinding] = Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port-api"))

  log.info(s"Server online at http://{}:{}/\nPress RETURN to stop...", config.getString("http.interface"), config.getInt("http.port-api"))

  StdIn.readLine()

  serverBindingFuture
    .flatMap(_.unbind())
    .onComplete { done =>
      done.failed.map { ex => log.error(ex, "Failed unbinding") }
      system.terminate()
    }
}