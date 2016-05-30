package ai.cog.models

import akka.http.scaladsl.model.Multipart
import org.joda.time.DateTime

/**
  * Created by andyyoo on 13/05/16.
  */

trait Model

/**
  * Data Entity Models
  */

case class Dataset(id: Option[Long],
                   name: String,
                   filename: String,
                   fileType: String,
                   s3Bucket: String,
                   hasHeader: Boolean) extends Model

case class DatasetColumn(id: Option[Long],
                         datasetId: Option[Long],
                         name: String,
                         dataType: String,
                         isCategorical: Option[Boolean],
                         imputationStrategy: Option[String]=Some("mean"),
                         sortOrder: Option[Int]) extends Model


case class Job(id: Option[Long],
               name: String,
               jobType: String,
               targetDataset: String,
               targetColumn: String,
               excludedColumns: Option[String]=None,
               description: String,
               scheduledAt: DateTime,
               startedAt: Option[DateTime] = None,
               endedAt: Option[DateTime] = None,
               createdAt: Option[DateTime] = Some(new DateTime),
               updatedAt: Option[DateTime] = Some(new DateTime),
               deletedAt: Option[DateTime] = None) extends Model

case class JobDataset(jobId: Option[Long], datasetId: Option[Long]) extends Model

/**
  * Http Entity Models
  */

trait Request
trait Result

case class DatasetRequest(dataset: Dataset, columns: Seq[DatasetColumn]) extends Request
case class DatasetUploadRequest(filename: String, fileData: Multipart.FormData, localCopy: Option[Boolean]) extends Model with Request
case class DatasetResult(id: Option[Long], message: String) extends Model with Result

case class PredictionResult(modelId: String, results: Seq[String]) extends Model with Result
case class PredictionRequest(modelId: String, instances: Seq[Seq[Double]]) extends Model with Request

case class JobRequest(job: Job, datasetIds: Seq[Long]) extends Model with Request
case class JobResult(id: Option[Long], message: String) extends Model with Result