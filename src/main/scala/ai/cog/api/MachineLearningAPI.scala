package ai.cog.api

import ai.cog.models.{PredictionRequest, JobRequest}
import ai.cog.services.MachineLearningService
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route

import scala.util.{Failure, Success}
/**
  * Created by andyyoo on 5/05/16.
  */
trait MachineLearningAPI extends BaseAPI {
  val machineLearningService: MachineLearningService

  def machineLearningRoutes: Route =
    path("jobs") {
      (post & entity(as[JobRequest])) { request =>
        val result = machineLearningService.setupNewJob(request)
        onComplete(result) {
          case Success(job) => complete(OK, job)
          case Failure(ex) => complete(InternalServerError, "Something went wrong")
        }

      }
    } ~
    path("predictions") {
      (post & entity(as[PredictionRequest])){ request =>
        val result = machineLearningService.predict(request)
        onComplete(result) {
          case Success(prediction) => complete(OK, prediction)
          case Failure(ex) => complete(InternalServerError, "Something went wrong.")
        }
      }
    }
}