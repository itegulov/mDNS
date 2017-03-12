package itmo.ctddev.mdns

import java.net._

import akka.actor.{ActorSystem, Props}
import itmo.ctddev.mdns.ClientMain.args
import itmo.ctddev.mdns.core.MDNSNode
import itmo.ctddev.mdns.strategy.MDNSExecutorStrategy
import itmo.ctddev.mdns.utils.Utils

/**
  * Created by sugakandrey.
  */
object ExecutorMain extends App {
  implicit val system = ActorSystem()
  val networkInterface = Utils.getInterface
  val inetSocketAddress = Utils.getInetSocketAddress(args)
  private val name = System.getProperty("user.name") + "_executor" + inetSocketAddress.getPort
  system.actorOf(
    Props(
      MDNSNode(
        networkInterface,
        MDNSExecutorStrategy(name),
        name,
        inetSocketAddress
      )
    ),
    "mainActor"
  )
}
