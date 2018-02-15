package org.wdias.single

import akka.actor.{ActorRef, ActorSystem, DeadLetter, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.wdias.`import`.SingleRoutes
import org.wdias.`import`.csv.ImportCSV
import org.wdias.`import`.json.ImportJSON
import org.wdias.adapters.extension_adapter.ExtensionAdapter
import org.wdias.adapters.grid_adapter.GridAdapter
import org.wdias.adapters.metadata_adapter.MetadataAdapter
import org.wdias.adapters.scalar_adapter.ScalarAdapter
import org.wdias.adapters.vector_adapter.VectorAdapter
import org.wdias.api.{ArchiveHandler, QueryHandler, StatusHandler}
import org.wdias.export.csv.ExportCSV
import org.wdias.export.json.ExportJSON
import org.wdias.extensions.ExtensionHandler
import org.wdias.extensions.interpolation.Interpolation
import org.wdias.extensions.transformation.Transformation
import org.wdias.extensions.validation.Validation
import org.wdias.util.DeadLetterMonitorActor

import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn

// REST API Main Class
object SingleServer extends App with SingleRoutes {
  // Load config
  val config = ConfigFactory.load()
  // Bootstrapping Server
  implicit val system: ActorSystem = ActorSystem("SingleServer", config)
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  // Needed for the Future and its methods flatMap/onComplete in the end
  implicit val executionContext: ExecutionContext = system.dispatcher

  // Create Extension Actors
  implicit val extensionAdapterRef: ActorRef = system.actorOf(Props[ExtensionAdapter], "extensionAdapter")
  implicit val extensionHandlerRef: ActorRef = system.actorOf(Props[ExtensionHandler], "extensionHandler")
  implicit val transformationRef: ActorRef = system.actorOf(Props[Transformation], "transformation")
  implicit val interpolationRef: ActorRef = system.actorOf(Props[Interpolation], "interpolation")
  implicit val validationRef: ActorRef = system.actorOf(Props[Validation], "validation")

  // Create Adapters
  implicit val metadataAdapterRef: ActorRef = system.actorOf(Props[MetadataAdapter], "metadataAdapter")
  implicit val scalarAdapterRef: ActorRef = system.actorOf(Props[ScalarAdapter], "scalarAdapter")
  implicit val vectorAdapterRef: ActorRef = system.actorOf(Props[VectorAdapter], "vectorAdapter")
  implicit val gridAdapterRef: ActorRef = system.actorOf(Props[GridAdapter], "gridAdapter")

  // Create Export Actors
  val exportJSONRef: ActorRef = system.actorOf(Props[ExportJSON], "exportJSON")
  val exportCSVRef: ActorRef = system.actorOf(Props[ExportCSV], "exportCSV")

  // Create Import Actors
  val importJSONRef: ActorRef = system.actorOf(Props[ImportJSON], "importJSON")
  val importCSVRef: ActorRef = system.actorOf(Props[ImportCSV], "importCSV")

  // Create API Actors
  implicit val statusHandlerRef: ActorRef = system.actorOf(Props[StatusHandler], "statusHandler")
  implicit val queryHandlerRef: ActorRef = system.actorOf(Props[QueryHandler], "queryHandler")
  implicit val archiveHandlerRef: ActorRef = system.actorOf(Props[ArchiveHandler], "archiveHandler")

  // Util Actors
  val deadLetterMonitorActor = system.actorOf(Props[DeadLetterMonitorActor], name = "deadLetterMonitorActor")
  //subscribe to system wide event bus 'DeadLetter'
  system.eventStream.subscribe(deadLetterMonitorActor, classOf[DeadLetter])

  // From InputRoutes trait
  lazy val routes: Route = singleRoutes

  val serverBindingFuture: Future[Http.ServerBinding] = Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port-single"))

  logSingleRoutes.info("Server online at http://{}:{}/\nPress RETURN to stop...", config.getString("http.interface"), config.getInt("http.port-single"))

  StdIn.readLine()

  serverBindingFuture
    .flatMap(_.unbind())
    .onComplete { done =>
      done.failed.map { ex => logImportCSVRoutes.error(ex, "Failed unbinding") }
      system.terminate()
    }
}