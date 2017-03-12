package itmo.ctddev.mdns.core

import java.net._

import akka.io.Inet.SocketOptionV2

/**
  * Created by sugakandrey.
  */
final case class MulticastGroup(
  address: String, 
  networkInterface: NetworkInterface
) extends SocketOptionV2 {

  override def afterBind(s: DatagramSocket): Unit = {
    val group = InetAddress.getByName(address)
    val membership = s.getChannel.join(group, networkInterface)
    println(s"Joined group $membership")
  }
}
