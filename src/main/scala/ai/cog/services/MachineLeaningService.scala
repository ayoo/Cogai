package ai.cog.services

import ai.cog.dao.{JobDAO, JobMySqlDAO}
import ai.cog.models.{JobRequest, JobResult, PredictionRequest, PredictionResult}

import scala.concurrent.Future

/**
  * Created by andyyoo on 13/05/16.
  */
trait MachineLearningService {
  this: JobDAO =>
  import scala.concurrent.ExecutionContext.Implicits.global

  /**
    * Setup a new batch job. The job will be queued into jobs table which will be periodically scanned by
    * Scheduler Actor
 *
    * @param request
    * @return
    */
  def setupNewJob(request: JobRequest): Future[JobResult] = {
    createJob(request.job, request.datasetIds).map { jobId => JobResult(jobId, "A new job was created successfully") }
  }

  /**
    *
    * @param request
    * @return
    */
  def predict(request: PredictionRequest): Future[PredictionResult] = {
    Future { PredictionResult("dummyModel", Seq("A", "B", "C", "A", "A","C")) }
  }
}

class MachineLearningServiceMySqlImpl extends MachineLearningService with JobMySqlDAO
