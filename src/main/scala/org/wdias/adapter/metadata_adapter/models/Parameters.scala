package org.wdias.adapter.metadata_adapter.models

import org.wdias.constant.ParameterType.ParameterType
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import org.wdias.constant._


class Parameters(tag: Tag) extends Table[ParameterObj](tag, "PARAMETERS") {
  implicit val parameterTypeMapper: JdbcType[ParameterType] with BaseTypedType[ParameterType] = MappedColumnType.base[ParameterType, String](
    e => e.toString,
    s => ParameterType.withName(s)
  )

  def parameterId = column[String]("PARAMETER_ID", O.PrimaryKey)

  def variable = column[String]("VARIABLE")

  def unit = column[String]("UNIT")

  def parameterType = column[ParameterType]("PARAMETER_TYPE")

  override def * = (parameterId, variable, unit, parameterType) <> (ParameterObj.tupled, ParameterObj.unapply)
}

object ParametersDAO extends TableQuery(new Parameters(_)) with DBComponent {

  def findById(parameterId: String): Future[Option[ParameterObj]] = {
    db.run(this.filter(_.parameterId === parameterId).result).map(_.headOption)
  }

  def create(parameter: ParameterObj): Future[Int] = {
    val tables = List(ParametersDAO)

    val existing = db.run(MTable.getTables)
    val f = existing.flatMap(v => {
      val names = v.map(mt => mt.name.name)
      val createIfNotExist = tables.filter(table =>
        !names.contains(table.baseTableRow.tableName)).map(_.schema.create)
      db.run(DBIO.sequence(createIfNotExist))
    })
    Await.result(f, Duration.Inf)

    // db.run(this returning this.map(_.id) into ((acc, id) => acc.copy(id = id)) += location)
    db.run(this += parameter)
  }

  def deleteById(parameterId: String): Future[Int] = {
    db.run(this.filter(_.parameterId === parameterId).delete)
  }
}