package org.wdias.adapter.models

import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

case class Location(id: String, name: String)

class Locations(tag: Tag) extends Table[Location](tag, "LOCATIONS") {
  def id = column[String]("LOCATION_ID", O.PrimaryKey) // This is the primary key column
  def name = column[String]("LOCATION_NAME")
  def lat = column[Float]("LATITUDE")
  def lon = column[Float]("LONGITUDE")
  def elevation = column[Float]("LONGITUDE")
  def description = column[String]("DESCRIPTION")
  // Every table needs a * projection with the same type as the table's type parameter
  def * = (id, name) <> (Location.tupled, Location.unapply)
}

object LocationsDAO extends TableQuery(new Locations(_)) with DBComponent {

  def findById(id: String): Future[Option[Location]] = {
    db.run(this.filter(_.id === id).result).map(_.headOption)
  }

  def create(location: Location): Future[Int] = {
    val tables = List(LocationsDAO)

    val existing = db.run(MTable.getTables)
    val f = existing.flatMap( v => {
      val names = v.map(mt => mt.name.name)
      val createIfNotExist = tables.filter( table =>
        !names.contains(table.baseTableRow.tableName)).map(_.schema.create)
      db.run(DBIO.sequence(createIfNotExist))
    })
    Await.result(f, Duration.Inf)

    // db.run(this returning this.map(_.id) into ((acc, id) => acc.copy(id = id)) += location)
    db.run(this += location)
  }

  def deleteById(id: String): Future[Int] = {
    db.run(this.filter(_.id === id).delete)
  }
}
