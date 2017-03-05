package itmo.ctddev.mdns.strategy

import akka.actor.ActorRef

/**
  * Created by itegulov.
  */
trait MDNSNodeStrategy {
  def accept(message: String, sender: ActorRef): Unit
}
