package ai.cog.serializers
import ai.cog.models._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, RootJsonFormat}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * Created by andyyoo on 13/05/16.
  */

trait CustomJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit object DateTimeFormat extends RootJsonFormat[DateTime] {
    val formatter = ISODateTimeFormat.basicDateTimeNoMillis
    def write(obj: DateTime): JsValue = {
      JsString(formatter.print(obj))
    }
    def read(json: JsValue): DateTime = json match {
      case JsString(s) => try {
        formatter.parseDateTime(s)
      }
      catch {
        case t: Throwable => error(s)
      }
      case _ =>
        error(json.toString())
    }
    def error(err: String): DateTime = {
      val example = formatter.print(0)
      throw new DeserializationException(err)
    }
  }
}

trait JsonSupport extends CustomJsonSupport {
  //dataset formats
  implicit val datasetFormat = jsonFormat6(Dataset)
  implicit val datasetColumnFormat = jsonFormat7(DatasetColumn)
  implicit val datasetColumnListFormat = listFormat(datasetColumnFormat)
  implicit val datasetRequestFormat = jsonFormat2(DatasetRequest)
  implicit val datasetResultFormat = jsonFormat2(DatasetResult)

  //prediction formats
  implicit val predictionFormat = jsonFormat2(PredictionResult)
  implicit val predictionRequestFormat = jsonFormat2(PredictionRequest)

  // job formats
  implicit val jobFormat = jsonFormat13(Job)
  implicit val jobRequestFormat = jsonFormat2(JobRequest)
  implicit val jobResultFormat = jsonFormat2(JobResult)

}

object JsonSupport extends JsonSupport {
  import spray.json._


  def byteArrayToJson = {
   // colsJson = new String(colsByteArray, "UTF-8").parseJson
   // parseJson = colsJson.asJsObject().getFields("columns")(0).toString
   // colsList <- jsonStringToModel[List[DatasetColumn]](parseJson)
  }

  /**
    * convert the provided byteArray to Json and then to the given type model.
    *
    * @param byteArray
    * @param jsonFormat
    * @tparam T
    * @return
    */
  def byteArrayToJsonToModel[T](byteArray: Array[Byte], rootElement: Option[String]=None)
                               (implicit jsonFormat: JsonFormat[T]) : Future[T] = {
    Try {
      var jsonSrc = new String(byteArray, "UTF-8").parseJson
      if(rootElement.isDefined) {
        val extractedJsonStr = jsonSrc.asJsObject().getFields("columns")(0).toString
        jsonSrc = extractedJsonStr.parseJson
      }
      jsonSrc.convertTo[T]
    } match {
      case Success(model) => Future.successful(model)
      case Failure(ex) => Future.failed(ex)
    }
  }

  /**
    *
    * @param jsonString
    * @param jsonFormat
    * @tparam T
    * @return
    */
  def jsonStringToModel[T](jsonString: String)(implicit jsonFormat: JsonFormat[T]) : Future[T] = {
    val jsonSrc =jsonString.parseJson
    Try(jsonSrc.convertTo[T]) match {
      case Success(model) => Future.successful(model)
      case Failure(ex) => Future.failed(ex)
    }
  }
}