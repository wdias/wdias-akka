package org.wdias.adapter.models

import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object TimeStepUnit extends Enumeration {
  type TimeStepUnit = Value
  val Second: TimeStepUnit.Value = Value("Second")
  val Minute: TimeStepUnit.Value = Value("Minute")
  val Hour: TimeStepUnit.Value = Value("Hour")
  val Day: TimeStepUnit.Value = Value("Day")
  val Week: TimeStepUnit.Value = Value("Week")
  val Month: TimeStepUnit.Value = Value("Month")
  val Year: TimeStepUnit.Value = Value("Year")
  val NonEquidistant: TimeStepUnit.Value = Value("NonEquidistant")

  implicit val timeStepUnit: JdbcType[TimeStepUnit] with BaseTypedType[TimeStepUnit] = MappedColumnType.base[TimeStepUnit, String](
    e => e.toString,
    s => TimeStepUnit.withName(s)
  )
}

import org.wdias.adapter.models.TimeStepUnit._

case class TimeStep(timeStepId: String, unit: TimeStepUnit, multiplier: Int, divider: Int)

class TimeSteps(tag: Tag) extends Table[TimeStep](tag, "TIMESTEPS") {
  def timeStepId = column[String]("TIME_STEP_ID", O.PrimaryKey)

  def unit = column[TimeStepUnit]("UNIT")

  def multiplier = column[Int]("MULTIPLIER")

  def divider = column[Int]("DIVIDER")

  override def * = (timeStepId, unit, multiplier, divider) <> (TimeStep.tupled, TimeStep.unapply)
}

object TimeStepsDAO extends TableQuery(new TimeSteps(_)) with DBComponent {
  def findById(timeStepId: String): Future[Option[TimeStep]] = {
    db.run(this.filter(_.timeStepId === timeStepId).result).map(_.headOption)
  }

  def create(timeStep: TimeStep): Future[Int] = {
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

  def deleteById(timeStepId: String): Future[Int] = {
    db.run(this.filter(_.timeStepId === timeStepId).delete)
  }
}