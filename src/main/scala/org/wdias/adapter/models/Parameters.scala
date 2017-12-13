package org.wdias.adapter.models

import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

// An algebraic data type for booleans
//sealed trait ParameterType
//case object Instantaneous extends ParameterType
//case object Accumulative extends ParameterType
//
//object ParameterTypeMapper {
//  val string_enum_mapping:Map[String, ParameterType] = Map(
//    "Instantaneous" -> Instantaneous,
//    "Accumulative" -> Accumulative
//  )
//  val enum_string_mapping:Map[ParameterType, String] = string_enum_mapping.map(_.swap)
//  implicit val parameterTypeStringMapper = MappedTypeMapper.base[ParameterType,String](
//    e => enum_string_mapping(e),
//    s => string_enum_mapping(s)
//  )
//}

object MyEnum extends Enumeration {
  type MyEnum = Value
  val A = Value("a")
  val B = Value("b")
  val C = Value("c")

  implicit val myEnumMapper = MappedColumnType.base[MyEnum, String](
    e => e.toString,
    s => MyEnum.withName(s)
  )
}

import MyEnum._

case class Parameter(parameterId: String, variable:String, unit:String, parameterType:MyEnum)

class Parameters(tag: Tag) extends Table[Parameter](tag, "PARAMETERS") {
  def parameterId = column[String]("PARAMETER_ID", O.PrimaryKey)
  def variable = column[String]("VARIABLE")
  def unit = column[String]("UNIT")
  def parameterType = column[MyEnum]("PARAMETER_TYPE")

  override def * = (parameterId, variable, unit, parameterType) <> (Parameter.tupled, Parameter.unapply)
}

object ParametersDAO extends TableQuery(new Parameters(_)) with DBComponent {

  def findById(parameterId: String): Future[Option[Parameter]] = {
    db.run(this.filter(_.parameterId === parameterId).result).map(_.headOption)
  }

  def create(parameter: Parameter): Future[Int] = {
    val tables = List(ParametersDAO)

    val existing = db.run(MTable.getTables)
    val f = existing.flatMap( v => {
      val names = v.map(mt => mt.name.name)
      val createIfNotExist = tables.filter( table =>
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