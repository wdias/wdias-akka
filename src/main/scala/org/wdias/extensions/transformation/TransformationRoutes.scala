package org.wdias.extensions.transformation

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes.{Created, OK}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.wdias.adapters.extension_adapter.ExtensionAdapter._
import org.wdias.extensions.ExtensionObj

import scala.concurrent.Future
import scala.concurrent.duration._

// Transformation REST API Routes class
trait TransformationRoutes extends TransformationProtocols {
  // abstract system value will be provide by app
  implicit def system: ActorSystem
  implicit def materializer: ActorMaterializer

  // logging for TransformationRoutes
  lazy val logTransformationRoutes = Logging(system, classOf[TransformationRoutes])

  // Actor dependencies required for TransformationRoutes
  def extensionAdapterRef: ActorRef

  // Required by the `ask` (?) method below
  implicit lazy val timeoutTransformationRoutes: Timeout = Timeout(15.seconds) // TODO: Obtain from config

  // --- All Input Routes ---
  lazy val transformationRoutes: Route = {
    concat(
      pathPrefix("extension" / "transformation") {
        concat(
          // GET: Get Extension / Transformation
          (get & pathPrefix(Segment)) { extensionId: String =>
            logTransformationRoutes.info("/extension/transformation GET request: > {}", extensionId)
            // Get Extension
            val response: Future[Option[ExtensionObj]] = (extensionAdapterRef ? GetExtensionById(extensionId)).mapTo[Option[ExtensionObj]]
            onSuccess(response) { extensionObj: Option[ExtensionObj] =>
              val extension = extensionObj.getOrElse(ExtensionObj("", "", "", "", "", "")).toExtension
              logTransformationRoutes.info("Got Extension: {}", extension)
              val a = TransformationExtensionObj("", "[]", "[]", "[]", "{}")
              // Get Transformation
              if (extension.extensionId.isEmpty) {
                complete(OK -> a.toTransformationExtension(extension))
              } else {
                val response2: Future[Option[TransformationExtensionObj]] = (extensionAdapterRef ? GetTransformationById(extension.extensionId)).mapTo[Option[TransformationExtensionObj]]
                onSuccess(response2) { transformationExtensionObj: Option[TransformationExtensionObj] =>
                  logTransformationRoutes.info("Got TransformationExtension: {}", transformationExtensionObj.getOrElse(a))
                  complete(Created -> transformationExtensionObj.getOrElse(a).toTransformationExtension(extension))
                }
              }
            }
          },
          // GET: Query on Extension / Transformations
          (get & pathEnd & parameters('extensionId.as[String].?, 'extension.as[String].?, 'function.as[String].?)) { (extensionId, extension, function) =>
            logTransformationRoutes.info("/extension/transformation GET request: List")
            // Get Extension
            val response: Future[Seq[ExtensionObj]] = (extensionAdapterRef ? GetExtensions(extensionId.getOrElse(""), extension.getOrElse(""), function.getOrElse(""))).mapTo[Seq[ExtensionObj]]
            onSuccess(response) { extensionObjs: Seq[ExtensionObj] =>
              // TODO: Embed TransformationExtension data also.
              // val a = TransformationExtensionObj("", "[]", "[]", "[]", "{}")
              complete(Created -> extensionObjs.map(_.toExtension))
            }
          },

          // POST: Create Extension / Transformation
          (post & entity(as[TransformationExtension])) { transformationExtension: TransformationExtension =>
            logTransformationRoutes.info("/extension/transformation POST request: > {}", transformationExtension.variables)
            val response: Future[Int] = (extensionAdapterRef ? CreateExtension(transformationExtension.toExtensionObj)).mapTo[Int]
            onSuccess(response) { isCreated: Int =>
              if(isCreated > 0) {
                val response2: Future[Int] = (extensionAdapterRef ? CreateTransformation(transformationExtension.toTransformationExtensionObj)).mapTo[Int]
                onSuccess(response2) { isCreated2: Int =>
                  complete(Created -> isCreated2.toString)
                }
              } else {
                complete(Created -> isCreated.toString)
              }
            }
          },
          // PUT: Replace Extension / Transformation
          (put & pathPrefix(Segment)) { extensionId: String =>
            entity(as[TransformationExtension]) { transformationExtension: TransformationExtension =>
              logTransformationRoutes.info("/extension/transformation GET request: Replace {} : {}", extensionId, transformationExtension)
              complete(Created -> "Replace")
            }
          },
          // PATCH: Update Extension / Transformation
          (patch & pathPrefix(Segment)) { extensionId: String =>
            // TODO: Read fields separately
            entity(as[TransformationExtension]) { transformationExtension: TransformationExtension =>
              logTransformationRoutes.info("/extension/transformation GET request: Update {} : {}", extensionId, transformationExtension)
              complete(Created -> "Update")
            }
          },
          // DELETE: Delete Extension / Transformation
          (delete & pathPrefix(Segment)) { extensionId: String =>
            logTransformationRoutes.info("/extension/transformation DELETE request: > {}", extensionId)
            val response: Future[Int] = (extensionAdapterRef ? DeleteExtensionById(extensionId)).mapTo[Int]
            onSuccess(response) { isCreated: Int =>
              if(isCreated > 0) {
                val response2: Future[Int] = (extensionAdapterRef ? DeleteTransformationById(extensionId)).mapTo[Int]
                onSuccess(response2) { isCreated2: Int =>
                  complete(Created -> isCreated2.toString)
                }
              } else {
                complete(Created -> isCreated.toString)
              }
            }
          }
        )
      }
    )
  }
}