package com.davezhu.blah.core

import actors.Actor
import ddf.db.{BookQuote, Session, Quote}
import ddf.net.IConnectionListener
import ddf.{IJerqClientListener, JerqClient}
import javax.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import QuoteLevel.QuoteLevel
import collection.mutable.{Map, Set}


@Service
class BarchartQuoteService @Autowired()(val client: JerqClient) extends QuoteService with Actor {
  actorRef =>

  val jerqListeners: Map[String, IJerqClientListener] = Map[String, IJerqClientListener]()

  val subscribersBySymbol: Map[String, Set[Actor]] = Map[String, Set[Actor]]()

  var connectionState = IConnectionListener.DISCONNECTED

  @PostConstruct
  def startService = super.start()

  def subscribe(subscriber: Actor, symbol: String, level: QuoteLevel) {
    actorRef ! Subscribe(subscriber, symbol, level)
  }

  def unSubscribe(subscriber: Actor, symbol: String) {
    actorRef ! UnSubscribe(subscriber, Some(symbol))
  }

  def unSubscribe(subscriber: Actor) {
    actorRef ! UnSubscribe(subscriber, None)
  }

  def stop {
    actorRef ! 'Stop
  }

  def act() {

    while (true) {

      receive {

        case msg@NotifyTick(symbol, _) => subscribersBySymbol.get(symbol).foreach {
          subscribers =>
            subscribers.foreach {
              _ ! msg
            }
        }

        case msg@NotifyQuote(symbol, _, _) => subscribersBySymbol.get(symbol).foreach {
          subscribers =>
            subscribers.foreach {
              _ ! msg
            }
        }

        case msg@NotifyBook(symbol, _, _) => subscribersBySymbol.get(symbol).foreach {
          subscribers =>
            subscribers.foreach {
              _ ! msg
            }
        }

        case msg: NotifyStatus => subscribersBySymbol.values.foreach(_.foreach(_ ! msg))

        case Subscribe(subscriber, symbol, level) =>

          if (!jerqListeners.contains(symbol)) {

            val listener = new SymbolListener(symbol)

            jerqListeners += ((symbol, listener))

            client.addClientListener(listener)

          }

          subscribersBySymbol.getOrElseUpdate(symbol, Set[Actor]()) += subscriber

        case UnSubscribe(subscriber, symbol) =>

          if (symbol.isDefined) {
            subscribersBySymbol.get(symbol.get).foreach {
              _.remove(subscriber)
            }
          } else {
            subscribersBySymbol.values.foreach {
              _ -= subscriber
            }
          }

          subscribersBySymbol.filter(_._2.isEmpty).map(_._1).foreach {
            symbol =>
              jerqListeners.remove(symbol).foreach {
                listener =>
                  client.removeClientListener(listener)
              }
          }

        case 'Stop =>
          println("Stopping...")
          jerqListeners.values.foreach {
            listener => client.removeClientListener(listener)
          }
          exit()

        case _ => Unit

      }

    }

  }

  /**
   * Listens to barchart messages for a particular symbol
   */
  class SymbolListener(val symbol: String) extends IJerqClientListener {

    def alertData(data: Any) {

      data match {

        case q: Quote => {

          val session: Session = q.getCombinedSession

          if (q.isTick) {
            actorRef ! NotifyTick(symbol, Pricing(price = session.getLast.toDouble, qty = session.getLastSize))
          }

          actorRef ! NotifyQuote(symbol = symbol, bid = Pricing(q.getBid.toDouble, q.getBidSize),
            ask = Pricing(q.getAsk.toDouble, q.getAskSize))

        }

        case b: BookQuote => {

          val askPrices = b.getAskData()(0).asInstanceOf[Array[Float]]
          val askSizes = b.getAskData()(1).asInstanceOf[Array[Int]]

          val bidPrices = b.getBidData()(0).asInstanceOf[Array[Float]]
          val bidSizes = b.getBidData()(1).asInstanceOf[Array[Int]]

          actorRef ! NotifyBook(
            symbol = symbol,
            bids = (bidPrices zip bidSizes).map {
              e => Pricing(price = e._1.toDouble, qty = e._2)
            },
            asks = (askPrices zip askSizes).map {
              e => Pricing(price = e._1.toDouble, qty = e._2)
            }
          )

        }

        case _ => Unit

      }

    }

    def getWatchList = symbol + "=SsBb"

    def newConnectionEvent(eventId: Int) {}

  }

}
