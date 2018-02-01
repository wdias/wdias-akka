package org.wdias.adapter.models

import slick.jdbc.MySQLProfile.api._

trait DBComponent {
    val db = Database.forConfig("db-mysql")
}