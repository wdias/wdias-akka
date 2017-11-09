package org.wdias.api

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.wdias.adapter.Adapter
import org.wdias.export.{ExportCSV, ExportJSON}

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
  implicit val adapter: ActorRef = system.actorOf(Props[Adapter], "adapter")
  val exportJSONRef: ActorRef = system.actorOf(Props[ExportJSON], "exportJSON")
  val exportCSVRef: ActorRef = system.actorOf(Props[ExportCSV], "exportCSV")

  // From InputRoutes trait
  lazy val routes: Route = restAPIRoutes

  val serverBindingFuture: Future[Http.ServerBinding] = Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port1"))

  println(s"Server online at http://localhost:8081/\nPress RETURN to stop...")

  StdIn.readLine()

  serverBindingFuture
    .flatMap(_.unbind())
    .onComplete { done =>
      done.failed.map { ex => log.error(ex, "Failed unbinding") }
      system.terminate()
    }
}