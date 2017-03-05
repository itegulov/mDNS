package itmo.ctddev.mdns

import java.net._

import akka.actor.{ActorSystem, Props}
import itmo.ctddev.mdns.core.MDNSNode
import itmo.ctddev.mdns.strategy.MDNSExecutorStrategy

/**
  * Created by sugakandrey.
  */
object ExecutorMain extends App {
  implicit val system = ActorSystem()

  system.actorOf(Props(MDNSNode(MDNSExecutorStrategy("sugok_executor"), "sugok_executor", new InetSocketAddress(args(0), args(1).toInt))))
}
