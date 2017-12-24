package org.wdias.adapter.models

import org.wdias.constant.ValueType.ValueType
import org.wdias.constant.TimeSeriesType.TimeSeriesType
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.MTable
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import org.wdias.constant.{ValueType, TimeSeriesType}
import org.wdias.constant._


class TimeSeriesMetadataTable(tag: Tag) extends Table[MetadataIds](tag, "TIME_SERIES_METADATA") {
  implicit val valueTypeMapper: JdbcType[ValueType] with BaseTypedType[ValueType] = MappedColumnType.base[ValueType, String](
    e => e.toString,
    s => ValueType.withName(s)
  )
  implicit val timeSeriesTypeMapper: JdbcType[TimeSeriesType] with BaseTypedType[TimeSeriesType] = MappedColumnType.base[TimeSeriesType, String](
    e => e.toString,
    s => TimeSeriesType.withName(s)
  )

  def timeSeriesId = column[String]("TIME_SERIES_ID", O.PrimaryKey) //
  def moduleId = column[String]("MODULE_ID", O.Unique, O.Length(255)) //
  def valueType = column[ValueType]("VALUE_TYPE") //
  def parameterId = column[String]("PARAMETER_ID", O.Length(255)) // Foreign Constrain
  def locationId = column[String]("LOCATION_ID", O.Length(255)) // Foreign Constrain
  def timeSeriesType = column[TimeSeriesType]("TIME_SERIES_TYPE") //
  def timeStepId = column[String]("TIME_STEP_ID", O.Length(255)) // Foreign Constrain

  override def * = (timeSeriesId, moduleId, valueType, parameterId, locationId, timeSeriesType, timeStepId) <> (MetadataIds.tupled, MetadataIds.unapply)

  def parameters = foreignKey("PARAMETER_ID_FK", parameterId, ParametersDAO)(_.parameterId, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

  def locations = foreignKey("LOCATION_ID_FK", locationId, LocationsDAO)(_.locationId, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

  def timeSteps = foreignKey("TIME_STEP_ID_FK", timeStepId, TimeStepsDAO)(_.timeStepId, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
}

object TimeSeriesMetadataDAO extends TableQuery(new TimeSeriesMetadataTable(_)) with DBComponent {

  def findById(timeSeriesId: String): Future[Option[MetadataIds]] = {
    db.run(this.filter(_.timeSeriesId === timeSeriesId).result).map(_.headOption)
  }

  def create(timeSeriesMetadata: MetadataIds): Future[Int] = {
    val tables = List(TimeSeriesMetadataDAO)

    val existing = db.run(MTable.getTables)
    val f = existing.flatMap(v => {
      val names = v.map(mt => mt.name.name)
      val createIfNotExist = tables.filter(table =>
        !names.contains(table.baseTableRow.tableName)).map(_.schema.create)
      db.run(DBIO.sequence(createIfNotExist))
    })
    Await.result(f, Duration.Inf)

    // db.run(this returning this.map(_.id) into ((acc, id) => acc.copy(id = id)) += location)
    db.run(this += timeSeriesMetadata)
  }

  def deleteById(timeSeriesId: String): Future[Int] = {
    db.run(this.filter(_.timeSeriesId === timeSeriesId).delete)
  }
}