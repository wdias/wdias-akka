package org.wdias.extensions.transformation

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.wdias.extensions._
import spray.json.{DefaultJsonProtocol, JsValue, RootJsonFormat}

object MyJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol with BaseExtensionProtocols {
  import spray.json._
  implicit object MyJsonFormat extends RootJsonFormat[Array[Variable]] {
    def write(v: Array[Variable]) = JsArray(v.map(_.toJson).toVector)

    def read(value: JsValue) = value match {
      case JsArray(elements) => elements.map(_.convertTo[Variable]).toArray[Variable]
      case x => deserializationError("Expected Array as JsArray, but got " + x)
    }
  }
}

case class TransformationExtension(
                      override val extensionId: String,
                      override val extension: String,
                      override val function: String,
                      override val trigger: Trigger,
                      variables: Array[Variable],
                      inputVariables: Array[String],
                      outputVariables: Array[String],
                      options: JsValue
                    ) extends BaseExtension {
  def toTransformationExtensionObj: TransformationExtensionObj = {
    import spray.json._
    val options = this.options.toString

    import MyJsonProtocol._
    val variables = this.variables.toJson.toString

    TransformationExtensionObj(this.extensionId, variables, this.inputVariables.mkString(","), this.outputVariables.mkString(","), options)
  }
  def toExtensionObj: ExtensionObj = {
    ExtensionObj(this.extensionId, this.extension, this.function, "%s:%s".format(this.trigger.`type`, this.trigger.data.mkString(",")), this.trigger.`type`, this.trigger.data.mkString(","))
  }
}

case class TransformationExtensionObj(
                                       extensionId: String,
                                       variables: String,
                                       inputVariables: String,
                                       outputVariables: String,
                                       options: String
                                     ) {
  def toTransformationExtension(e: Extension): TransformationExtension = {
    import spray.json._
    val options = this.options.parseJson

    import MyJsonProtocol._
    val variables: Array[Variable] = this.variables.parseJson.convertTo[Array[Variable]]

    TransformationExtension(this.extensionId, e.extension, e.function, e.trigger, variables, this.inputVariables.split(","), this.outputVariables.split(","), options)
  }
  def toTransformationExtensionWithExtensionObj(e: ExtensionObj): TransformationExtension = {
    import spray.json._
    val options = this.options.parseJson

    import MyJsonProtocol._
    val variables = this.variables.parseJson.convertTo[Array[Variable]]

    TransformationExtension(this.extensionId, e.extension, e.function, Trigger(e.triggerType, e.triggerData.split(",")), variables, this.inputVariables.split(","), this.outputVariables.split(","), options)
  }
}

trait TransformationProtocols extends BaseExtensionProtocols {
  implicit val transformationExtensionFormat: RootJsonFormat[TransformationExtension] = jsonFormat8(TransformationExtension.apply)
}
