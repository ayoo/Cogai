package ai.cog.utils

import com.typesafe.config.ConfigFactory

/**
  * Created by andyyoo on 30/04/16.
  */
trait Config {
  val config = ConfigFactory.load()
  val apiConfig = config.getConfig("api")
  val awsConfig = config.getConfig("aws")
  val s3Config = awsConfig.getConfig("s3")
  val sparkConfig = config.getConfig("spark")

  val apiHost = apiConfig.getString("host")
  val apiPort = apiConfig.getInt("port")
  val awsAccessKey = awsConfig.getString("accesskey")
  val awsSecretKey = awsConfig.getString("secretkey")
  val s3Bucket = s3Config.getString("bucket")
  val sparkMaster = sparkConfig.getString("master")
  val sparkAppName = sparkConfig.getString("appName")
}

object AppConfig extends Config