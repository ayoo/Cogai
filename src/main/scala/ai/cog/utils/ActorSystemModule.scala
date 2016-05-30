package ai.cog.utils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

/**
  * Created by andyyoo on 11/05/16.
  */
trait ActorSystemModule {
  implicit val system = ActorSystem("CogaiPrototype")
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()
}
