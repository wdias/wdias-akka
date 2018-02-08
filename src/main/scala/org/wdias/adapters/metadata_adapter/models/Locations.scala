package org.wdias.adapters.metadata_adapter.models

import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import org.wdias.constant.Location

class Locations(tag: Tag) extends Table[Location](tag, "LOCATIONS") {
  def locationId = column[String]("LOCATION_ID", O.PrimaryKey) // This is the primary key column
  def name = column[String]("LOCATION_NAME")
  def lat = column[Float]("LATITUDE")
  def lon = column[Float]("LONGITUDE")
  def elevation = column[Option[Float]]("ELEVATION")
  def description = column[Option[String]]("DESCRIPTION")
  // Every table needs a * projection with the same type as the table's type parameter
  def * = (locationId, name, lat, lon, elevation, description) <> (Location.tupled, Location.unapply)
}

object LocationsDAO extends TableQuery(new Locations(_)) with DBComponent {

  def findById(locationId: String): Future[Option[Location]] = {
    db.run(this.filter(_.locationId === locationId).result).map(_.headOption)
  }

  def find(locationId: String, name: String): Future[Seq[Location]] = {
    val q1 = if(locationId.isEmpty) this else this.filter(_.locationId === locationId)
    val q2 = if(name.isEmpty) q1 else q1.filter(_.name === name)
    val action = q2.result
    db.run(action)
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

  def deleteById(locationId: String): Future[Int] = {
    db.run(this.filter(_.locationId === locationId).delete)
  }
}
