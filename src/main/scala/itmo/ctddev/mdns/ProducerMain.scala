package itmo.ctddev.mdns

import java.net._

import akka.actor.{ActorSystem, Props}
import itmo.ctddev.mdns.core.MDNSNode
import itmo.ctddev.mdns.strategy.MDNSProducerStrategy

/**
  * Created by sugakandrey.
  */
object ProducerMain extends App {
  implicit val system = ActorSystem()

  system.actorOf(Props(MDNSNode(MDNSProducerStrategy(() => "something"), "sugok_producer", new InetSocketAddress(args(0), args(1).toInt))))
}
