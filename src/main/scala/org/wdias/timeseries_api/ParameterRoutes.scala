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
import thredds.catalog.ThreddsMetadata.Variable

import scala.concurrent.Future
import scala.concurrent.duration._

// Parameter REST API Routes class
trait ParameterRoutes extends Protocols {
  // abstract system value will be provide by app
  implicit def system: ActorSystem
  implicit def materializer: ActorMaterializer

  // logging for InputRoutes
  lazy val logParameterRoutes = Logging(system, classOf[ParameterRoutes])

  // Actor dependencies required for ParameterRoutes
  def metadataAdapterRef: ActorRef

  // Required by the `ask` (?) method below
  implicit lazy val timeoutParameterRoutes: Timeout = Timeout(15.seconds) // TODO: Obtain from config

  // --- All Input Routes ---
  lazy val parameterRoutes: Route = {
    concat(
      pathPrefix("parameter") {
        concat(
          // GET: Get Parameter
          (get & pathPrefix(Segment)) { parameterId: String =>
            logParameterRoutes.info("/parameter GET request: > {}", parameterId)
            val response = (metadataAdapterRef ? GetParameterById(parameterId)).mapTo[Option[ParameterObj]]
            onSuccess(response) { parameterObj: Option[ParameterObj] =>
              complete(Created -> parameterObj.getOrElse(ParameterObj("","","",ParameterType.Instantaneous)).toParameter)
            }
          },
          // GET: Query on Parameters
          (get & pathEnd & parameters('parameterId.as[String].?, 'variable.as[String].?, 'unit.as[String].?, 'parameterType.as[String].?)) { (parameterId, variable, unit, parameterType) =>
            logParameterRoutes.info("/parameter GET request: List")
            val response: Future[Seq[ParameterObj]] = (metadataAdapterRef ? GetParameters(parameterId.getOrElse(""), variable.getOrElse(""), unit.getOrElse(""), parameterType.getOrElse(""))).mapTo[Seq[ParameterObj]]
            onSuccess(response) { parameterObjs: Seq[ParameterObj] =>
              complete(Created -> parameterObjs.map(_.toParameter))
            }
          },

          // POST: Create Parameter
          (post & entity(as[Parameter])) { parameter: Parameter =>
            logParameterRoutes.info("/parameter POST request: > {}", parameter)
            val response: Future[Int] = (metadataAdapterRef ? CreateParameter(parameter.toParameterObj)).mapTo[Int]
            onSuccess(response) { isCreated: Int =>
              complete(Created -> isCreated.toString)
            }
          },
          // PUT: Replace Parameter
          (put & pathPrefix(Segment)) { parameterId: String =>
            entity(as[Parameter]) { parameter: Parameter =>
              logParameterRoutes.info("/parameter/point GET request: Replace {} : {}", parameterId, parameter)
              complete(Created -> "Replace")
            }
          },
          // PATCH: Update Parameter
          (patch & pathPrefix(Segment)) { parameterId: String =>
            // TODO: Read fields separately
            entity(as[Parameter]) { parameter: Parameter =>
              logParameterRoutes.info("/parameter/point GET request: Update {} : {}", parameterId, parameter)
              complete(Created -> "Update")
            }
          },
          // DELETE: Delete Parameter
          (delete & pathPrefix(Segment)) { parameterId: String =>
            logParameterRoutes.info("/parameter/point DELETE request: > {}", parameterId)
            val response: Future[Int] = (metadataAdapterRef ? DeleteParameterById(parameterId)).mapTo[Int]
            onSuccess(response) { isDeleted: Int =>
              complete(Created -> isDeleted.toString)
            }
          }
        )
      }
    )
  }
}