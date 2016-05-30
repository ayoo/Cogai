package ai.cog

import ai.cog.actors.Supervisor
import ai.cog.services.{DatasetServiceMySqlImpl, MachineLearningServiceMySqlImpl}
import ai.cog.api.{DatasetAPI, MachineLearningAPI, StatusAPI}
import ai.cog.utils.{ActorSystemModule, Config, SparkContextModule}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

/**
  * Created by andyyoo on 30/04/16.
  */
object Main extends App
  with Config
  with ActorSystemModule
  with SparkContextModule
  with StatusAPI
  with DatasetAPI
  with MachineLearningAPI {

  //Register all enabled services here
  override val datasetService = new DatasetServiceMySqlImpl
  override val machineLearningService = new MachineLearningServiceMySqlImpl

  //Register all enabled services here
  val routes: Route = pathPrefix("v1") {
      statusRoutes ~
      datasetRoutes ~
      machineLearningRoutes
  } ~ path("") {
    getFromResource("public/index.html")
  }

  //Register the supervising actors here
  val supervisor = system.actorOf(
    Supervisor.props(
      sparkContext,
      machineLearningService,
      datasetService), "Supervisor")

  Http().bindAndHandle(routes, apiHost, apiPort)
}

