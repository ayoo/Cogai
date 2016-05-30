package ai.cog.models.Tables

import ai.cog.models.{Dataset, DatasetColumn, Job, JobDataset}
import org.joda.time._
import slick.driver.MySQLDriver.api._
import slick.lifted.Tag
/**
  * Created by andyyoo on 18/05/16.
  */
/**
  * Database Table Definitions
  */
trait SlickTableDefinitions {
  val datasetQuery = TableQuery[DatasetTable]
  val datasetColumnQuery = TableQuery[DatasetColumnTable]
  val jobQuery = TableQuery[JobTable]
  val jobDatasetQuery = TableQuery[JobDatasetTable]

  import com.github.tototoshi.slick.MySQLJodaSupport._

  class DatasetTable(tag: Tag) extends Table[Dataset](tag, "datasets"){
    def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def filename = column[String]("filename")
    def fileType = column[String]("filetype")
    def s3Bucket = column[String]("s3_bucket")
    def hasHeader = column[Boolean]("has_header")
    def * = (id, name, filename, fileType, s3Bucket, hasHeader) <> (Dataset.tupled, Dataset.unapply)
  }
  class DatasetColumnTable(tag: Tag) extends Table[DatasetColumn](tag, "dataset_columns") {
    def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
    def datasetId = column[Option[Long]]("dataset_id")
    def name = column[String]("name")
    def dataType = column[String]("data_type")
    def isCategorical = column[Option[Boolean]]("is_categorical")
    def imputationStrategy = column[Option[String]]("imputation_strategy")
    def sortOrder = column[Option[Int]]("sort_order")
    def * = (id, datasetId, name, dataType, isCategorical,
      imputationStrategy, sortOrder) <> (DatasetColumn.tupled, DatasetColumn.unapply)
    def dataset = foreignKey("dataset_fk", datasetId, datasetQuery)(_.id.get)
  }
  class JobTable(tag: Tag) extends Table[Job](tag, "jobs") {
    def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def jobType = column[String]("job_type") // classification, regression, recommendation and clustering
    def targetDataset = column[String]("target_dataset")
    def targetColumn = column[String]("target_column")
    def excludedColumns = column[Option[String]]("excluded_columns")
    def description = column[String]("description")
    def scheduledAt = column[DateTime]("scheduled_at")
    def startedAt = column[Option[DateTime]]("started_at")
    def endedAt = column[Option[DateTime]]("ended_at")
    def createdAt = column[Option[DateTime]]("created_at")
    def updatedAt = column[Option[DateTime]]("updated_at")
    def deletedAt = column[Option[DateTime]]("deleted_at")
    def * = (id, name, jobType, targetDataset, targetColumn, excludedColumns, description,
      scheduledAt, startedAt, endedAt, createdAt, updatedAt, deletedAt) <> (Job.tupled, Job.unapply)
    def datasets = jobDatasetQuery.filter(_.jobId === id).flatMap(_.job)
  }
  class JobDatasetTable(tag: Tag) extends Table[JobDataset](tag, "jobs_datasets") {
    def jobId = column[Option[Long]]("job_id")
    def datasetId = column[Option[Long]]("dataset_id")
    def * = (jobId, datasetId) <> (JobDataset.tupled, JobDataset.unapply)
    def job = foreignKey("job_fk", jobId, jobQuery)(_.id.get)
    def dataset = foreignKey("dataset_fk", datasetId, datasetQuery)(_.id.get)
  }
}