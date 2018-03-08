package org.wdias.adapters.extension_adapter.models

import slick.jdbc.MySQLProfile.api._

trait DBComponent {
  val db = Database.forConfig("db-mysql-extension")
}