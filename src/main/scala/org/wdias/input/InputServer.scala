package org.wdias.input

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.wdias.`import`.{ImportCSV, ImportJSON}
import org.wdias.adapter.{Adapter, ExtensionAdapter}
import org.wdias.extensions.ExtensionHandler
import org.wdias.input.OnDemandInput.{config, routes}

import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn

// Input Main Class
object InputServer extends App with InputRoutes {
  // Load config
  val config = ConfigFactory.load()
  // Bootstrapping Server
  implicit val system: ActorSystem = ActorSystem("InputServer", config)
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  // Needed for the Future and its methods flatMap/onComplete in the end
  implicit val executionContext: ExecutionContext = system.dispatcher

  // Create Actors
  implicit val extensionAdapterRef: ActorRef = system.actorOf(Props[ExtensionAdapter], "extensionAdapter")
  implicit val extensionHandlerRef: ActorRef = system.actorOf(Props[ExtensionHandler], "extensionHandler")
  implicit val adapter: ActorRef = system.actorOf(Props[Adapter], "adapter")
  val importJSONRef: ActorRef = system.actorOf(Props[ImportJSON], "importJSON")
  val importCSVRef: ActorRef = system.actorOf(Props[ImportCSV], "importCSV")

  // From InputRoutes trait
  lazy val routes: Route = inputRoutes

  val serverBindingFuture: Future[Http.ServerBinding] = Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port-input"))

  log.info(s"Server online at http://{}:{}/\nPress RETURN to stop...", config.getString("http.interface"), config.getInt("http.port-input"))

  StdIn.readLine()

  serverBindingFuture
    .flatMap(_.unbind())
    .onComplete { done =>
      done.failed.map { ex => log.error(ex, "Failed unbinding") }
      system.terminate()
    }
}