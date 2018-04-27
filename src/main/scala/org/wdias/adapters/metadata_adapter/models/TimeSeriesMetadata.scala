package org.wdias.adapters.metadata_adapter.models

import java.security.MessageDigest

import org.wdias.constant.ValueType.ValueType
import org.wdias.constant.TimeSeriesType.TimeSeriesType
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import org.wdias.constant.{TimeSeriesType, ValueType}
import org.wdias.constant._


class TimeSeriesMetadataTable(tag: Tag) extends Table[MetadataIdsObj](tag, "TIME_SERIES_METADATA") {
  implicit val valueTypeMapper: JdbcType[ValueType] with BaseTypedType[ValueType] = MappedColumnType.base[ValueType, String](
    e => e.toString,
    s => ValueType.withName(s)
  )
  implicit val timeSeriesTypeMapper: JdbcType[TimeSeriesType] with BaseTypedType[TimeSeriesType] = MappedColumnType.base[TimeSeriesType, String](
    e => e.toString,
    s => TimeSeriesType.withName(s)
  )

  def timeSeriesId = column[String]("TIME_SERIES_ID", O.Length(255)) //  O.PrimaryKey
  def moduleId = column[String]("MODULE_ID", O.Length(255)) //
  def valueType = column[ValueType]("VALUE_TYPE") //
  def parameterId = column[String]("PARAMETER_ID", O.Length(255)) // Foreign Constrain
  def locationId = column[String]("LOCATION_ID", O.Length(255)) // Foreign Constrain
  def timeSeriesType = column[TimeSeriesType]("TIME_SERIES_TYPE") //
  def timeStepId = column[String]("TIME_STEP_ID", O.Length(255)) // Foreign Constrain
  // TODO: Add `tags` support

  override def * = (timeSeriesId, moduleId, valueType, parameterId, locationId, timeSeriesType, timeStepId) <> (MetadataIdsObj.tupled, MetadataIdsObj.unapply)

  def pk = primaryKey("pk_a", timeSeriesId)
  // TODO: Not working for the moment
  // def idx = index("idx_metadata_ids", (moduleId, valueType, parameterId, locationId, timeSeriesType, timeStepId), unique = true)

  def parameters = foreignKey("PARAMETER_ID_FK", parameterId, ParametersDAO)(_.parameterId, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

  def locations = foreignKey("LOCATION_ID_FK", locationId, LocationsDAO)(_.locationId, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

  def timeSteps = foreignKey("TIME_STEP_ID_FK", timeStepId, TimeStepsDAO)(_.timeStepId, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
}

object TimeSeriesMetadataDAO extends TableQuery(new TimeSeriesMetadataTable(_)) with DBComponent {

  def findById(timeseriesId: String): Future[Option[MetadataIdsObj]] = {
    db.run(this.filter(_.timeSeriesId === timeseriesId).result).map(_.headOption)
  }

  def find(timeSeriesId: Option[String], moduleId: Option[String], valueType: Option[String], parameterId: Option[String], locationId: Option[String], timeSeriesType: Option[TimeSeriesType], timeStepId: Option[String]): Future[Seq[MetadataIdsObj]] = {
    val q1 = if (timeSeriesId.isEmpty) this else this.filter(_.timeSeriesId === timeSeriesId.get)
    val q2 = if (moduleId.isEmpty) q1 else q1.filter(_.moduleId === moduleId)
    val q3 = if (valueType.isEmpty) q2 else q2.filter(_.valueType.asInstanceOf[Rep[String]] === valueType.get)
    val q4 = if (parameterId.isEmpty) q3 else q3.filter(_.parameterId === parameterId.get)
    val q5 = if (locationId.isEmpty) q4 else q4.filter(_.locationId === locationId.get)
    val q6 = if (timeSeriesType.isEmpty) q5 else q5.filter(_.timeSeriesType.asInstanceOf[Rep[String]] === timeSeriesType.get.toString)
    val q7 = if (timeStepId.isEmpty) q6 else q6.filter(_.timeStepId === timeStepId.get)
    val action = q7.result
    db.run(action)
  }

  def getTimeseriesHash(timeseriesHash: TimeseriesHash): String = {
    MessageDigest.getInstance("SHA-256")
      .digest(timeseriesHash.toString.getBytes("UTF-8"))
      .map("%02x".format(_)).mkString
  }

  def create(metadataIdsObj: MetadataIdsObj): Future[Int] = {
    val tables = List(TimeSeriesMetadataDAO)

    val timeseriesHash = TimeseriesHash(metadataIdsObj.moduleId, metadataIdsObj.valueType.toString, metadataIdsObj.parameterId, metadataIdsObj.locationId, metadataIdsObj.timeSeriesType.toString, metadataIdsObj.timeStepId)
    val timeseriesId = this.getTimeseriesHash(timeseriesHash)
    val metadataIdsObj2 = metadataIdsObj.copy(timeSeriesId = timeseriesId)

    val existing = db.run(MTable.getTables)
    val f = existing.flatMap(v => {
      val names = v.map(mt => mt.name.name)
      val createIfNotExist = tables.filter(table =>
        !names.contains(table.baseTableRow.tableName)).map(_.schema.create)
      db.run(DBIO.sequence(createIfNotExist))
    })
    Await.result(f, Duration.Inf)

    // db.run(this returning this.map(_.id) into ((acc, id) => acc.copy(id = id)) += location)
     db.run(this += metadataIdsObj2)
  }

  def upsert(metadataIdsObj: MetadataIdsObj): Future[Int] = {
    val tables = List(TimeSeriesMetadataDAO)

    val timeseriesHash = TimeseriesHash(metadataIdsObj.moduleId, metadataIdsObj.valueType.toString, metadataIdsObj.parameterId, metadataIdsObj.locationId, metadataIdsObj.timeSeriesType.toString, metadataIdsObj.timeStepId)
    val timeseriesId = this.getTimeseriesHash(timeseriesHash)
    val metadataIdsObj2 = metadataIdsObj.copy(timeSeriesId = timeseriesId)

    val existing = db.run(MTable.getTables)
    val f = existing.flatMap(v => {
      val names = v.map(mt => mt.name.name)
      val createIfNotExist = tables.filter(table =>
        !names.contains(table.baseTableRow.tableName)).map(_.schema.create)
      db.run(DBIO.sequence(createIfNotExist))
    })
    Await.result(f, Duration.Inf)

    db.run(this.insertOrUpdate(metadataIdsObj2))
  }

  def deleteById(timeSeriesId: String): Future[Int] = {
    db.run(this.filter(_.timeSeriesId === timeSeriesId).delete)
  }
}