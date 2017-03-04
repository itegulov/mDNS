package itmo.ctddev.mdns.core

import java.net.InetSocketAddress

/**
  * Created by sugakandrey.
  */
sealed trait MNDSMessage

case class NewPeerAlive(name: String, addr: InetSocketAddress) extends MNDSMessage {
  override def toString: String = s"hey $name ${addr.getAddress.getHostAddress}:${addr.getPort}"
}

case class PeerDied(name: String) extends MNDSMessage
