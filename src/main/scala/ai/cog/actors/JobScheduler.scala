package ai.cog.actors

import ai.cog.actors.JobScheduler._
import ai.cog.actors.Supervisor.{JobLoaded, SchedulerStatus}
import ai.cog.dao.JobDAO
import akka.actor.{Actor, ActorRef, Props}
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}
/**
  * Created by andyyoo on 24/05/16.
  */
object JobScheduler {
  def props(supervisor: ActorRef, jobDao: JobDAO) = Props(new JobScheduler(supervisor, jobDao))

  // message types
  case object StatusCheck
  case object TickNext
}

class JobScheduler(supervisor: ActorRef, jobDAO: JobDAO) extends Actor{

  val tick = context.system.scheduler.schedule(500.millis, 15.seconds, self, TickNext)

  override def postStop() = tick.cancel()

  override def receive: Receive = {

    case StatusCheck =>
       supervisor ! SchedulerStatus(tick.isCancelled)

    case TickNext =>
      println(s"Received the next tick ")
      jobDAO.findNextJobWithDatasets.onComplete {
        case Success(mapSeq) =>
          if(! mapSeq.isEmpty) {
            val job = mapSeq.head._1
            val datasets = mapSeq.head._2
            jobDAO.updateJob(job.id, job.copy(startedAt = Some(new DateTime))) map { result =>
              supervisor ! JobLoaded(job, datasets)
            }
          }
        case Failure(ex) =>
          println(ex)
          //notify system admin
          context.stop(self)
      }
  }
}
