package org.wdias.adapter.models

import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.MTable
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object ValueType extends Enumeration {
  type ValueType = Value
  val Scalar: ValueType.Value = Value("Scalar")
  val Vector: ValueType.Value = Value("Vector")
  val Grid: ValueType.Value = Value("Grid")

  implicit val valueTypeMapper: JdbcType[ValueType] with BaseTypedType[ValueType] = MappedColumnType.base[ValueType, String](
    e => e.toString,
    s => ValueType.withName(s)
  )
}

import org.wdias.adapter.models.ValueType._

object TimeSeriesType extends Enumeration {
  type TimeSeriesType = Value
  val ExternalHistorical: TimeSeriesType.Value = Value("ExternalHistorical")
  val ExternalForecasting: TimeSeriesType.Value = Value("ExternalForecasting")
  val SimulatedHistorical: TimeSeriesType.Value = Value("SimulatedHistorical")
  val SimulatedForecasting: TimeSeriesType.Value = Value("SimulatedForecasting")

  implicit val timeSeriesTypeMapper: JdbcType[TimeSeriesType] with BaseTypedType[TimeSeriesType] = MappedColumnType.base[TimeSeriesType, String](
    e => e.toString,
    s => TimeSeriesType.withName(s)
  )
}

import org.wdias.adapter.models.TimeSeriesType._

case class TimeSeriesMetadata(timeSeriesId: String, moduleId: String, valueType: ValueType, parameterId: String, locationId: String, timeSeriesType: TimeSeriesType, timeStepId: String)

class TimeSeriesMetadataTable(tag: Tag) extends Table[TimeSeriesMetadata](tag, "TIME_SERIES_METADATA") {
  def timeSeriesId = column[String]("TIME_SERIES_ID", O.PrimaryKey) //
  def moduleId = column[String]("MODULE_ID", O.Unique) //
  def valueType = column[ValueType]("VALUE_TYPE", O.Unique) //
  def parameterId = column[String]("PARAMETER_ID", O.Unique) // Foreign Constrain
  def locationId = column[String]("LOCATION_ID", O.Unique) // Foreign Constrain
  def timeSeriesType = column[TimeSeriesType]("TIME_SERIES_TYPE", O.Unique) //
  def timeStepId = column[String]("TIME_STEP_ID", O.Unique) // Foreign Constrain

  override def * = (timeSeriesId, moduleId, valueType, parameterId, locationId, timeSeriesType, timeStepId) <> (TimeSeriesMetadata.tupled, TimeSeriesMetadata.unapply)

//  def parameters = foreignKey("PARAMETER_ID_FK", parameterId, ParametersDAO)
//
//  def locations = foreignKey("LOCATION_ID_FK", locationId, LocationsDAO)
//
//  def timeSteps = foreignKey("TIME_STEP_ID_FK", timeStepId, TimeStepsDAO)
}

object TimeSeriesMetadataDAO extends TableQuery(new TimeSeriesMetadataTable(_)) with DBComponent {

  def findById(timeSeriesId: String): Future[Option[TimeSeriesMetadata]] = {
    db.run(this.filter(_.timeSeriesId === timeSeriesId).result).map(_.headOption)
  }

  def create(timeSeriesMetadata: TimeSeriesMetadata): Future[Int] = {
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