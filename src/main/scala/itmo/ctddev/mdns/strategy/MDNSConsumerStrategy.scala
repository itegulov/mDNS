package itmo.ctddev.mdns.strategy

import akka.actor.ActorRef
import akka.io.Tcp.Write
import akka.util.ByteString
import itmo.ctddev.mdns.core.ConsumerAck

/**
  * Created by itegulov.
  */
case class MDNSConsumerStrategy(consumeFunction: String => Unit) extends MDNSNodeStrategy {
  private val consumeMessage = """consume\s+(.*)""".r

  override def accept(message: String, sender: ActorRef): Unit = message match {
    case consumeMessage(body) =>
      consumeFunction(body)
      sender ! Write(ByteString("consumer ack"))
    case _ =>
      println(s"Malformed tcp message: $message.")
  }
}
