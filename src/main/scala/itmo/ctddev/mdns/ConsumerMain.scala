package itmo.ctddev.mdns

import akka.actor.{ActorSystem, Props}
import itmo.ctddev.mdns.ClientMain.args
import itmo.ctddev.mdns.core.MDNSNode
import itmo.ctddev.mdns.strategy.MDNSConsumerStrategy
import itmo.ctddev.mdns.utils.Utils

/**
  * Created by sugakandrey.
  */
object ConsumerMain extends App {
  implicit val system = ActorSystem()
  val networkInterface = Utils.getInterface
  val inetSocketAddress = Utils.getInetSocketAddress(args)
  private val name = System.getProperty("user.name") + "_consumer" + inetSocketAddress.getPort

  system.actorOf(
    Props(
      MDNSNode(
        networkInterface,
        MDNSConsumerStrategy(println),
        name,
        inetSocketAddress
      )
    )
  )
}
