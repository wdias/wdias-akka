package org.wdias.`import`

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.wdias.`import`.csv.ImportCSV
import org.wdias.`import`.json.ImportJSON
import org.wdias.adapters.extension_adapter.ExtensionAdapter
import org.wdias.adapters.grid_adapter.GridAdapter
import org.wdias.extensions.ExtensionHandler

import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn

// Input Main Class
object ImportServer extends App with ImportRoutes {
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
  implicit val adapter: ActorRef = system.actorOf(Props[GridAdapter], "adapter")
  val importJSONRef: ActorRef = system.actorOf(Props[ImportJSON], "importJSON")
  val importCSVRef: ActorRef = system.actorOf(Props[ImportCSV], "importCSV")

  // From InputRoutes trait
  lazy val routes: Route = importRoutes

  val serverBindingFuture: Future[Http.ServerBinding] = Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port-input"))

  logImportRoutes.info("Server online at http://{}:{}/\nPress RETURN to stop...", config.getString("http.interface"), config.getInt("http.port-input"))

  StdIn.readLine()

  serverBindingFuture
    .flatMap(_.unbind())
    .onComplete { done =>
      done.failed.map { ex => logImportRoutes.error(ex, "Failed unbinding") }
      system.terminate()
    }
}