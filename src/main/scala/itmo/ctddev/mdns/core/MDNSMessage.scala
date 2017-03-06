package itmo.ctddev.mdns.core

import java.net.InetSocketAddress

/**
  * Created by sugakandrey.
  */
sealed trait MDNSMessage

case class NewPeerAlive(name: String, addr: InetSocketAddress) extends MDNSMessage {
  override def toString: String = s"hey $name ${addr.getAddress.getHostAddress}:${addr.getPort}"
}

case class PeerDied(name: String) extends MDNSMessage

case object ListPeers extends MDNSMessage

case class Peers(
  mdnsCache: Map[String, InetSocketAddress],
  mdnsFree: Map[String, Int]
) extends MDNSMessage

case class SendConsumer(name: String, body: String) extends MDNSMessage

case object ConsumerAck extends MDNSMessage

case class SendProducer(name: String) extends MDNSMessage

case class ProducerAnswer(body: String) extends MDNSMessage

case class SendExecutor(name: String, code: String) extends MDNSMessage

case class ExecutorResult(result: String) extends MDNSMessage
