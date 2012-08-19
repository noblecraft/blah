package com.davezhu.blah.core

import org.scalatest.mock.EasyMockSugar
import org.easymock.EasyMock.isA
import ddf.{IJerqClientListener, JerqClient}
import actors._
import Actor._
import org.scalatest.{BeforeAndAfter, GivenWhenThen, FeatureSpec}
import org.easymock.{IAnswer, EasyMock}
import scalaz.concurrent.Promise
import ddf.db.{Session, Quote}


class BarchartQuoteServiceSpec extends FeatureSpec with GivenWhenThen with EasyMockSugar with BeforeAndAfter {

  val TIME_OUT = 8000L
  val TIME_OUT_SHORT = 500L

  val SYMBOL_ES = "ESM2"
  val SYMBOL_IBM = "IBM"

  val NOTIF_IBM_TICK_1 = NotifyTick(symbol = SYMBOL_IBM, trade = Pricing(100.0, 10))

  val NOTIF_ES_TICK_1 = NotifyTick(symbol = SYMBOL_ES, trade = Pricing(100.0, 10))
  val NOTIF_ES_TICK_2 = NotifyTick(symbol = SYMBOL_ES, trade = Pricing(101.0, 10))
  val NOTIF_ES_TICK_3 = NotifyTick(symbol = SYMBOL_ES, trade = Pricing(101.0, 11))

  val NOTIF_QUOTE_ES_1 = NotifyQuote(symbol = SYMBOL_ES, bid = Pricing(100.0, 10), ask = Pricing(102.0, 12))
  val NOTIF_QUOTE_ES_2 = NotifyQuote(symbol = SYMBOL_ES, bid = Pricing(200.0, 10), ask = Pricing(202.0, 12))

  val NOTIF_QUOTE_IBM_1 = NotifyQuote(symbol = SYMBOL_IBM, bid = Pricing(100.0, 10), ask = Pricing(102.0, 12))
  val NOTIF_QUOTE_IBM_2 = NotifyQuote(symbol = SYMBOL_IBM, bid = Pricing(200.0, 10), ask = Pricing(202.0, 12))

  val NOTIF_DISCONNECTED = NotifyStatus(ServiceStatus.Disconnected, None)
  val NOTIF_CONNECTED = NotifyStatus(ServiceStatus.Connected, None)

  val mockJerqClient = niceMock[JerqClient]
  val mockQuote1 = mock[Quote]
  val mockQuote2 = mock[Quote]
  val mockSession = mock[Session]

  // Trap to be linked with all worker (mock subscribers) threads, to be notified when the worker thread finishes
  val trap = self
  trap.trapExit = true

  before {
    EasyMock.reset(mockJerqClient, mockQuote1, mockQuote2, mockSession)
    mockQuote(mock = mockQuote1, bid = Pricing(100.0, 10), ask = Pricing(102.0, 12))
    mockQuote(mock = mockQuote2, bid = Pricing(200.0, 10), ask = Pricing(202.0, 12))
    //mockJerqClient.removeClientListener(isA(classOf[IJerqClientListener])).anyTimes
  }

