package ai.cog

import ai.cog.dao.{DatasetColumnDAO, DatasetDAO, JobDAO}
import ai.cog.models.{Dataset, DatasetColumn, Job}
import ai.cog.services.{DatasetService, MachineLearningService}
import ai.cog.utils.{ActorSystemModule, Config, DBConfig}
import akka.actor.ActorSystem
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.{ImplicitSender, TestKit}
import org.joda.time.DateTime
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfter, Matchers, WordSpecLike}
import slick.driver.{JdbcProfile, MySQLDriver}

import scala.util.{Failure, Success}
/**
  * Created by andyyoo on 12/05/16.
  */

trait TestConfig extends Config {
  //Overrides configs for test here
}


trait BaseSpec extends WordSpecLike
  with TestConfig
  with Matchers
  with BeforeAndAfter
  with MockFactory
  with ScalaFutures {

  //Useful mockable traits from all places
  trait MachineLearningServiceTest extends MachineLearningService with JobDAO with DBConfig
  trait DatasetServiceTest extends DatasetService with DatasetDAO with DatasetColumnDAO with DBConfig
  case class TestDAO() extends BaseDaoSpec
}

trait BaseApiSepc extends BaseSpec with ScalatestRouteTest {
  override implicit val executor = system.dispatcher
}

trait BaseDaoSpec extends BaseSpec with ActorSystemModule {
  trait TestMysqlConfig extends DBConfig {
    import slick.driver.MySQLDriver.api._
    val driver:JdbcProfile = MySQLDriver
    val db = Database.forConfig("mysql_test")

    def reloadSchema(tableName: String): Unit = {
      db.run(sqlu"TRUNCATE TABLE #$tableName").onComplete {
        case Success(_) => println(s"Successfully truncated table, " + tableName)
        case Failure(ex) => throw ex
      }
    }
  }

  import scala.concurrent.Await
  import scala.concurrent.duration._

  /**
    *  DatasetDAO
    */
  class DatasetMySqlDaoTest extends DatasetDAO with TestMysqlConfig
  val datasetDao = new DatasetMySqlDaoTest

  def buildNewDataset(id: Option[Long]= None,
                      name: String="test dataset",
                      filename: String="test.csv",
                      fileType: String="csv",
                      s3Bucket: String="project.borg.data",
                      hasHeader: Boolean=true): Dataset = {
    Dataset(id, name, filename, fileType, s3Bucket, hasHeader)
  }

  def createNewDataset(newDataset: Dataset): Dataset = {
    Await.result(datasetDao.createDataset(newDataset), 1.seconds).getOrElse(throw new Exception("Something went wrong in creating a dataset"))
  }

  /**
    *  DatasetColumnDAO
    */
  class DatasetColumnDAOTest extends DatasetColumnDAO with TestMysqlConfig
  val datasetColumnDao = new DatasetColumnDAOTest

  def buildNewDatasetColumn(id: Option[Long] = None,
                            datasetId: Option[Long] = None,
                            name: String = "test column",
                            dataType: String = "StringType",
                            isCategorical: Option[Boolean] = Some(false),
                            imputationStrategy: Option[String] = Some("mean"),
                            sortOrder: Option[Int] = None) = {
    DatasetColumn(id, datasetId, name, dataType, isCategorical, imputationStrategy, sortOrder)
  }

  def createNewDatasetColumn(col: DatasetColumn): Option[Long] = {
    Await.result(datasetColumnDao.createDatasetColumn(col), 1.seconds)
  }
  def createNewDatasetColumns(datasetIdOpt: Option[Long], colList: List[DatasetColumn]) = {
    Await.result(datasetColumnDao.createDatasetColumns(datasetIdOpt, colList), 1.seconds)
  }

  /**
    * JobDAO
    */
  class JobDAOTest extends JobDAO with TestMysqlConfig
  val jobDao = new JobDAOTest

  def buildNewJob(id: Option[Long] = None,
                  name: String = "Test Job",
                  jobType: String = "Classification",
                  targetDataset: String = "test.csv",
                  targetColumn: String = "test_col1",
                  excludedColumns: Option[String] = None,
                  description: String = "ready",
                  scheduledAt: DateTime = new DateTime,
                  startedAt: Option[DateTime] = None,
                  endedAt: Option[DateTime] = None,
                  createdAt: Option[DateTime] = Some(new DateTime),
                  updatedAt: Option[DateTime] = Some(new DateTime),
                  deletedAt: Option[DateTime] = None): Job = {
    Job(id, name, jobType, targetDataset, targetColumn, excludedColumns, description, scheduledAt,
      startedAt, endedAt, createdAt, updatedAt, deletedAt)
  }

  def createNewJob(newJob: Job, datasetIds: Seq[Long]): Option[Long] = {
    Await.result(jobDao.createJob(newJob, datasetIds), 1.second)
  }
}

class BaseActorSpec extends TestKit(ActorSystem("test-system")) with ImplicitSender with BaseSpec
