package com.davezhu.blah.web

import actors.{TIMEOUT, Actor}
import org.joda.time.{Duration, DateTime}
import com.davezhu.blah.core._
import com.davezhu.blah.core.Pricing
import com.davezhu.blah.core.NotifyTick
import com.davezhu.blah.core.NotifyBook
import org.slf4j.LoggerFactory

sealed abstract class Quote(val dts: DateTime)

case class Book(symbol: String, override val dts: DateTime, bids: Seq[Pricing], asks: Seq[Pricing]) extends Quote(dts)

case class Tick(symbol: String, override val dts: DateTime, pricing: Pricing) extends Quote(dts)

// LongPoll message sent to QuoteProvider
case class LongPoll(symbol: String, seq: Long, replyTo: Actor, dts: DateTime = new DateTime())

// This is the reply to LongPoll messages
case class QuotesReply(quotes: Seq[Sequenced[Quote]])


// TODO - test
class QuoteProvider(val quoteService: QuoteService, val dateTimeService: DateTimeService = new DateTimeService {}) extends Actor {

  val LOG = LoggerFactory.getLogger(classOf[QuoteProvider])

  val quotes = new Quotes(delay = 1)

  val polls = scala.collection.mutable.ArrayBuffer[LongPoll]()

  def act() {

    // H=March
    // M=June
    // U=September
    // Z=December

    // symbol is static for now
    quoteService.subscribe(Actor.self, ES_SYMBOL, QuoteLevel.II)

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

        case lp: LongPoll => {
          Logging.debug(LOG, "LongPoll: " + lp)
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
    removeOldPolls
    polls.map(sendQuotes(_)).zipWithIndex.foreach({
      case (true, i) => polls.remove(i)
      case _ => Unit
    })
  }

  private def removeOldPolls {
    val now = dateTimeService.now
    val oldPolls = polls.filter(p => new Duration(p.dts, now).getMillis > FIVE_MINUTES)
    polls --= oldPolls
    Logging.debug(LOG, "Removed " + oldPolls.size + " LongPolls for being > 5 minutes old")
    Logging.debug(LOG, "LongPolls size=" + polls.size)
  }

  private def sendQuotes(p: LongPoll): Boolean = {

    val quotesSince = getQuotes(p)

    if (quotesSince.size > 0) {

      Actor.actor { p.replyTo ! QuotesReply(quotesSince) }

      true

    } else {

      false

    }

  }

  private def getQuotes(p: LongPoll): scala.Seq[Sequenced[Quote]] = {
    val quotesSince = if (p.seq < 0) {
      quotes.latest.map(Seq(_)).getOrElse(Seq())
    } else {
      quotes.since(p.seq)
    }
    quotesSince
  }

}

// Quotes simulator that returns random tick/book data
class MockQuoteProvider extends Actor {

  val LOG = LoggerFactory.getLogger(classOf[MockQuoteProvider])

  val BASE_PRICE = 10000.00

  val polls = scala.collection.mutable.ArrayBuffer[LongPoll]()

  def act() {

    loop {

      reactWithin(randomWait) {

        case TIMEOUT =>
          polls.foreach (poll => {
            val quotes: Seq[Sequenced[Quote]] = randomQuotes
            Logging.debug(LOG, "Sending LongPoll response: " + quotes)
            poll.replyTo ! QuotesReply(quotes)
          })
          polls.clear

        case lp: LongPoll =>
          Logging.debug(LOG, "Received LongPoll request: " + lp)
          polls += lp

      }

    }

  }

  def randomWait = (math.random * ONE_MINUTE * 3).toInt

  def randomQuotes: Seq[Sequenced[Quote]] = {

    if (math.random > 0.5) {
      // tick
      Seq(Sequenced(1, Tick(ES_SYMBOL, new DateTime(), randomPricings(1).head)))
    } else {
      // book
      val bids = randomPricings(10).sortBy(_.price)
      val asks = bids.map(p => Pricing(p.price + 1.0, p.qty))
      Seq(Sequenced(1, Book(ES_SYMBOL, new DateTime(), bids, asks)))
    }

  }

  def randomPricings(n: Int) = for(_ <- 0 until n) yield Pricing(BASE_PRICE + 5000 * math.random, (10 * math.random).toInt)

}
