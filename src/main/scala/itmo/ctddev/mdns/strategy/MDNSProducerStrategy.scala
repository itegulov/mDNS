package itmo.ctddev.mdns.strategy

import akka.actor.ActorRef

/**
  * Created by itegulov.
  */
case class MDNSProducerStrategy(produceFunction: () => String) extends MDNSNodeStrategy {
  private val produceMessage = """produce""".r

  override def accept(message: String, sender: ActorRef): Unit = message match {
    case produceMessage() =>
      sender ! produceFunction()
    case _ =>
      println(s"Malformed tcp message: $message.")
  }
}
