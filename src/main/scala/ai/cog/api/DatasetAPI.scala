package ai.cog.api

import ai.cog.models.{DatasetRequest, DatasetUploadRequest}
import ai.cog.services.DatasetService
import akka.http.scaladsl.model.Multipart
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route

import scala.util.{Failure, Success}

/**
  * Created by andyyoo on 4/05/16.
  */

/**
  * Experimental API
  */
trait DatasetAPI extends BaseAPI {
  val datasetService: DatasetService

  def datasetRoutes: Route =
    path("datasets") {
      (post & entity(as[DatasetRequest])) { request =>
        val resultF = datasetService.save(request)
        onComplete(resultF) {
          case Success(result) => complete(OK, result)
          case Failure(ex) => complete(InternalServerError, "Something went wrong")
        }
      }
    } ~
    path("datasets" / IntNumber) { id =>
      (put & entity(as[DatasetRequest])) { request =>
        val resultF = datasetService.update(id, request)
        onComplete(resultF) {
          case Success(result) => complete(OK, result)
          case Failure(ex) => complete(InternalServerError, "Something went wrong")
        }
      }
    } ~
    path("datasets" / "s3") {
      (post & entity(as[Multipart.FormData])) { multipartFormData =>
        parameters('local_copy.as[Boolean].?) { (localCopy) =>
          val fileName = datasetService.getRandomFilename()
          val result = datasetService.saveAndUpload(DatasetUploadRequest(fileName, multipartFormData, localCopy))
          onComplete(result) {
            case Success(dataset) =>
              complete(OK, dataset)
            case Failure(ex) =>
              println(ex)
              complete(InternalServerError, s"Something went wrong.")
          }
        }
      }
    }
}
