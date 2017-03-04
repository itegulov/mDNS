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

  import context.system

  private val opts = List(
    InetProtocolFamily,
    ReuseAddress(true),
    MulticastGroup(group, interface)
  )

  private val tcpManager = IO(Tcp)
  private val udpManager = IO(Udp)

  udpManager ! Udp.Bind(self, new InetSocketAddress(group, port), opts)
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
    //      sender ! PoisonPill
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
        case _ =>
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
      context.actorOf(Props(ConnectivityChecker(msg, nodeName, nodeAddr)))
      log.info(s"New cache state = $mdnsCache")
    }
  }
}

  final case class ConnectivityChecker(
    msg: NewPeerAlive,
    remoteName: String,
    remoteAddr: InetSocketAddress
  ) extends Actor with ActorLogging {
    import context.{system, dispatcher}
    import Tcp._
    private val manager = IO(Tcp)

    manager ! Connect(remoteAddr)

    private case object Tick
    private case object Timeout


    override def receive: Receive = {
      case Connected(remoteAddress, localAddress) =>
        sender ! Register(self)
        sender ! Write(ByteString(msg.toString))
        sender ! Close
        context.system.scheduler.schedule(0 millis, 10 seconds, self, Timeout)
        context.become(ready)
      case CommandFailed(_: Connect) =>
        log.error(s"Failed to connect to remote node $remoteName @ $remoteAddr.")
        context stop self
    }
    
    private[this] var isAlive = true

    private def ready: Receive = {
      case Tick =>
        manager ! Connect(remoteAddr)
      case CommandFailed(_: Connect) => 
        isAlive = false
        context.system.scheduler.scheduleOnce(100 millis, self, Tick)
      case Connected(remoteAddress, localAddress) =>
        isAlive = true
        sender ! Close
      case Timeout => 
        if (!isAlive) {
          context.parent ! PeerDied(remoteName)
          context stop self
        }
        context.system.scheduler.scheduleOnce(0 millis, self, Tick)
    }
  }

