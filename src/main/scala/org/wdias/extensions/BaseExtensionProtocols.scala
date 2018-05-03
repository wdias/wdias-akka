package org.wdias.extensions

import org.wdias.constant.{Metadata, MetadataIds, Protocols}
import spray.json.RootJsonFormat

case class Variable(
                   variableId: String,
                   timeSeriesId: Option[String] = Option(null),
                   timeSeries: Option[Metadata] = Option(null),
                   timeSeriesWithIds: Option[MetadataIds] = Option(null)
                   )

case class Trigger(
                  `type`: String, // TODO: Replace with Enum of ['onChange', 'onTime']
                  data: Array[String]
                  )

trait BaseExtension {
  def extensionId: String
  def extension: String // TODO: Convert to Enum and load via application.conf
  def function: String
  def trigger: Trigger
}

case class Extension(
                      override val extensionId: String,
                      override val extension: String,
                      override val function: String,
                      override val trigger: Trigger,
                    ) extends BaseExtension {
  def toExtensionObj: ExtensionObj = ExtensionObj(this.extensionId, this.extension, this.function, "%s:%s".format(this.trigger.`type`, this.trigger.data.mkString(",")), this.trigger.`type`, this.trigger.data.mkString(","))
}

case class ExtensionObj(
                      extensionId: String,
                      extension: String,
                      function: String,
                      trigger: String, // TODO: Slick is not supporting JSON format - https://github.com/json4s/json4s
                      triggerType: String,
                      triggerData: String,
                    ) {
  def toExtension: Extension = Extension(this.extensionId, this.extension, this.function, Trigger(this.triggerType, this.triggerData.split(",").map(_.trim)))
}

// Internal Triggering
case class TriggerExtension(extensionObj: ExtensionObj)

trait BaseExtensionProtocols extends Protocols {
  implicit val variableFormat: RootJsonFormat[Variable] = jsonFormat4(Variable.apply)
  implicit val triggerFormat: RootJsonFormat[Trigger] = jsonFormat2(Trigger.apply)
  implicit val extensionFormat: RootJsonFormat[Extension] = jsonFormat4(Extension.apply)
}