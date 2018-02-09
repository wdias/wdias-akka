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

// Timeseries REST API Routes class
trait TimeseriesRoutes extends Protocols {
  // abstract system value will be provide by app
  implicit def system: ActorSystem

  implicit def materializer: ActorMaterializer

  // logging for InputRoutes
  lazy val logTimeseriesRoutes = Logging(system, classOf[TimeseriesRoutes])

  // Actor dependencies required for TimeseriesRoutes
  def metadataAdapterRef: ActorRef

  // Required by the `ask` (?) method below
  implicit lazy val timeoutTimeseriesRoutes: Timeout = Timeout(15.seconds) // TODO: Obtain from config

  // --- All Input Routes ---
  lazy val timeseriesRoutes: Route = {
    concat(
      pathPrefix("timeseries") {
        concat(
          // GET: Get Timeseries
          (get & pathPrefix(Segment)) { timeseriesId: String =>
            logTimeseriesRoutes.info("/timeseries GET request: > {}", timeseriesId)
            val response: Future[Option[MetadataIdsObj]] = (metadataAdapterRef ? GetTimeseriesById(timeseriesId)).mapTo[Option[MetadataIdsObj]]
            onSuccess(response) { metadataIds: Option[MetadataIdsObj] =>
              complete(Created -> metadataIds.getOrElse(MetadataIdsObj("", "", ValueType.Scalar, "", "", TimeSeriesType.ExternalHistorical, "")).toMetadataIds)
            }
          },
          // GET: Query on Timeseries
          (get & pathEnd & parameters('timeseriesId.as[String].?, 'moduleId.as[String].?, 'valueType.as[String].?, 'parameterId.as[String].?, 'locationId.as[String].?, 'timeseriesType.as[String].?, 'timeStepId.as[String].?)) { (timeSeriesId, moduleId, valueType, parameterId, locationId, timeSeriesType, timeStepId) =>
            logTimeseriesRoutes.info("/timeseries GET request: List")
            val response: Future[Seq[MetadataIdsObj]] = (metadataAdapterRef ? GetTimeseries(timeSeriesId.getOrElse(""), moduleId.getOrElse(""), valueType.getOrElse(""), parameterId.getOrElse(""), locationId.getOrElse(""), timeSeriesType.getOrElse(""), timeStepId.getOrElse(""))).mapTo[Seq[MetadataIdsObj]]
            onSuccess(response) { timeseries: Seq[MetadataIdsObj] =>
              complete(Created -> timeseries.map(_.toMetadataIds))
            }
          },

          // POST: Create Timeseries With Ids
          (post & path("ids") & entity(as[MetadataIds])) { metadataIds: MetadataIds =>
            logTimeseriesRoutes.info("/timeseries/ids POST request: > {}", metadataIds)
            val response: Future[Int] = (metadataAdapterRef ? CreateTimeseriesWithIds(metadataIds.toMetadataIdsObj)).mapTo[Int]
            onSuccess(response) { isCreated: Int =>
              complete(Created -> isCreated.toString)
            }
          },
          // POST: Create Timeseries
          (post & entity(as[Metadata])) { metadata: Metadata =>
            logTimeseriesRoutes.info("/timeseries POST request: > {}", metadata)
            val response: Future[Int] = (metadataAdapterRef ? CreateTimeseries(metadata.toMetadataObj)).mapTo[Int]
            onSuccess(response) { isCreated: Int =>
              complete(Created -> isCreated.toString)
            }
          },
          // PUT: Replace Timeseries
          (put & pathPrefix(Segment)) { timeseriesId: String =>
            entity(as[Metadata]) { metadata: Metadata =>
              logTimeseriesRoutes.info("/timeseries GET request: Replace {} : {}", timeseriesId, metadata)
              complete(Created -> "Replace")
            }
          },
          // PATCH: Update Timeseries
          (patch & pathPrefix(Segment)) { timeseriesId: String =>
            // TODO: Read fields separately
            entity(as[Metadata]) { metadata: Metadata =>
              logTimeseriesRoutes.info("/timeseries GET request: Update {} : {}", timeseriesId, metadata)
              complete(Created -> "Update")
            }
          },
          // DELETE: Delete Timeseries
          (delete & pathPrefix(Segment)) { timeseriesId: String =>
            logTimeseriesRoutes.info("/timeseries DELETE request: > {}", timeseriesId)
            val response: Future[Int] = (metadataAdapterRef ? DeleteTimeseriesById(timeseriesId)).mapTo[Int]
            onSuccess(response) { isDeleted: Int =>
              complete(Created -> isDeleted.toString)
            }
          }
        )
      }
    )
  }
}