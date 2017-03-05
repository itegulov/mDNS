package itmo.ctddev.mdns

import java.net._

import akka.actor.{ActorSystem, Props}
import itmo.ctddev.mdns.core.MDNSNode
import itmo.ctddev.mdns.strategy.MDNSConsumerStrategy

/**
  * Created by sugakandrey.
  */
object ConsumerMain extends App {
  implicit val system = ActorSystem()

  system.actorOf(Props(MDNSNode(MDNSConsumerStrategy(_ => ()), "sugok_consumer", new InetSocketAddress(args(0), args(1).toInt))))
}
