import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import org.wdias.`import`.ImportJSON
import org.wdias.adapter.Adapter
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, RequestEntity}
import akka.stream.ActorMaterializer
import org.wdias.constant._
import org.wdias.input.Service
import org.wdias.input.OnDemandInput.{bindingFuture, routes, system}
import akka.http.scaladsl.model._

import scala.concurrent.Future
import scala.util.{Failure, Success}

class InputJSONSysSpec() extends TestKit(ActorSystem("input-api"))
  with App
  with Service
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  implicit val config: Config = ConfigFactory.load()
  override implicit val executor = system.dispatcher
  override implicit val materializer: ActorMaterializer = ActorMaterializer()

  val adapter = system.actorOf(Props[Adapter], "adapter")
  val importJSONRef: ActorRef = system.actorOf(Props[ImportJSON], "importJSON")

  val bindingFuture: Future[Http.ServerBinding] = Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))

  override def afterAll {
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate() foreach { _ =>
        println("Actor system was shut down")
      })
  }

  "An Echo actor" must {

    "send back messages unchanged" in {
      val p = Parameter("1234", "Precipitation", "mm", "Accumulative")
      val l = Location("wdias_hanwella", "Hanwella", 6.90f, 80.08f)
      val t = TimeStep("every_5_min", "Minutes", Option(5))
      val metaData: MetaData = new MetaData("ModuleTest", "Scalar", p, l, "ExternalHistorical", t, Array("Test1"))
      val timeseries: TimeSeries = new TimeSeries(List(
        new DataPoint("2017-09-15 00:00:00", 0.0),
        new DataPoint("2017-09-15 01:00:00", 0.1),
        new DataPoint("2017-09-15 02:00:00", 0.2),
        new DataPoint("2017-09-15 03:00:00", 0.3)
      ))

      Marshal(timeseries).to[RequestEntity] map { entity =>
        println(entity)
        val request: Future[HttpResponse] =
          Http().singleRequest(HttpRequest(HttpMethods.POST, uri = "http://localhost:8000/observed123", entity = entity))
        request onComplete {
          case Failure(ex) =>
            println(ex)
            System.out.println(s"Failed to post, reason: $ex")
          case Success(response) =>
            System.out.println(s"Server responded with $response")
            expectMsg("Hello")
        }
      }
    }

  }
}