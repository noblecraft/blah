package com.davezhu.blah.core

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.beans.factory.FactoryBean
import ddf.JerqClient
import ddf.net.IConnectionListener

@Component
class JerqClientFactory @Autowired()(val settings: BarchartSettings,
                                     val context: ApplicationContext) extends FactoryBean[JerqClient] {

  val CONNECTION_FAILED_MSG = "Connection failed - possibly due to network problems"

  val USER_LOCKOUT_MSG = "User locked out - possibly due to multiple connections"

  def getObject: JerqClient = {

    new JerqClient(settings.user, settings.password, settings.streamServer(0).get, JerqClient.TCP) {

      override def newConnectionEvent(eventId: Int) {

        val status = eventId match {
          case IConnectionListener.CONNECTED => (ServiceStatus.Connected, None)
          case IConnectionListener.DISCONNECTED => (ServiceStatus.Disconnected, None)
          case IConnectionListener.CONNECTION_FAILED => (ServiceStatus.UnableToConnect, Some(CONNECTION_FAILED_MSG))
          case IConnectionListener.USER_LOCKOUT => (ServiceStatus.UnableToConnect, Some(USER_LOCKOUT_MSG))
          case unknown => (ServiceStatus.Unknown, Some("Unknown service status code: " + unknown))
        }

        val service = context.getBean(classOf[BarchartQuoteService])

        service ! NotifyStatus(status._1, status._2)

      }

    }

  }

  def getObjectType = classOf[JerqClient]

  def isSingleton = true

}
