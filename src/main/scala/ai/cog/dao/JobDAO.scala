package ai.cog.dao

import ai.cog.models.Tables.SlickTableDefinitions
import ai.cog.models.{Dataset, Job, JobDataset}
import ai.cog.utils.{DBConfig, MySqlConfig}

import scala.concurrent.Future
/**
  * Created by andyyoo on 22/05/16.
  */
trait JobDAO extends SlickTableDefinitions {
  this: DBConfig =>

  import driver.api._
  import scala.concurrent.ExecutionContext.Implicits.global

  /**
    *
    * @param id
    * @return
    */
  def findJobById(id: Option[Long]) : Future[Option[Job]] = {
    db.run(jobQuery.filter(_.id === id).result.headOption)
  }

  /**
    *
    * @param id
    * @return
    */
  def findJobWithDatasetsById(id: Option[Long]): Future[Map[Job, Seq[Dataset]]] = {
    val jobDatasetQ = jobDatasetQuery joinLeft datasetQuery on (_.datasetId === _.id)
    val jobQ = jobQuery filter (_.id === id) joinLeft jobDatasetQ on (_.id === _._1.jobId)
    db.run(jobQ.result).map { seq =>
      seq.groupBy(_._1).map {
        case (job, seq) =>
          (job, seq.flatMap {
            case (_, Some(a)) => a._2
            case _ => None
          })
      }
    }
  }

  /**
    *
    * @return
    */
  def findNextJobWithDatasets: Future[Map[Job, Seq[Dataset]]] = {
    isAnyJobRunning.flatMap {
      case true =>
        Future(Map[Job, Seq[Dataset]]())
      case false =>
        val jobQ = jobQuery filter(_.startedAt.isEmpty) filter(_.endedAt.isEmpty) sortBy(_.id)
        db.run(jobQ.result).map(_.headOption).flatMap { headOpt =>
          if(headOpt.isEmpty) Future(Map[Job, Seq[Dataset]]()) // returns empty map when there aren't anything
          else findJobWithDatasetsById(headOpt.get.id)
        }
    }
  }

  /**
    *
    * @return
    */
  def isAnyJobRunning: Future[Boolean] = {
    val q = jobQuery filter(! _.startedAt.isEmpty) filter(_.endedAt.isEmpty)
    db.run(q.result).map(! _.isEmpty)
  }

  /**
    *
    * @param job
    * @param datasetIds
    * @return
    */
  def createJob(job: Job, datasetIds: Seq[Long]) : Future[Option[Long]] = {
    var createdJobId: Option[Long] = None
    val jobAction: DBIO[Option[Long]] = jobQuery returning jobQuery.map(_.id) += job
    val jobDatasetAction = jobAction.flatMap { jobId =>
      createdJobId = jobId
      val jobDatasets = for (datasetId <- datasetIds) yield {
        JobDataset(jobId, Option(datasetId))
      }
      jobDatasetQuery ++= jobDatasets
    }
    db.run(DBIO.seq(jobDatasetAction).transactionally).map(_ => createdJobId)
  }

  /**
    *
    * @param id
    * @param job
    * @return
    */
  def updateJob(id: Option[Long], job: Job) : Future[Int] = {
    db.run(jobQuery.filter(_.id === id).update(job))
  }
}

trait JobMySqlDAO extends JobDAO with MySqlConfig
class JobMysqlDAOImpl extends JobMySqlDAO