  feature("User should be able to subscribe to BarChartQuoteService to receive quotes") {

    scenario("User subscribes using valid symbol") {

      withService { service =>

        val jerqClient = expectJerqClientRegistrationForSymbol(SYMBOL_ES)

        mockQuote1.isTick.andReturn(true)
        mockQuote2.isTick.andReturn(true)

        expectSessionLastPrices((100.0, 10), (100.0, 10))

        whenExecuting(mockJerqClient, mockSession, mockQuote1, mockQuote2) {

          given("the user has not subscribed to the symbol")

          when("user subscribes to the symbol")
          val subscriber = subscriberToReceiveMessages(SYMBOL_ES, NOTIF_ES_TICK_1, NOTIF_QUOTE_ES_1,
            NOTIF_ES_TICK_1, NOTIF_QUOTE_ES_2)
          service.subscribe(subscriber, SYMBOL_ES, QuoteLevel.I)

          then("user should be notified of quotes for the symbol in a sequential fashion")
          sendQuotes(jerqClient.get, mockQuote1, mockQuote2)

        }

      }

      trapExits(1)

    }

    scenario("User tries to subscribe to a symbol already subscribed to") {

      withService { service =>

        val jerqClient = expectJerqClientRegistrationForSymbol(SYMBOL_ES)

        mockQuote1.isTick.andReturn(true)
        mockQuote2.isTick.andReturn(true)
        expectSessionLastPrices((100.0, 10), (100.0, 10))

        whenExecuting(mockJerqClient, mockSession, mockQuote1, mockQuote2) {

          val subscriber = subscriberToReceiveMessages(SYMBOL_ES, NOTIF_ES_TICK_1, NOTIF_QUOTE_ES_1, NOTIF_ES_TICK_1,
            NOTIF_QUOTE_ES_2)

          given("the user has already subscribed to the symbol")
          service.subscribe(subscriber, SYMBOL_ES, QuoteLevel.I)

          when("user attempts to subscribe to the symbol again")
          service.subscribe(subscriber, SYMBOL_ES, QuoteLevel.I)

          then("the user should not receive duplicate quotes")
          sendQuotes(jerqClient.get, mockQuote1, mockQuote2)
        }

      }

      trapExits(1)

    }

    scenario("Multiple subscribers to the same symbol") {

      withService { service =>

        val jerqClient1 = expectJerqClientRegistrationForSymbol(SYMBOL_ES)
        val jerqClient2 = expectJerqClientRegistrationForSymbol(SYMBOL_IBM)

        mockQuote1.isTick.andReturn(true).times(2)
        mockQuote2.isTick.andReturn(true).times(2)
        expectSessionLastPrices((100.0, 10), (100.0, 10))
        expectSessionLastPrices((100.0, 10), (100.0, 10))

        whenExecuting(mockJerqClient, mockSession, mockQuote1, mockQuote2) {

          // 3 subscribers
          when("multiple users subscribes to the same symbol")
          service.subscribe(subscriberToReceiveMessages(SYMBOL_ES, NOTIF_ES_TICK_1, NOTIF_QUOTE_ES_1,
            NOTIF_ES_TICK_1, NOTIF_QUOTE_ES_2), SYMBOL_ES, QuoteLevel.I)
          service.subscribe(subscriberToReceiveMessages(SYMBOL_ES, NOTIF_ES_TICK_1, NOTIF_QUOTE_ES_1,
            NOTIF_ES_TICK_1, NOTIF_QUOTE_ES_2), SYMBOL_ES, QuoteLevel.I)
          service.subscribe(subscriberToReceiveMessages(SYMBOL_IBM, NOTIF_IBM_TICK_1, NOTIF_QUOTE_IBM_1,
            NOTIF_IBM_TICK_1, NOTIF_QUOTE_IBM_2), SYMBOL_IBM, QuoteLevel.I)

          then("each subscriber should receive quotes for the symbol")
          sendQuotes(jerqClient1.get, mockQuote1, mockQuote2)
          sendQuotes(jerqClient2.get, mockQuote1, mockQuote2)

        }

      }

      trapExits(3)

    }

    scenario("User should receive last price notifications only whent he price or qty has changed")(pending)

    scenario("User tries to subscribe using invalid symbol")(pending)

    scenario("Quote service status") {

      withService { service =>

        given("there are subscribers to the quote service")
        val subscriber1 = subscriberToReceiveMessages(SYMBOL_ES, NOTIF_DISCONNECTED, NOTIF_CONNECTED)
        val subscriber2 = subscriberToReceiveMessages(SYMBOL_IBM, NOTIF_DISCONNECTED, NOTIF_CONNECTED)
        service.subscribe(subscriber1, SYMBOL_ES, QuoteLevel.I)
        service.subscribe(subscriber2, SYMBOL_IBM, QuoteLevel.I)
        when("server goes down and later service is restored")
        service ! NotifyStatus(ServiceStatus.Disconnected)
        service ! NotifyStatus(ServiceStatus.Connected)
        then("users should be notified of the service statuses")

      }

      trapExits(2)

    }

  }

