package org.wdias.adapter.metadata_adapter.models

import slick.jdbc.MySQLProfile.api._

trait DBComponent {
    val db = Database.forConfig("db-mysql")
}