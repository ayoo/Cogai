package ai.cog.actors

import ai.cog.Main._
import ai.cog.actors.BatchTrainer.StartBatchTrain
import ai.cog.actors.Supervisor.{JobLoaded, SchedulerStatus}
import ai.cog.dao.{DatasetDAO, JobDAO}
import ai.cog.models.{Dataset, Job}
import akka.actor.{Actor, Props}
import org.apache.spark.SparkContext

/**
  * Created by andyyoo on 21/05/16.
  */
object Supervisor {
  def props(sparkContext: SparkContext, jobDAO: JobDAO, datasetDAO: DatasetDAO) = Props(new Supervisor(sparkContext, jobDAO))

  // message types
  case class SchedulerStatus(running: Boolean)
  case class JobLoaded(job: Job, datasets: Seq[Dataset])
}

class Supervisor(sparkContext: SparkContext, jobDAO: JobDAO) extends Actor {

  //Children actors
  val scheduler = system.actorOf(JobScheduler.props(self, machineLearningService), "Scheduler")
  val batchTrainer = system.actorOf(BatchTrainer.props(self, sparkContext, machineLearningService, datasetService))

  override def preStart() = println("Starting the supervisor...")

  override def receive: Receive = {

    case SchedulerStatus(running) =>
      println(s"Scheduler is ${if(running) "running" else "not running"}")

    case JobLoaded(job, datasets) =>
      println(s"Supervisor received ${job.name} with datasets=[${datasets.mkString(",")}]")
      batchTrainer ! StartBatchTrain(job, datasets)

    case _ =>
      println("Unknown message was received and this will throw an error in production")
  }
}