package ai.cog.actors

import ai.cog.BaseActorSpec
import ai.cog.actors.JobScheduler.StatusCheck
import ai.cog.actors.Supervisor.{JobLoaded, SchedulerStatus}
import ai.cog.models.{Dataset, Job}
import akka.testkit.TestProbe

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
/**
  * Created by andyyoo on 25/05/16.
  */
class JobSchedulerSpec extends BaseActorSpec {
  //val sparkContext = stub[SparkContext]
  val machineLearningService = stub[MachineLearningServiceTest]
  val datasetService = stub[DatasetServiceTest]
  val job = TestDAO().buildNewJob(id=Some(1L), name="Test Job")
  val datasets = Seq(TestDAO().buildNewDataset(id=Some(1L), name="Test Dataset"))

  "JobScheduler actor " should {
    "send a SchedulerStatus message to its supervisor on StatusCheck" in {
      val supervisor = TestProbe()
      val scheduler = system.actorOf(JobScheduler.props(supervisor.ref, machineLearningService))
      (machineLearningService.findNextJobWithDatasets _).when().returns(Future(Map[Job, Seq[Dataset]](job -> datasets )))
      (machineLearningService.updateJob _).when(*, *).returns(Future(1))
      supervisor.send(scheduler, StatusCheck)
      supervisor.expectMsg(SchedulerStatus(false)) // scheduler actually not started so ok
    }
    "send a JobLoaded message to its supervisor on TickNext" in {
      val supervisor = TestProbe()
      val scheduler = system.actorOf(JobScheduler.props(supervisor.ref, machineLearningService))
      (machineLearningService.findNextJobWithDatasets _).when().returns(Future(Map[Job, Seq[Dataset]](job -> datasets )))
      (machineLearningService.updateJob _).when(*, *).returns(Future(1))
      //supervisor.send(scheduler, TickNext) TickNext will be fired from scheduler's constructor
      supervisor.expectMsg(JobLoaded(job, datasets))
    }
  }

}
