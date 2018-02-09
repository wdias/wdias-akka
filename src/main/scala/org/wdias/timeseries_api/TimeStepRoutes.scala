package org.wdias.timeseries_api

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes.Created
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.wdias.adapters.metadata_adapter.MetadataAdapter._
import org.wdias.constant.{Protocols, _}

import scala.concurrent.Future
import scala.concurrent.duration._

// TimeStep REST API Routes class
trait TimeStepRoutes extends Protocols {
  // abstract system value will be provide by app
  implicit def system: ActorSystem

  implicit def materializer: ActorMaterializer

  // logging for InputRoutes
  lazy val logTimeStepRoutes = Logging(system, classOf[TimeStepRoutes])

  // Actor dependencies required for TimeStepRoutes
  def metadataAdapterRef: ActorRef

  // Required by the `ask` (?) method below
  implicit lazy val timeoutTimeStepRoutes: Timeout = Timeout(15.seconds) // TODO: Obtain from config

  // --- All Input Routes ---
  lazy val timeStepRoutes: Route = {
    concat(
      pathPrefix("timeStep") {
        concat(
          // GET: Get TimeStep
          (get & pathPrefix(Segment)) { timeStepId: String =>
            logTimeStepRoutes.info("/timeStep GET request: > {}", timeStepId)
            val response = (metadataAdapterRef ? GetTimeStepById(timeStepId)).mapTo[Option[TimeStepObj]]
            onSuccess(response) { timeStepObj: Option[TimeStepObj] =>
              complete(Created -> timeStepObj.getOrElse(TimeStepObj("", TimeStepUnit.Second)).toTimeStep)
            }
          },
          // GET: Query on TimeSteps
          (get & pathEnd & parameters('timeStepId.as[String].?, 'unit.as[String].?, 'multiplier.as[Int].?, 'divider.as[Int].?)) { (timeStepId, unit, multiplier, divider) =>
            logTimeStepRoutes.info("/timeStep GET request: List")
            val response: Future[Seq[TimeStepObj]] = (metadataAdapterRef ? GetTimeSteps(timeStepId.getOrElse(""), unit.getOrElse(""), multiplier.getOrElse(0), divider.getOrElse(0))).mapTo[Seq[TimeStepObj]]
            onSuccess(response) { timeStepObjs: Seq[TimeStepObj] =>
              complete(Created -> timeStepObjs.map(_.toTimeStep))
            }
          },

          // POST: Create TimeStep
          (post & entity(as[TimeStep])) { timeStep: TimeStep =>
            logTimeStepRoutes.info("/timeStep POST request: > {}", timeStep)
            val response: Future[Int] = (metadataAdapterRef ? CreateTimeStep(timeStep.toTimeStepObj)).mapTo[Int]
            onSuccess(response) { isCreated: Int =>
              complete(Created -> isCreated.toString)
            }
          },
          // PUT: Replace TimeStep
          (put & pathPrefix(Segment)) { timeStepId: String =>
            entity(as[TimeStep]) { timeStep: TimeStep =>
              logTimeStepRoutes.info("/timeStep/point GET request: Replace {} : {}", timeStepId, timeStep)
              complete(Created -> "Replace")
            }
          },
          // PATCH: Update TimeStep
          (patch & pathPrefix(Segment)) { timeStepId: String =>
            // TODO: Read fields separately
            entity(as[TimeStep]) { timeStep: TimeStep =>
              logTimeStepRoutes.info("/timeStep/point GET request: Update {} : {}", timeStepId, timeStep)
              complete(Created -> "Update")
            }
          },
          // DELETE: Delete TimeStep
          (delete & pathPrefix(Segment)) { timeStepId: String =>
            logTimeStepRoutes.info("/timeStep/point DELETE request: > {}", timeStepId)
            val response: Future[Int] = (metadataAdapterRef ? DeleteTimeStepById(timeStepId)).mapTo[Int]
            onSuccess(response) { isDeleted: Int =>
              complete(Created -> isDeleted.toString)
            }
          }
        )
      }
    )
  }
}