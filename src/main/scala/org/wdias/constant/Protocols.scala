package org.wdias.constant

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

case class Station(name: String, latitude: Double, longitude: Double)

case class Unit(unit: String, `type`: String)

case class MetaData(station: Station, `type`: String, source: String, unit: Unit, tags: Array[String])

trait Protocols extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val stationFormat = jsonFormat3(Station.apply)
  implicit val unitFormat = jsonFormat2(Unit.apply)
  implicit val metaDataFormat = jsonFormat5(MetaData.apply)
}
