package org.wdias.extensions.transformation

import org.json4s.DefaultFormats
import org.wdias.extensions._
import spray.json.RootJsonFormat

case class TransformationExtension(
                      override val extensionId: String,
                      override val extension: String,
                      override val function: String,
                      override val trigger: Trigger,
                      variables: Array[Variable],
                      inputVariables: Array[String],
                      outputVariables: Array[String],
                      options: String
                    ) extends BaseExtension {
  def toTransformationExtensionObj: TransformationExtensionObj = {
    import org.json4s._
    import org.json4s.native.Serialization
    import org.json4s.native.Serialization.write
//    implicit val formats = Serialization.formats(ShortTypeHints(List(classOf[Variable])))
    implicit val formats = DefaultFormats

    val variables = write(this.variables)
    TransformationExtensionObj(this.extensionId, variables, this.inputVariables.mkString(","), this.outputVariables.mkString(","), this.options)
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
    import org.json4s._
    import org.json4s.native.Serialization
    import org.json4s.native.Serialization.read
//    implicit val formats = Serialization.formats(ShortTypeHints(List(classOf[Variable])))
    implicit val formats = DefaultFormats

    val variables = read(this.variables)
    TransformationExtension(this.extensionId, e.extension, e.function, e.trigger, variables, this.inputVariables.split(","), this.outputVariables.split(","), this.options)
  }
  def toTransformationExtensionWithExtensionObj(e: ExtensionObj): TransformationExtension = {
    import org.json4s.native.Serialization.read
    implicit val formats = DefaultFormats
    val variables = read(this.variables)
    TransformationExtension(this.extensionId, e.extension, e.function, Trigger(e.triggerType, e.triggerData.split(",")), variables, this.inputVariables.split(","), this.outputVariables.split(","), this.options)
  }
}

case class TransformationMeta(name: String, latitude: Double, longitude: Double)

trait TransformationProtocols extends BaseExtensionProtocols {
  implicit val transformationExtensionFormat: RootJsonFormat[TransformationExtension] = jsonFormat8(TransformationExtension.apply)
}