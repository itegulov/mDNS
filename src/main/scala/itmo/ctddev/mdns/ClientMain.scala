package itmo.ctddev.mdns

import java.net._

import akka.actor.{ActorSystem, Props}
import akka.event.Logging.LogLevel
import akka.pattern.ask
import akka.util.Timeout
import itmo.ctddev.mdns.ExecutorMain.args
import itmo.ctddev.mdns.core._
import itmo.ctddev.mdns.strategy.MDNSClientStrategy
import itmo.ctddev.mdns.utils.Utils

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn
import scala.language.postfixOps

/**
  * Created by itegulov.
  */
object ClientMain extends App {
  val help = "help".r
  val ls = "ls".r
  val sendConsumer = """send\s+consumer\s+([a-zA-ZА-Яа-я0-9_]+)\s+(.*)""".r
  val sendProducer = """send\s+producer\s+([a-zA-ZА-Яа-я0-9_]+)""".r
  val sendExecutor = """send\s+executor\s+([a-zA-ZА-Яа-я0-9_]+)\s+(.*)""".r
  val networkInterface = Utils.getInterface
  implicit val system = ActorSystem()
  system.eventStream.setLogLevel(LogLevel(0))

  val inetSocketAddress = Utils.getInetSocketAddress(args)
  private val name = System.getProperty("user.name") + "_client" + inetSocketAddress.getPort

  val clientActor = system.actorOf(
    Props(
      MDNSNode(
        networkInterface,
        MDNSClientStrategy,
        name,
        inetSocketAddress
      )
    )
  )
  implicit val timeout = Timeout(20 seconds)
  while (true) {
    print("> ")
    val command = StdIn.readLine()
    command match {
      case help() =>
        println(
          """
            | -- help                          print this help
            | -- ls                            print all alive nodes' names and free threads
            | -- send consumer <name> <text>   send <text> to consumer with name <name>
            | -- send producer <name>          request text from producer with name <name>
            | -- send executor <name> <code>   send <code> to executor with name <name> for execution
          """.stripMargin)
      case ls() =>
        val future = clientActor ? ListPeers
        val Peers(mdnsCache, mdnsFree) = Await.result(future, timeout.duration).asInstanceOf[Peers]
        for ((name, _) <- mdnsCache) {
          mdnsFree.get(name) match {
            case Some(free) =>
              println(s"  $name with $free free thread")
            case None =>
              println(s"  $name")
          }
        }
      case sendConsumer(name, body) =>
        val future = clientActor ? SendConsumer(name, body)
        val answer = Await.result(future, timeout.duration)
        answer match {
          case ConsumerAck =>
            println(s"Consumer $name has acknowledged consuming")
          case other =>
            println(s"ERROR: got $other")
        }
      case sendProducer(name) =>
        val future = clientActor ? SendProducer(name)
        val answer = Await.result(future, timeout.duration)
        answer match {
          case ProducerAnswer(body) =>
            println(s"Producer $name produced $body")
          case other =>
            println(s"ERROR: got $other")
        }
      case sendExecutor(name, code) =>
        val future = clientActor ? SendExecutor(name, code)
        val answer = Await.result(future, timeout.duration)
        answer match {
          case ExecutorResult(result) =>
            println(s"Executor $name produced $result")
          case other =>
            println(s"ERROR: got $other")
        }
      case _ =>
    }
  }
}
