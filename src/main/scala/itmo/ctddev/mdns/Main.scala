package itmo.ctddev.mdns

import java.net._

import akka.actor.{ActorSystem, Props}
import itmo.ctddev.mdns.core.MDNSNode

/**
  * Created by sugakandrey.
  */
object Main extends App {
  implicit val system = ActorSystem()

  system.actorOf(Props(MDNSNode("sugak", new InetSocketAddress(args(0), args(1).toInt))))
}
