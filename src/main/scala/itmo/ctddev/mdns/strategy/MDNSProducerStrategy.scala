package itmo.ctddev.mdns.strategy

import akka.actor.ActorRef
import akka.io.Tcp.Write
import akka.util.ByteString

/**
  * Created by itegulov.
  */
case class MDNSProducerStrategy(produceFunction: () => String) extends MDNSNodeStrategy {
  private val produceMessage = """produce""".r

  override def accept(message: String, sender: ActorRef): Unit = message match {
    case produceMessage() =>
      sender ! Write(ByteString("producer " + produceFunction()))
    case _ =>
      println(s"Malformed tcp message: $message.")
  }
}
