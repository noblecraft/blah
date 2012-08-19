package com.davezhu.blah.web

import org.scalatest.{GivenWhenThen, FeatureSpec}
import org.joda.time.{DateTimeConstants, DateTime}
import collection.mutable.ArrayBuffer
import com.davezhu.blah.core.Pricing


class QuotesSpec extends FeatureSpec with GivenWhenThen {

  val SYMBOL = "ESM2"

  val EMPTY: Seq[Pricing] = Seq()

  val DTS_NOW = new DateTime(2012, DateTimeConstants.JANUARY, 1, 0, 0, 0)

  val NO_DELAY = 0

  val quote1 = book(DTS_NOW.minusMinutes(5).minusSeconds(1))
  val quote2 = book(DTS_NOW.minusMinutes(1).minusSeconds(1))
  val quote3 = book(DTS_NOW.minusSeconds(59))
  val quote4 = book(DTS_NOW)

  feature("Quotes create / append") {

    scenario("Append quotes to empty quotes object") {

      given("I have an empty quotes object")
      val quotes = new Quotes()
      when("I append 2 quotes to it")
      quotes += book(new DateTime())
      quotes += book(new DateTime())
      then("the size of quotes should be 2")
      assert(quotes.size == 2)
      assert(quotes.seq == 2)
    }

    scenario("Creating quotes object with invalid seq numbers") {

      try {
        new Quotes(quotes = ArrayBuffer((2L, book(new DateTime)), (1L, book(new DateTime()))))
      } catch {
        case e: AssertionError => Unit // expected
        case e => fail("Should throw AssertionError")
      }

    }

    scenario("Append quotes to non-empty quotes object") {

      given("I have an quotes object with 2 quotes")
      val quotes = new Quotes(quotes = ArrayBuffer((0L, book(new DateTime)), (1L, book(new DateTime()))))
      when("I append 2 quotes to it")
      quotes += book(new DateTime())
      quotes += book(new DateTime())
      then("the size of quotes should be 4")
      assert(quotes.size == 4)
      assert(quotes.seq == 4)

    }

  }

  feature("Quotes data access") {

    scenario("Accessing quotes data since, with no delay") {

      given("I have a 3 quotes")
      val quotes = createQuotes(NO_DELAY, quote1, quote2, quote3)
      when("I get quotes since 1")
      val quotesSince = quotes.since(1L)
      then("there I should get 2 quotes")
      assert(quotesSince.size == 2)
      assert(quotesSince.contains(quote2))
      assert(quotesSince.contains(quote3))

    }

    scenario("Accessing quotes data with 1 minute delay") {

      given("I have a 4 quotes with a 1 minute delay")
      val quotes = createQuotes(ONE_MINUTE, quote1, quote2, quote3, quote4)
      when("I get quotes since 1")
      val quotesSince = quotes.since(1L)
      then("I should get only quotes older than the delay")
      assert(quotesSince.size == 1)
      assert(quotesSince.contains(quote2))

    }

    scenario("Get last quote with no delay") {

      given("I have 4 quotes with no delay")
      val quotes = createQuotes(NO_DELAY, quote1, quote2, quote3, quote4)
      when("I get lastest quote")
      val lastQuote = quotes.latest
      then("I should get quote#4")
      assert(lastQuote == quote4)

    }

    scenario("Get last quote with 1 minute delay") {

      given("I have 4 quotes with 1 minute delay")
      val quotes = createQuotes(ONE_MINUTE, quote1, quote2, quote3, quote4)
      when("I get lastest quote")
      val lastQuote = quotes.latest
      then("I should get quote#2")
      assert(lastQuote == quote2)

    }

  }

  feature("Quotes discard") {

    scenario("Quotes older than 5 minutes should be discarded") {

      given("I have 4 quotes with only quote#1 older than 5 minutes")
      val quotes = createQuotes(NO_DELAY, quote1, quote2, quote3, quote4)
      when("I discard old quotes")
      quotes.discardOld
      then("quote#1 should be the only one removed")
      assert(quotes.size == 3)
      assert(quotes.contains(quote1) == false)

    }

  }

  def book(dts: DateTime) = Book(SYMBOL, dts, bids = EMPTY, asks = EMPTY)

  def createQuotes(delay: Int, quotes: Book*) = {
    val out = new Quotes(delay = delay, dateTimeService = mockDateTimeService())
    for (quote <- quotes) {
      out += quote
    }
    out
  }

  def mockDateTimeService(dts: DateTime = DTS_NOW) = new DateTimeService {
    override def now = dts
  }


}
