package ai.cog.utils

import java.io.FileOutputStream

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Multipart
import akka.stream.ActorMaterializer
import akka.util.ByteString

import scala.concurrent.Future

/**
  * Created by andyyoo on 6/05/16.
  */
object IOHelper {

  /**
    *
    * @param fileData
    * @param parallelism
    * @param system
    * @return
    */
  def buildByteArrayAsync(fileData: Multipart.FormData, parallelism: Int = 4)
                         (implicit system: ActorSystem): Future[Array[Tuple2[String, Future[Array[Byte]]]]] = {
    implicit val executor = system.dispatcher
    implicit val materializer = ActorMaterializer()

    fileData.parts.mapAsync(parallelism) { bodyPart =>
      def accumulateToArray(array: Array[Byte], byteString: ByteString): Array[Byte] = {
        val byteArray: Array[Byte] = byteString.toArray
        array ++ byteArray
      }
      println(s"param name: ${bodyPart.name}")
      Future(bodyPart.name, bodyPart.entity.dataBytes.runFold(Array[Byte]())(accumulateToArray))
    }.runFold(Array[Tuple2[String, Future[Array[Byte]]]]())((array, filenameAndByteTuple) => array :+ filenameAndByteTuple)
  }

  /**
    *
    * @param filePath
    * @param fileData
    * @param parallelism
    * @param system
    * @return
    */
  def saveFromFormDataAsync(filePath: String, fileData: Multipart.FormData, parallelism: Int = 1)
                   (implicit system: ActorSystem): Future[Int] = {
    implicit val executor = system.dispatcher
    implicit val materializer = ActorMaterializer()

    val fileOutput = new FileOutputStream(filePath)
    fileData.parts.mapAsync(parallelism) { bodyPart =>
      //println(s"param name: ${bodyPart.name}")
      // array: accumulator, byteString: next byteString
      def writeFileOnLocal(array: Array[Byte], byteString: ByteString): Array[Byte] = {
        val byteArray: Array[Byte] = byteString.toArray
        fileOutput.write(byteArray)
        array ++ byteArray
      }
      // start with empty byte array
      bodyPart.entity.dataBytes.runFold(Array[Byte]())(writeFileOnLocal)
    }.runFold(0){_ + _.length}
  }
}
