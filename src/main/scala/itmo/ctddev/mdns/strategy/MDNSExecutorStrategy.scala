package itmo.ctddev.mdns.strategy

import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, ActorSystem, Props}
import akka.io.Tcp.Write
import akka.util.ByteString

import scala.collection.mutable

/**
  * Created by itegulov.
  */
case class MDNSExecutorStrategy(n: Int = 10)(implicit system: ActorSystem) extends MDNSNodeStrategy {
  private val executeMessage = """execute\s+(.*)""".r
  private val schedulerActor = system.actorOf(Props(SchedulerActor()))

  private case class Task(code: String, submitter: ActorRef)
  private case class Finished(executor: ActorRef)

  case class ExecutorActor() extends Actor with ActorLogging {
    override def receive: Receive = {
      case Task(code, submitter) =>
        val result = code.split(" ").map(_.toInt).sum
        log.info(s"Executed task and got $result.")
        submitter ! Write(ByteString("executed " + result))
        sender ! Finished(self)
    }
  }

  case class SchedulerActor() extends Actor with ActorLogging {
    private val executors = mutable.Queue.empty[ActorRef]

    for (_ <- 1 to n) {
      executors += context.actorOf(Props(ExecutorActor()))
    }

    override def receive: Receive = {
      case task: Task =>
        executors.headOption match {
          case Some(executor) =>
            log.info("Submitting task to executor.")
            executor ! task
            executors.dequeue()
          case None =>
            log.info("No available executors.")
            task.submitter ! Write(ByteString("not executed"))
        }
      case Finished(executor) =>
        executors.enqueue(executor)
    }
  }

  override def accept(message: String, sender: ActorRef): Unit = message match {
    case executeMessage(code) =>
      schedulerActor ! Task(code, sender)
    case _ =>
      println(s"Malformed tcp message: $message.")
  }
}