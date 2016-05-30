package ai.cog.api

import java.io.File

import ai.cog.BaseApiSepc
import ai.cog.models.{DatasetColumn, DatasetRequest, DatasetResult, DatasetUploadRequest}
import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.stream.scaladsl.Source
import akka.util.ByteString

import scala.concurrent.Future

/**
  * Created by andyyoo on 9/05/16.
  */
class DatasetAPISpec extends BaseApiSepc with DatasetAPI {

  import spray.json._

  override val datasetService = stub[DatasetServiceTest] //new DefaultFileUploadServiceImpl()

  val file = new File("uploadable.txt")
  val fileEntity = HttpEntity(ContentTypes.`application/octet-stream`, 11, Source.single(ByteString("hello again")))
  val multipartForm = Multipart.FormData(
    Source(List(
      Multipart.FormData.BodyPart.Strict("val", HttpEntity("Just string value")),
      Multipart.FormData.BodyPart("file", fileEntity)
    )))

  "DatasetAPI" should {
    "Posting to /datasets should return 200 on success" in {
      val columns = Seq(TestDAO().buildNewDatasetColumn())
      val dataset = TestDAO().buildNewDataset(name="Test Dataset")
      val request = DatasetRequest(dataset, columns)
      val result = Future(DatasetResult(Some(1L), "Success"))

      print(s"Request JSON: ${request.toJson}")

      (datasetService.save _).when(*).returns(result)
      Post("/datasets", request) ~> datasetRoutes ~> check {
        status.isSuccess() shouldEqual true
      }
    }
    "Posting to /datasets/s3 should return 200 status code if everything is right" in {
      val filename:String = "mocked.txt"
      val localCopy = Option(true)
      val request = DatasetUploadRequest(filename, multipartForm, localCopy)
      val result = Future { DatasetResult(id=Some(1L), message="Successfully uploaded")}

      (datasetService.getRandomFilename _).when().returns(filename)
      // the generated multipart data is in different format, this won't compile for some reasons
      // so use wild match instead for now.
      (datasetService.saveAndUpload(_: DatasetUploadRequest)(_: ActorSystem)).when(*, *).returns(result)
      Post("/datasets/s3?local_copy=true", Marshal(multipartForm).to[RequestEntity]) ~> datasetRoutes ~> check {
        status.isSuccess() shouldEqual true
        entityAs[DatasetResult].id should be === Some(1L)
      }
    }
    "Posting to /datasets/s3 should return 500 status code if something went wrong" in {
      val filename:String = "mocked.txt"
      val localCopy = Option(true)
      val result = Future {throw new Exception("TEST-only: Some errors occurred")}
      (datasetService.getRandomFilename _).when().returns(filename)
      (datasetService.saveAndUpload(_: DatasetUploadRequest)(_: ActorSystem)).when(*, *).returns(result)
      Post("/datasets/s3?local_copy=true", Marshal(multipartForm).to[RequestEntity]) ~> datasetRoutes ~> check {
        status.isSuccess() shouldEqual false
      }
    }
    "Putting to /datasets should return 200 on success" in {
      val dataset = TestDAO().buildNewDataset(id=Some(1L), name="Updated Dataset")
      val request = DatasetRequest(dataset, Seq[DatasetColumn]())
      val result = Future(DatasetResult(dataset.id, "Updated OK"))
      (datasetService.update _).when(*, *).returns(result)
      Put("/datasets/1", request) ~> datasetRoutes ~> check {
        status.isSuccess() should be === true
      }

    }
  }
}
