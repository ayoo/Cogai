package ai.cog.actors

import java.io.FileOutputStream

import S3FileUploader.MultiPartForm
import ai.cog.utils.{AWSHelper, IOHelper}
import akka.actor.{Actor, ReceiveTimeout}
import akka.http.scaladsl.model.Multipart

import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Created by andyyoo on 5/05/16.
  */
object S3FileUploader {
  sealed trait UploadableForm
  case class MultiPartForm(val fileName: String,
                           val fileData: Multipart.FormData,
                           val localCopy: Option[Boolean]) extends UploadableForm
}

/**
  * This is the request per actor and needs to be stopped at the end
  */
class S3FileUploader extends Actor{
  implicit val system = context.system
  implicit val executor = system.dispatcher

  context.setReceiveTimeout(5.seconds)

  override def receive: Receive = {

    case MultiPartForm(filename, fileData, localCopy) =>
      val baFuture = IOHelper.buildByteArrayAsync(fileData)
      baFuture.map { array =>
        array.foreach { filenameAndByteTuple =>
          if (filenameAndByteTuple._1 == "file") {
            filenameAndByteTuple._2.onComplete {
              case Success(byteArray) =>
                AWSHelper.S3.put(filename, byteArray) match {
                  case Success(_) =>
                    if(localCopy.getOrElse(false))
                      new FileOutputStream(filename).write(byteArray)
                  case Failure(ex) => throw ex
                }
              case Failure(ex) => throw ex
            }
          }
        }
      }
      context stop self

    case ReceiveTimeout =>
      context stop self
  }
}
