package ai.cog.api
import ai.cog.BaseApiSepc
import ai.cog.models.{JobRequest, JobResult, PredictionRequest, PredictionResult}
import ai.cog.services.MachineLearningService

import scala.concurrent.Future

/**
  * Created by andyyoo on 13/05/16.
  */
class MachineLearningAPISpec extends BaseApiSepc with MachineLearningAPI {
  //Stubbing the MachineLearningService mixingin all the prerequisites
  override val machineLearningService: MachineLearningService = stub[MachineLearningServiceTest]

  "PredictionAPI" should {
    "have POST /jobs and return Job id in json format" in {
      val datasetIds = Seq(1L)
      val newJob = TestDAO().buildNewJob(name="ML Job")
      val request = JobRequest(newJob, datasetIds)
      val result = JobResult(id=Some(1L), message = "a new job was created")
      (machineLearningService setupNewJob _).when(*).returns(Future(result)) // for some reason 'request' will cause the error here. scalamock bug?

      Post("/jobs", request) ~> machineLearningRoutes ~> check {
        status.isSuccess should be === true
        entityAs[JobResult].id === Some(1L)
      }
    }
    "have POST /predictions and return Prediction JSON" in {
      val modelId = "dummyId"
      val instances = Seq(Seq(0.2, 1.5, 1.0, 0.0),Seq(1.0, 2.2, 0.0, 3.5))
      val request = PredictionRequest("dummyId", instances)
      val result = PredictionResult("dummyId", Seq("A", "B"))
      (machineLearningService.predict _).when(request).returns(Future(result))

      Post("/predictions", request) ~> machineLearningRoutes ~> check {
        status.isSuccess should be === true
        entityAs[PredictionResult].modelId should be === modelId
      }
    }
  }
}
