package com.davezhu.blah.web

import actors.{TIMEOUT, Actor}
import org.joda.time.DateTime
import com.davezhu.blah.core.QuoteLevel
import com.davezhu.blah.core.QuoteService
import com.davezhu.blah.core.Pricing
import com.davezhu.blah.core.NotifyBook
import com.davezhu.blah.core.NotifyTick

sealed abstract class Quote(val dts: DateTime)

case class Book(symbol: String, override val dts: DateTime, bids: Seq[Pricing], asks: Seq[Pricing]) extends Quote(dts)

case class Tick(symbol: String, override val dts: DateTime, pricing: Pricing) extends Quote(dts)

case class LongPoll(symbol: String, seq: Long, replyTo: Actor)

// This is the reply to LongPoll messages
case class QuotesReply(quotes: Seq[Quote])

class QuoteProvider(val quoteService: QuoteService) extends Actor {

  val quotes = new Quotes

  val polls = scala.collection.mutable.ArrayBuffer[LongPoll]()

  def act() {

    // H=March
    // M=June
    // U=September
    // Z=December

    // symbol is static for now
    quoteService.subscribe(Actor.self, ES_SYMBOL, QuoteLevel.I)

    while(true) {

      receiveWithin(500L) {

        case NotifyTick(symbol, trade) => {

          quotes += Tick(symbol, new DateTime(), trade)

          sendQuotesAndClearPolls

          quotes.discardOld

        }

        case NotifyBook(symbol, bids, asks) => {

          // TODO: per symbol

          quotes += Book(symbol, new DateTime(), bids, asks)

          sendQuotesAndClearPolls

          quotes.discardOld

        }

        case lp @ LongPoll(symbol, seq, replyTo) => {

          if (!sendQuotes(lp)) {
            polls += lp
          }

        }

        case TIMEOUT => quotes.discardOld

        case 'Stop => exit

        case _ => Unit

      }

    }

  }

  private def sendQuotesAndClearPolls {

    polls.map(sendQuotes(_)).zipWithIndex.foreach({
      case (true, i) => polls.remove(i)
      case _ => Unit
    })

  }

  private def sendQuotes(p: LongPoll): Boolean = {

    val quotesSince = quotes.since(p.seq)

    if (quotesSince.size > 0) {

      Actor.actor {
        p.replyTo ! QuotesReply(quotesSince)
      }

      true

    } else {

      false

    }

  }

}

// Quotes simulator that returns random tick/book data
class MockProvider extends Actor {

  val BASE_PRICE = 10000.00

  val polls = scala.collection.mutable.ArrayBuffer[LongPoll]()

  def act() {

    loop {

      reactWithin(randomWait) {

        case TIMEOUT => {
          polls.foreach(_.replyTo ! QuotesReply(randomQuotes))
          polls.clear
        }

        case lp @ LongPoll(symbol, seq, replyTo) => polls += lp

      }

    }

  }

  def randomWait = (math.random * 10000).toInt

  def randomQuotes: Seq[Quote] = {

    if (math.random > 0.5) {
      // tick
      Seq(Tick(ES_SYMBOL, new DateTime(), randomPricings(1).head))
    } else {
      // book
      val bids = randomPricings(10).sortBy(_.price)
      val asks = bids.map(p => Pricing(p.price + 1.0, p.qty))
      Seq(Book(ES_SYMBOL, new DateTime(), bids, asks))
    }

  }

  def randomPricings(n: Int) = for(_ <- 0 until n) yield Pricing(BASE_PRICE + 5000 * math.random, (10 * math.random).toInt)

}
