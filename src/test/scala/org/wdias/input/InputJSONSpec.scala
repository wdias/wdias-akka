package org.wdias.input

import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.scalatest.{Matchers, WordSpec}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server._
import Directives._
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.wdias.`import`.ImportJSON
import org.wdias.`import`.ImportJSON.ImportJSONData
import org.wdias.adapter.Adapter
import org.wdias.constant._
import org.wdias.input.OnDemandInput.system

import scala.concurrent.duration._

class InputJSONSpec extends WordSpec
    with Matchers
    with ScalatestRouteTest
    with BeforeAndAfterAll
    with Service {
    override val config = ConfigFactory.load()

    override def afterAll {
        TestKit.shutdownActorSystem(system)
    }

    "OnDemandInput" should {
        "return success for POST request with valid JSON data" in {
            val metaData: MetaData = new MetaData(Station("Hanwella", 6.90, 80.08), "Observed", "HEC-HMS", Unit("m3/s", "Instantaneous"), Array("Test1"))
            val timeseries: TimeSeries = new TimeSeries(List(
                new DataPoint("2017-09-15 00:00:00", 0.0),
                new DataPoint("2017-09-15 01:00:00", 0.1),
                new DataPoint("2017-09-15 02:00:00", 0.2),
                new DataPoint("2017-09-15 03:00:00", 0.3),
            ))
            Post("/observed123", TimeSeriesEnvelop(metaData, Some(timeseries), None)) ~> routes ~> check {
                status shouldEqual StatusCodes.Created
                responseAs[String] shouldEqual "Success true"
            }
        }
    }
    override implicit val importJSONRef: ActorRef = system.actorOf(Props[ImportJSON], "importJSON")
}