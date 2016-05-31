package ai.cog.services

import java.io.FileOutputStream
import java.util.UUID

import ai.cog.actors.S3FileUploader
import ai.cog.actors.S3FileUploader.MultiPartForm
import ai.cog.dao.{DatasetColumnDAO, DatasetColumnMySqlDAO, DatasetDAO, DatasetMySqlDAO}
import ai.cog.models._
import ai.cog.utils.{AWSHelper, IOHelper}
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.{HttpResponse, Multipart, StatusCodes}

import scala.concurrent.Future
import scala.util.{Failure, Success}
/**
  * Created by andyyoo on 11/05/16.
  */
trait DatasetService {

  this: DatasetDAO with DatasetColumnDAO =>
  import scala.concurrent.ExecutionContext.Implicits.global

  /**
    *
    * @return
    */
  def getRandomFilename(): String = {
    s"${UUID.randomUUID().toString()}.txt"
  }

  /**
    *
    * @param fileName
    * @param fileData
    * @param localCopy
    * @param system
    * @return
    */
  def routeWithActor(fileName: String, fileData: Multipart.FormData, localCopy: Option[Boolean])
                    (implicit system: ActorSystem): HttpResponse = {
    val fileUploader = system.actorOf(Props[S3FileUploader])
    fileUploader ! MultiPartForm(fileName, fileData, localCopy)
    HttpResponse(StatusCodes.OK, entity = s"File is now being processed. Please wait and comeback later.")
  }

  /**
    *
    * @param request
    * @return
    */
  def save(request: DatasetRequest) : Future[DatasetResult]= {
    for {
      created <- createDataset(request.dataset)
      createdColumns <- createDatasetColumns(created.get.id, request.columns)
    } yield DatasetResult(created.get.id, s"Dataset ${created.get.id} was created")
  }

  /**
    *
    * @param request
    * @param system
    * @return
    */
  def saveAndUpload(request: DatasetUploadRequest)(implicit system: ActorSystem): Future[DatasetResult] = {
    var totalFileSize = 0
    val baFuture: Future[Array[Tuple2[String, Future[Array[Byte]]]]] = IOHelper.buildByteArrayAsync(request.fileData)

    import ai.cog.serializers.JsonSupport._

    for {
      array <- baFuture
      // 1. Reorganise the multi parts to Futures of each field including 'file'
      fileF = array.filter(_._1 == "file").map(_._2).head
      metaF = array.filter(_._1 == "meta").map(_._2).headOption.getOrElse(throw new Exception("meta should be included"))
      // if not provided, columns still can be inferred from Spark Dataframe
      columnsF = array.filter(_._1 == "columns").map(_._2).headOption.getOrElse(Future(Array[Byte]()))
      //2. uploads the 'file' to s3 and make a local copy if necessary
      byteArray <- fileF
      uploaded <- uploadToS3(request.filename, byteArray, request.localCopy)
      //3. builds Dataset model from byte array and insert it to db
      metaByteArray <- metaF
      dataset <- byteArrayToJsonToModel[Dataset](metaByteArray)
      createdDataset <- createDataset(dataset)
      //4. builds a list of DatasetColumn model and bulk inserts them to db
      // if columns are not provided then it will throw an exception which will be converted to empty list to allow it
      colsByteArray <- columnsF
      colsList <- byteArrayToJsonToModel[List[DatasetColumn]](colsByteArray, Some("columns")) recover{ case _ => List[DatasetColumn]()}
      createdDatasetColumns <- createDatasetColumns(createdDataset.get.id, colsList)
    } yield {
      DatasetResult(createdDataset.get.id, s"${byteArray.length} byte size file has been uploaded successfully")
    }
  }

  /**
    *
    * @param id
    * @param request
    * @return
    */
  def update(id: Long, request: DatasetRequest) : Future[DatasetResult] = {
    val updated = if(request.columns.isEmpty) {
        updateDatasetWithColumns(id, request.dataset, request.columns)
      } else {
        updateDataset(id, request.dataset)
      }
    updated.map { result => DatasetResult(Some(id), s"Dataset ${id} was updated") }
  }

  /**
    *
    * @param filename
    * @param byteArray
    * @param localCopy
    * @return
    */
  private def uploadToS3(filename: String, byteArray: Array[Byte], localCopy: Option[Boolean]): Future[Unit] = {
    Future {
      AWSHelper.S3.put(filename, byteArray) match {
        case Success(_) =>
          if(localCopy.getOrElse(false))
            new FileOutputStream(filename).write(byteArray)
        case Failure(ex) => throw ex
      }
      println(">> Succssefully uploaded to aws-s3 and now saving dataset info to database")
    }
  }
}

/**
  * Default DatasetService Implementation using MySqlDAO
  */
class DatasetServiceMySqlImpl extends DatasetService with DatasetMySqlDAO with DatasetColumnMySqlDAO


/*
    ugliy Future callbacks but has been replaced by for-comprehension.

    baFuture.flatMap { array =>
      val fileF = array.filter(_._1 == "file").map(_._2).headOption.get // null file is ok since it might already have been uploaded to s3
      val metaF = array.filter(_._1 == "meta").map(_._2).headOption.getOrElse(throw new Exception("meta should be included"))
      val columnsF = array.filter(_._1 == "columns").map(_._2).headOption.getOrElse(throw new Exception("columns should be included"))
      fileF.map {
        case byteArray: Array[Byte] => {
          uploadToS3(filename, byteArray, localCopy).map {
            case _ => println(">> Succssefully uploaded to aws-s3 and now saving dataset info to database")
              metaF.map { metaByteArray =>
                val dataset = byteArrayToJsonToModel[Dataset](metaByteArray).get
                createDataset(dataset).onComplete {
                  case Success(created) =>
                    columnsF.map { colsByteArray =>
                      val colsJson = new String(colsByteArray, "UTF-8").parseJson
                      val parseJson = colsJson.asJsObject().getFields("columns")(0).toString
                      val colsList = jsonStringToModel[List[DatasetColumn]](parseJson)
                      println(colsList)
                    }

                  case Failure(ex) => println(ex)
                }
              }
          }
          totalFileSize += byteArray.length
          totalFileSize
        }
      }
    }
    */