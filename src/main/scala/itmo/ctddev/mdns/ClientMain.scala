package itmo.ctddev.mdns

import java.net._

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import itmo.ctddev.mdns.core._
import itmo.ctddev.mdns.strategy.MDNSClientStrategy

import scala.concurrent.Await
import scala.language.postfixOps

/**
  * Created by itegulov.
  */
object ClientMain extends App {
  implicit val system = ActorSystem()

  val clientActor = system.actorOf(Props(MDNSNode(MDNSClientStrategy, "sugok_client", new InetSocketAddress(args(0), args(1).toInt))))

  implicit val timeout = Timeout(5 seconds)
  Thread.sleep(10000)
  val future = clientActor ? SendProducer("sugok_producer")
  val answer = Await.result(future, timeout.duration)
  println(answer)
}
