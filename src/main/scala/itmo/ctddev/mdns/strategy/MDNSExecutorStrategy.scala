package itmo.ctddev.mdns.strategy

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.io.{IO, Udp}
import akka.io.Tcp.Write
import akka.util.{ByteString, Timeout}
import akka.pattern.ask

import scala.concurrent.duration._

import scala.collection.mutable
import com.twitter.util.Eval
import itmo.ctddev.mdns.core._

import scala.concurrent.Await
import scala.language.postfixOps
import scala.util.Random

/**
  * Created by itegulov.
  */
case class MDNSExecutorStrategy(
  name: String,
  n: Int = 10,
  group: String = "224.0.0.251",
  port: Int = 5353
)(implicit system: ActorSystem) extends MDNSNodeStrategy {
  private val executeMessage = """execute\s+(.*)""".r
  private val schedulerActor = system.actorOf(Props(SchedulerActor()))

  private case class Task(code: String, submitter: ActorRef)
  private case class Finished(executor: ActorRef)

  case class ExecutorActor() extends Actor with ActorLogging {
    override def receive: Receive = {
      case Task(code, submitter) =>
        log.error(code)
        val result = Eval.apply(code).toString
        log.error(result)
        log.info(s"Executed task and got $result.")
        submitter ! Write(ByteString("executed " + result))
        sender ! Finished(self)
    }
  }

  case class SchedulerActor() extends Actor with ActorLogging {
    private val executors = mutable.Queue.empty[ActorRef]
    private val udpManager = IO(Udp)
    import context.dispatcher

    udpManager ! Udp.SimpleSender

    for (_ <- 1 to n) {
      executors += context.actorOf(Props(ExecutorActor()))
    }

    private case object Tick

    private val timeout = context.system.scheduler.schedule(1 seconds, 5 seconds, self, Tick)

    private implicit val t = Timeout(5 seconds)

    override def receive: Receive = {
      case Udp.SimpleSenderReady =>
        log.info(s"UdpMulticastSender initiated.")
        sender ! Udp.Send(ByteString(s"free $name ${executors.length}"), new InetSocketAddress(group, port))
        context.become(ready(sender()))
        log.info(s"Sending UDP-multicast message with free info.")
    }

    def ready(udpSend: ActorRef): Receive = {
      case task: Task =>
        if (Random.nextInt(10) == 0) {
          executors.headOption match {
            case Some(executor) =>
              log.info("Submitting task to executor.")
              executor ! task
              executors.dequeue()
              udpSend ! Udp.Send(ByteString(s"free $name ${executors.length}"), new InetSocketAddress(group, port))
              log.info(s"Sending UDP-multicast message with free info.")
            case None =>
              log.info("No available executors.")
              task.submitter ! Write(ByteString("not executed"))
          }
        } else {
          val future = context.actorSelection("/user/mainActor") ? ListPeers
          val Peers(_, mdnsFree) = Await.result(future, Duration.Inf)
          mdnsFree.find { case (otherName, free) => otherName != name && free != 0 } match {
            case Some((otherName, _)) =>
              log.info(s"Couldn't execute code. Redirecting to $otherName")
              context.actorSelection("/user/mainActor") ! SendRedirectExecutor(otherName, task.submitter, task.code)
            case None =>
              task.submitter ! Write(ByteString("not executed"))
          }
        }
      case Finished(executor) =>
        executors.enqueue(executor)
        udpSend ! Udp.Send(ByteString(s"free $name ${executors.length}"), new InetSocketAddress(group, port))
        log.info(s"Sending UDP-multicast message with free info.")
      case Tick =>
        udpSend ! Udp.Send(ByteString(s"free $name ${executors.length}"), new InetSocketAddress(group, port))
        log.info("Sending UDP-multicast message with free info on timeout.")
      case RedirectExecutorResult(result, replySender) =>
        replySender ! Write(ByteString("executed " + result))
    }
  }

  override def accept(message: String, sender: ActorRef): Unit = message match {
    case executeMessage(code) =>
      schedulerActor ! Task(code, sender)
    case _ =>
      println(s"Malformed tcp message: $message.")
  }
}
