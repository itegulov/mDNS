package itmo.ctddev.mdns.utils

import java.net.{InetSocketAddress, NetworkInterface}
import java.util.Collections

/**
  * Created by Aleksei Latyshev on 12.03.2017.
  */
object Utils {
  def getInterface: NetworkInterface = {
    import collection.JavaConverters._
    Collections
      .list(NetworkInterface.getNetworkInterfaces)
      .asScala
      .filter(
        x =>
          x.getDisplayName.toLowerCase.contains("wireless")
            || x.getName.toLowerCase.startsWith("wl")
      )
      .find(x => x.isUp && !x.isVirtual && !x.isLoopback && x.supportsMulticast)
      .getOrElse(throw new IllegalStateException("Couldn't find proper network interface"))
  }

  def getInetSocketAddress(args: Array[String]): InetSocketAddress =
    args match {
      case Array(ip, port) => new InetSocketAddress(ip, port.toInt)
      case Array(port) =>
        val address = getInterface.getInetAddresses.nextElement()
        new InetSocketAddress(address, port.toInt)
      case _ => throw new IllegalArgumentException("Specify address with port or port")
    }
}
