package org.wdias.adapters.metadata_adapter.models

import org.wdias.constant.TimeStepUnit
import org.wdias.constant.TimeStepUnit.TimeStepUnit
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import org.wdias.constant._


class TimeSteps(tag: Tag) extends Table[TimeStepObj](tag, "TIMESTEPS") {
  implicit val timeStepUnit: JdbcType[TimeStepUnit] with BaseTypedType[TimeStepUnit] = MappedColumnType.base[TimeStepUnit, String](
    e => e.toString,
    s => TimeStepUnit.withName(s)
  )

  def timeStepId = column[String]("TIME_STEP_ID", O.PrimaryKey)

  def unit = column[TimeStepUnit]("UNIT")

  def multiplier = column[Option[Int]]("MULTIPLIER")

  def divider = column[Option[Int]]("DIVIDER")

  override def * = (timeStepId, unit, multiplier, divider) <> (TimeStepObj.tupled, TimeStepObj.unapply)
}

object TimeStepsDAO extends TableQuery(new TimeSteps(_)) with DBComponent {
  def findById(timeStepId: String): Future[Option[TimeStepObj]] = {
    db.run(this.filter(_.timeStepId === timeStepId).result).map(_.headOption)
  }

  def find(timeStepId: String, unit: String, multiplier: Int, divider: Int): Future[Seq[TimeStepObj]] = {
    val q1 = if (timeStepId.isEmpty) this else this.filter(_.timeStepId === timeStepId)
    val q2 = if (unit.isEmpty) q1 else q1.filter(_.unit.asInstanceOf[Rep[String]] === unit)
    val q3 = if (multiplier == 0) q2 else q2.filter(_.multiplier === multiplier)
    val q4 = if (divider == 0) q3 else q3.filter(_.divider === divider)
    val action = q4.result
    db.run(action)
  }

  def create(timeStep: TimeStepObj): Future[Int] = {
    val tables = List(TimeStepsDAO)

    val existing = db.run(MTable.getTables)
    val f = existing.flatMap(v => {
      val names = v.map(mt => mt.name.name)
      val createIfNotExist = tables.filter(table =>
        !names.contains(table.baseTableRow.tableName)).map(_.schema.create)
      db.run(DBIO.sequence(createIfNotExist))
    })
    Await.result(f, Duration.Inf)

    // db.run(this returning this.map(_.id) into ((acc, id) => acc.copy(id = id)) += location)
    db.run(this += timeStep)
  }

  def upsert(timeStepObj: TimeStepObj): Future[Int] = {
    val tables = List(TimeStepsDAO)

    val existing = db.run(MTable.getTables)
    val f = existing.flatMap(v => {
      val names = v.map(mt => mt.name.name)
      val createIfNotExist = tables.filter(table =>
        !names.contains(table.baseTableRow.tableName)).map(_.schema.create)
      db.run(DBIO.sequence(createIfNotExist))
    })
    Await.result(f, Duration.Inf)

    db.run(this.insertOrUpdate(timeStepObj))
  }

  def deleteById(timeStepId: String): Future[Int] = {
    db.run(this.filter(_.timeStepId === timeStepId).delete)
  }
}