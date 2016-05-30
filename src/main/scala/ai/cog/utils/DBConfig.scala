package ai.cog.utils

import slick.driver.{JdbcProfile, MySQLDriver}

/**
  * Created by andyyoo on 16/05/16.
  */
trait DBConfig {
  val driver: JdbcProfile
  val db: driver.api.Database
}

trait MySqlConfig extends DBConfig {
  import slick.driver.MySQLDriver.api._
  val driver:JdbcProfile = MySQLDriver
  val db = Database.forConfig("mysql")
}