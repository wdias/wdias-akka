package org.wdias.input

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.wdias.`import`.{ImportCSV, ImportJSON}
import org.wdias.adapter.Adapter
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
  implicit val adapter: ActorRef = system.actorOf(Props[Adapter], "adapter")
  val importJSONRef: ActorRef = system.actorOf(Props[ImportJSON], "importJSON")
  val importCSVRef: ActorRef = system.actorOf(Props[ImportCSV], "importCSV")

  // From InputRoutes trait
  lazy val routes: Route = inputRoutes

  val serverBindingFuture: Future[Http.ServerBinding] = Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")

  StdIn.readLine()

  serverBindingFuture
    .flatMap(_.unbind())
    .onComplete { done =>
      done.failed.map { ex => log.error(ex, "Failed unbinding") }
      system.terminate()
    }
}