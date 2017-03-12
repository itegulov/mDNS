package itmo.ctddev.mdns

import akka.actor.{ActorSystem, Props}
import itmo.ctddev.mdns.core.MDNSNode
import itmo.ctddev.mdns.strategy.MDNSProducerStrategy
import itmo.ctddev.mdns.utils.Utils

/**
  * Created by sugakandrey.
  */
object ProducerMain extends App {
  implicit val system = ActorSystem()
  val networkInterface = Utils.getInterface
  val inetSocketAddress = Utils.getInetSocketAddress(args)
  private val name = System.getProperty("user.name") + "_producer" + inetSocketAddress.getPort
  var times = 0
  def produceFunction(): String = {
    times += 1
    println("hhehe")
//    Thread.sleep(3000)
    "something" + times
  }


  system.actorOf(
    Props(
      MDNSNode(
        networkInterface,
        MDNSProducerStrategy(produceFunction),
        name,
        inetSocketAddress
      )
    )
  )
}
