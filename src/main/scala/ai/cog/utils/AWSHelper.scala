package ai.cog.utils

import java.io.ByteArrayInputStream

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectResult}

import scala.util.{Failure, Success, Try}

/**
  * Created by andyyoo on 6/05/16.
  */
object AWSHelper extends Config {

  val credentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey)
  val awsS3Client = new AmazonS3Client(credentials)
  //awsS3Client.setRegion(Region.getRegion(Regions.AP_SOUTHEAST_2))

  val client = new ClientConfiguration()
  client.setSocketTimeout(300000)

  object S3 {
    def put(filename: String, byteArray: Array[Byte]): Try[PutObjectResult] = {
      val inputStream = new ByteArrayInputStream(byteArray)
      Try(awsS3Client.putObject(s3Bucket, filename, inputStream, new ObjectMetadata()))
    }

    def fileExist(filename: String): Option[Boolean] = {
      Try(awsS3Client.getObjectMetadata(s3Bucket, filename)) match {
        case Success(_) => Some(true)
        case Failure(ex) => Some(false)
      }
    }
  }

}