  feature("User should be able to unsubscribe from BarChartQuoteService") {

    scenario("User unsubscribe from a single symbol") {

      withService { service =>

        given("user is subscribed to the symbol")
        val jerqClient = expectJerqClientRegistrationForSymbol(SYMBOL_ES)
        val subscriber1 = subscriberToReceiveMessages(SYMBOL_ES, NOTIF_QUOTE_ES_1)
        val subscriber2 = subscriberToReceiveMessages(SYMBOL_ES, NOTIF_QUOTE_ES_1, NOTIF_QUOTE_ES_2)

        mockQuote1.isTick.andReturn(false).anyTimes
        mockQuote2.isTick.andReturn(false).anyTimes

        whenExecuting(mockJerqClient, mockSession, mockQuote1,mockQuote2) {

          service.subscribe(subscriber1, SYMBOL_ES, QuoteLevel.I)
          service.subscribe(subscriber2, SYMBOL_ES, QuoteLevel.I)
          sendQuotes(jerqClient.get, mockQuote1)

          when("user unsubscribes from the symbol")
          service.unSubscribe(subscriber1, SYMBOL_ES)

          then("user should not receive further quotes for the symbol")
          sendQuotes(jerqClient.get, mockQuote2)

        }

      }

      trapExits(2)

    }

    scenario("User unsubscribe from all symbols")(pending)

    scenario("User tries to unsubscribe to symbol that is not subscribed to")(pending)

    // should call jerqClient.removeClientListener()
    scenario("All clients for a symbol have unsubscribed")(pending)

  }

  def withService(fun: (BarchartQuoteService) => Unit) {
    val service = new BarchartQuoteService(mockJerqClient)
    service.start
    fun(service)
    service ! 'Stop
  }

  def sendQuotes(client: IJerqClientListener, quotes: Quote*) {
    quotes.foreach { quote =>
      client.alertData(quote)
    }
  }

  // The 'worker' actors are linked to the main trap actor, when
  // all workers exit normally ('normal), then this will method will complete normally,
  // otherwise, if at least one worker exits abnormally (e.g. by with an exception) then this will fail
  def trapExits(count: Int) {
    times(count) {
      trap.receiveWithin(TIME_OUT) {
        case Exit(from, 'normal) => Unit
        case Exit(from, e: Exception) => throw e
        case e => fail("Unexpected: " + e)
      }
    }
  }

  def times(count: Int)(fun: => Unit) {
    for (i <- 0 until count) {
      fun
    }
  }

  def subscriberToReceiveMessages(symbol: String, messages: Any*) = {

    actor {

      link(trap)

      messages.foreach { message =>

        receiveWithin(TIME_OUT) {

          case TIMEOUT => fail("Did not receive the expected message: " + message + ", within set a set time")
          case actual if (actual == message) => Unit
          case actual => fail("Did not receive the expected message: " + message + ", instead received: " + actual)

        }

      }

      // make sure no more messages...

      receiveWithin(TIME_OUT_SHORT) {
        case TIMEOUT => exit()
        case e => fail("Unexpected message: " + e)
      }

    }

  }

  def expectSessionLastPrices(prices: (Double, Int)*) {
    prices.foreach { p =>
      mockSession.getLast.andReturn(p._1.toFloat)
      mockSession.getLastSize.andReturn(p._2)
    }
  }

  def expectJerqClientRegistrationForSymbol(symbol: String): Promise[IJerqClientListener] = {

    val promise = new Promise[IJerqClientListener]()

    mockJerqClient.addClientListener(isA(classOf[IJerqClientListener])).andAnswer(new IAnswer[Unit] {
      def answer() = {
        val client = EasyMock.getCurrentArguments()(0).asInstanceOf[IJerqClientListener]
        promise.fulfill(client)
        assert(client.getWatchList == symbol + "=SsBb")
        Unit
      }

    })

    promise

  }

  def mockQuote(mock: Quote, bid: Pricing, ask: Pricing) = {
    mock.getBid.andReturn(bid.price.toFloat).anyTimes
    mock.getBidSize.andReturn(bid.qty).anyTimes
    mock.getAsk.andReturn(ask.price.toFloat).anyTimes
    mock.getAskSize.andReturn(ask.qty).anyTimes
    mock.getCombinedSession.andReturn(mockSession).anyTimes
    mock
  }

}
