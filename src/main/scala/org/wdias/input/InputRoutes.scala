package org.wdias.input

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes.Created
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.util.Timeout
import org.wdias.`import`.ImportJSON.ImportJSONData
import org.wdias.adapter.Adapter.StoreSuccess
import org.wdias.constant.{Protocols, TimeSeriesEnvelop}

import scala.concurrent.Future
import scala.concurrent.duration._

// Input Routes class
trait InputRoutes extends Protocols {
    // abstract system value will be provide by app
    implicit def system: ActorSystem

    // logging for InputRoutes
    lazy val log = Logging(system, classOf[InputRoutes])

    // Other dependencies required by InputRoutes
    def importJSONRef: ActorRef

    // Required by the `ask` (?) method below
    implicit lazy val timeout: Timeout = Timeout(5.seconds) // TODO: Obtain from config

    // --- All Input Routes ---
    lazy val inputRoutes: Route = {
        pathPrefix("observed") {
            (post & entity(as[TimeSeriesEnvelop])) { timeSeriesEnvelop =>
                val response: Future[StoreSuccess] = (importJSONRef ? ImportJSONData(timeSeriesEnvelop)).mapTo[StoreSuccess]
                onSuccess(response) { result =>
                complete(Created -> result.metadata)
            }
            }
        }
    }
}