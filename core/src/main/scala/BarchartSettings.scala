package com.davezhu.blah.core

import org.springframework.stereotype.Component
import java.net.URL
import java.text.MessageFormat
import xml.XML
import org.springframework.beans.factory.annotation.{Autowired, Value}

@Component
class BarchartSettings @Autowired() (@Value("${barchart.settings}") val settingsUrl: String,
                                     @Value("${barchart.user}") val user: String,
                                     @Value("${barchart.password}") val password: String) {

  val settings = {
    val url: URL = new URL(MessageFormat.format(settingsUrl, user, password))
    XML.load(url)
  }

  val streamServer = getServers("stream")

  val historicalServers = getServers("historicalv2")

  def getServers(serverType: String) = {
    val serverNode = settings \\ "server" find { n => (n \ "@type" text) == serverType }
    Seq(serverNode.map { _ \ "@primary" text }, serverNode.map { _ \ "@secondary" text })
  }

}
