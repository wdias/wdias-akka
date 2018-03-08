package org.wdias.adapters.extension_adapter.models

import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import org.wdias.extensions.transformation.TransformationExtensionObj

class Transformations(tag: Tag) extends Table[TransformationExtensionObj](tag, "TRANSFORMATIONS") {
  def extensionId = column[String]("EXTENSION_ID", O.PrimaryKey) // This is the primary key column
  def variables = column[String]("VARIABLES")
  def inputVariables = column[String]("INPUT_VARIABLES")
  def outputVariables = column[String]("OUTPUT_VARIABLES")
  def options = column[String]("OPTIONS")
  // Every table needs a * projection with the same type as the table's type parameter
  def * = (extensionId, variables, inputVariables, outputVariables, options) <> (TransformationExtensionObj.tupled, TransformationExtensionObj.unapply)
}

object TransformationsDAO extends TableQuery(new Transformations(_)) with DBComponent {

  def findById(extensionId: String): Future[Option[TransformationExtensionObj]] = {
    db.run(this.filter(_.extensionId === extensionId).result).map(_.headOption)
  }

  def find(extensionId: String): Future[Seq[TransformationExtensionObj]] = {
    val q1 = if(extensionId.isEmpty) this else this.filter(_.extensionId === extensionId)
    val action = q1.result
    db.run(action)
  }

  def create(transformationExtension: TransformationExtensionObj): Future[Int] = {
    val tables = List(TransformationsDAO)

    val existing = db.run(MTable.getTables)
    val f = existing.flatMap( v => {
      val names = v.map(mt => mt.name.name)
      val createIfNotExist = tables.filter( table =>
        !names.contains(table.baseTableRow.tableName)).map(_.schema.create)
      db.run(DBIO.sequence(createIfNotExist))
    })
    Await.result(f, Duration.Inf)

    // db.run(this returning this.map(_.id) into ((acc, id) => acc.copy(id = id)) += extension)
    db.run(this += transformationExtension)
  }

  def deleteById(extensionId: String): Future[Int] = {
    db.run(this.filter(_.extensionId === extensionId).delete)
  }
}
