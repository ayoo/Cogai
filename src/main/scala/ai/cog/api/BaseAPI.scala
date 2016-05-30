package ai.cog.api

import ai.cog.models.Result
import ai.cog.serializers.JsonSupport
import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Created by andyyoo on 13/05/16.
  */
trait BaseAPI extends Directives with JsonSupport {
  implicit val system: ActorSystem
}