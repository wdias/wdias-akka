package org.wdias.adapters.extension_adapter.models

import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import org.wdias.extensions.ExtensionObj

class Extensions(tag: Tag) extends Table[ExtensionObj](tag, "EXTENSIONS") {
  def extensionId = column[String]("EXTENSION_ID", O.PrimaryKey) // This is the primary key column
  def extension = column[String]("EXTENSION")
  def function = column[String]("FUNCTION")
  def trigger = column[String]("TRIGGER")
  def triggerType = column[String]("TRIGGER_TYPE")
  def triggerData = column[String]("TRIGGER_DATA")
  // Every table needs a * projection with the same type as the table's type parameter
  def * = (extensionId, extension, function, trigger, triggerType, triggerData) <> (ExtensionObj.tupled, ExtensionObj.unapply)
}

object ExtensionsDAO extends TableQuery(new Extensions(_)) with DBComponent {

  def findById(extensionId: String): Future[Option[ExtensionObj]] = {
    db.run(this.filter(_.extensionId === extensionId).result).map(_.headOption)
  }

  def find(extensionId: String, extension: String, function: String): Future[Seq[ExtensionObj]] = {
    val q1 = if(extensionId.isEmpty) this else this.filter(_.extensionId === extensionId)
    val q2 = if(extension.isEmpty) q1 else q1.filter(_.extension === extension)
    val q3 = if(function.isEmpty) q2 else q1.filter(_.function === function)
    val action = q3.result
    db.run(action)
  }

  def create(extension: ExtensionObj): Future[Int] = {
    val tables = List(ExtensionsDAO)

    val existing = db.run(MTable.getTables)
    val f = existing.flatMap( v => {
      val names = v.map(mt => mt.name.name)
      val createIfNotExist = tables.filter( table =>
        !names.contains(table.baseTableRow.tableName)).map(_.schema.create)
      db.run(DBIO.sequence(createIfNotExist))
    })
    Await.result(f, Duration.Inf)

    // db.run(this returning this.map(_.id) into ((acc, id) => acc.copy(id = id)) += extension)
    db.run(this += extension)
  }

  def deleteById(extensionId: String): Future[Int] = {
    db.run(this.filter(_.extensionId === extensionId).delete)
  }
}
