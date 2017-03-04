package itmo.ctddev.mdns.core

import java.net.StandardProtocolFamily
import java.nio.channels.DatagramChannel

import akka.io.Inet.DatagramChannelCreator

/**
  * Created by sugakandrey.
  */
case object InetProtocolFamily extends DatagramChannelCreator {
  override def create(): DatagramChannel =
    DatagramChannel.open(StandardProtocolFamily.INET)
}
