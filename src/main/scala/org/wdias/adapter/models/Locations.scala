package org.wdias.adapter.models

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import slick.dbio.DBIOAction
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import akka.event.Logging
import slick.jdbc.meta.MTable

case class Location(id: String, name: String)

class Locations(tag: Tag) extends Table[Location](tag, "LOCATIONS") {
  def id = column[String]("LOCATION_ID", O.PrimaryKey) // This is the primary key column
  def name = column[String]("LOCATION_NAME")
  // Every table needs a * projection with the same type as the table's type parameter
  def * = (id, name) <> (Location.tupled, Location.unapply)
}

//val locations = TableQuery[Locations]

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
