package itmo.ctddev.mdns.core

import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

import scala.collection.{mutable => m}
import scala.concurrent.duration._
import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import akka.pattern.ask
import akka.io.Inet.SO.ReuseAddress
import akka.io.{IO, Tcp, Udp}
import akka.util.{ByteString, Timeout}

import scala.language.postfixOps

/**
  * Created by sugakandrey.
  */
final case class MDNSNode(
  name: String,
  addr: InetSocketAddress,
  group: String = "224.0.0.251",
  port: Int = 5353,
  interface: String = "en0"
) extends Actor with ActorLogging {

  import context.{system, dispatcher}

  private val opts = List(
    InetProtocolFamily,
    ReuseAddress(true),
    MulticastGroup(group, interface)
  )

  private val tcpManager = IO(Tcp)
  private val udpManager = IO(Udp)

  udpManager ! Udp.Bind(self, new InetSocketAddress(port), opts)
  udpManager ! Udp.SimpleSender
  tcpManager ! Tcp.Bind(self, addr)

  private val msg = NewPeerAlive(name, addr)
  private val mdnsCache = m.Map.empty[String, InetSocketAddress]
  private val protocol = """hey\s+([a-zA-Z]\w*)\s+(\d+\.\d+\.\d+\.\d+):(\d+)""".r

  override def receive: Receive = {
    case Tcp.Connected(remote, local) =>
      log.info(s"incoming tcp connection from $remote.")  
      sender ! Tcp.Register(self)
    case Udp.Bound(to) =>
      log.info(s"UDP bound node $name to $to.")
    case Udp.SimpleSenderReady =>
      log.info(s"UdpMulticastSender initiated.")
      val msg = NewPeerAlive(name, addr)
      sender ! Udp.Send(ByteString(msg.toString), new InetSocketAddress(group, port))
      log.info(s"Sending UDP-multicase message with self info.")
      sender ! PoisonPill
    case Udp.CommandFailed(_: Udp.Bind) =>
      log.error(s"Failed to bind to UDP multicast group.")
      context stop self
    case Tcp.Bound(localAddress) =>
      log.info(s"TCP bound node $name to $localAddress.")
    case Tcp.CommandFailed(_: Tcp.Bind) =>
      log.error(s"Failed to bind to TCP address $addr.")
      context stop self
    case Tcp.Received(data) =>
      val msg = data.decodeString(StandardCharsets.UTF_8)
      if (!msg.contains("googlecast")) log.info(s"Received tcp msg: $msg.")
      msg match {
        case protocol(nodeName, nodeIp, nodePort) =>
          log.info(s"Received info about node $nodeName. Updating cache.")
          registerNewPeer(nodeName, new InetSocketAddress(nodeIp, nodePort.toInt))
        case _  =>
          log.info(s"Malformed tcp message: $msg. Format is: hey *name* *ip*:*port*.")
      }
    case Udp.Received(data, _) =>
      val msg = data.decodeString(StandardCharsets.UTF_8)
      msg match {
        case protocol(nodeName, nodeIp, nodePort) =>
          log.info(s"Received info about node $nodeName {name: $nodeName, ip: $nodeIp, port: $nodePort}.")
          registerNewPeer(nodeName, new InetSocketAddress(nodeIp, nodePort.toInt))
        case _ =>
          log.info(s"Malformed multicast message: $msg. Format is: hey *name* *ip*:*port*.")
      }
    case PeerDied(nodeName) =>
      log.info(s"Peer $nodeName died. Removing from mDNS cache.")
      mdnsCache -= nodeName
      log.info(s"New cache state = $mdnsCache")
  }

  private def registerNewPeer(nodeName: String, nodeAddr: InetSocketAddress): Unit = {
    val oldAddr = mdnsCache.get(nodeName)
    if (!oldAddr.contains(nodeAddr)) {
      log.info(s"Updating caches.")
      mdnsCache += (nodeName -> nodeAddr)
      context.actorOf(Props(ConnectivityChecker(nodeName, nodeAddr)))
      log.info(s"New cache state = $mdnsCache")
    }
  }

  final case class ConnectivityChecker(
    remoteName: String,
    remoteAddr: InetSocketAddress
  ) extends Actor with ActorLogging {

    import Tcp._

    tcpManager ! Connect(remoteAddr)

    private case object Tick

    system.scheduler.schedule(1 second, 10 seconds, self, Tick)

    override def receive: Receive = {
      case Connected(remoteAddress, localAddress) =>
        context.become(ready(sender))
      case CommandFailed(_: Connect) =>
        log.error(s"Failed to connect to remote node $remoteName @ $remoteAddr from node $addr.")
        context stop self
    }

    private def ready(send: ActorRef): Receive = {
      case Tick =>
        import scala.util.{Success, Failure}

        implicit val timeout: Timeout = 10 seconds
        val response = send ? Write(ByteString(msg.toString))
        log.info(s"Pinging remote node.")
        response.onComplete {
          case Failure(_) =>
            send ! Close
            context.parent ! PeerDied(remoteName)
            context stop self
          case Success(s) =>
            log.info(s"Got response from remote $s.")
            send ! Close
        }
    }
  }

}
