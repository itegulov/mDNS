package itmo.ctddev.mdns.strategy

import akka.actor.ActorRef

/**
  * Created by itegulov.
  */
object MDNSClientStrategy extends MDNSNodeStrategy {
  override def accept(message: String, sender: ActorRef): Unit = {
  }
}
