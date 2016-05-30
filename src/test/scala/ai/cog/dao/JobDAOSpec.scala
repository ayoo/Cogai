package ai.cog.dao

import ai.cog.BaseDaoSpec
import org.joda.time.DateTime

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by andyyoo on 23/05/16.
  */
class JobDAOSpec extends BaseDaoSpec {

  before {
    jobDao.reloadSchema("jobs")
    jobDao.reloadSchema("jobs_datasets")
  }

  val dummyDatasetIds = Seq(1L, 2L)

  "JobDao" should {
    "create a new job with associated dataset ids" in {
      val job = buildNewJob(name="New Test Job")
      val result = Await.result(jobDao.createJob(job, dummyDatasetIds), 1.second).getOrElse(assert(false))
      result should be === 1
    }
    "find a job " in {
      val jobId = createNewJob(buildNewJob(name="New Test Job2"), dummyDatasetIds)
      val job = Await.result(jobDao.findJobById(jobId), 1.second).get
      job.name should be === "New Test Job2"
    }
    "find a job by id with datasets" in {
      val dataset1Id = createNewDataset(buildNewDataset(name="Dummy dataset-1")).id
      val dataset2Id = createNewDataset(buildNewDataset(name="Dummy dataset-2")).id
      val jobId = createNewJob(buildNewJob(name="New Test Job2"), Seq(dataset1Id.get, dataset2Id.get))
      val jobDatasetMap = Await.result(jobDao.findJobWithDatasetsById(jobId), 1.second)
      val datasets = jobDatasetMap.map {
        case (_, seq) => seq.filter(_.id == dataset1Id).head.name should be === "Dummy dataset-1"
        case (_) => assert(false)
      }
    }
    "find a next job with datasets and return the first job" in {
      val dataset1Id = createNewDataset(buildNewDataset(name="Dummy dataset-1")).id
      val dataset2Id = createNewDataset(buildNewDataset(name="Dummy dataset-2")).id
      val jobId1 = createNewJob(buildNewJob(name="New Test Job1"), Seq(dataset1Id.get, dataset2Id.get))
      val jobId2 = createNewJob(buildNewJob(name="New Test Job2"), Seq(dataset1Id.get, dataset2Id.get))
      val jobDatasetMap = Await.result(jobDao.findNextJobWithDatasets, 1.second)
      val datasets = jobDatasetMap.map {
        case(job, _) => job.id should be === jobId1
        case(_) => assert(false)
      }
    }
    "find a next job with datasets and return the second job after the first job finished" in {
      val dataset1Id = createNewDataset(buildNewDataset(name="Dummy dataset-1")).id
      val dataset2Id = createNewDataset(buildNewDataset(name="Dummy dataset-2")).id
      val jobId1 = createNewJob(buildNewJob(name="New Test Job1", startedAt = Some(new DateTime), endedAt = Some(new DateTime)), Seq(dataset1Id.get, dataset2Id.get))
      val jobId2 = createNewJob(buildNewJob(name="New Test Job2"), Seq(dataset1Id.get, dataset2Id.get))
      val jobDatasetMap = Await.result(jobDao.findNextJobWithDatasets, 1.second)
      val datasets = jobDatasetMap.map {
        case(job, _) => job.id should be === jobId2
        case(_) => assert(false)
      }
    }
    "return no job when there is a running job at the moment " in {
      val dataset1Id = createNewDataset(buildNewDataset(name="Dummy dataset-1")).id
      val dataset2Id = createNewDataset(buildNewDataset(name="Dummy dataset-2")).id
      val jobId1 = createNewJob(buildNewJob(name="New Test Job1", startedAt = Some(new DateTime)), Seq(dataset1Id.get, dataset2Id.get))
      val jobId2 = createNewJob(buildNewJob(name="New Test Job2"), Seq(dataset1Id.get, dataset2Id.get))
      val jobDatasetMap = Await.result(jobDao.findNextJobWithDatasets, 1.second)
      jobDatasetMap.isEmpty should be === true
    }
    "update the job with id" in {
      val jobId = createNewJob(buildNewJob(name="New Test Job3"), dummyDatasetIds)
      val job = Await.result(jobDao.findJobById(jobId), 1.second).get
      val newJob = job.copy(name="Updated Test Job3", description="running")
      val updated = Await.result(jobDao.updateJob(job.id, newJob), 1.second)
      val found = Await.result(jobDao.findJobById(jobId), 1.second).get
      updated should be === 1
      found.description should be === "running"
    }
  }
}
