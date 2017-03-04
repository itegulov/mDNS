package itmo.ctddev.mdns.core

import java.net._

import akka.io.Inet.SocketOptionV2

/**
  * Created by sugakandrey.
  */
final case class MulticastGroup(
  address: String, 
  interface: String
) extends SocketOptionV2 {
  
  override def afterBind(s: DatagramSocket): Unit = {
    val group = InetAddress.getByName(address)
    val networkInterface = NetworkInterface.getByName(interface)
    s.getChannel.join(group, networkInterface)
    println(s"${s.getPort} Joined group $group interface $interface")
  }
}
