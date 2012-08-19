package com.davezhu.blah.core


import actors.Actor

object QuoteLevel extends Enumeration {

  type QuoteLevel = Value

  val I = Value("I")
  val II = Value("II")

}

object ServiceStatus extends Enumeration {

  type ServiceStatus = Value

  val Connected = Value("Connected")
  val Disconnected = Value("Disconnected")
  val UnableToConnect = Value("UnableToConnect")
  val Unknown = Value("Unkown")

}

trait QuoteService {

  def subscribe(subscriber: Actor, symbol: String, level: QuoteLevel.QuoteLevel)

  def unSubscribe(subscriber: Actor, symbol: String)

  def stop: Unit

}

case class Pricing(price: Double, qty: Int)

sealed trait QuoteMessage

case class NotifyQuote(symbol: String, bid: Pricing, ask: Pricing) extends QuoteMessage

case class NotifyBook(symbol: String, bids: Seq[Pricing], asks: Seq[Pricing]) extends QuoteMessage

case class NotifyTick(symbol: String, trade: Pricing) extends QuoteMessage

case class NotifyStatus(status: ServiceStatus.ServiceStatus, msg: Option[String] = None) extends QuoteMessage

case class Subscribe(subscriber: Actor, symbol: String, level: QuoteLevel.QuoteLevel) extends QuoteMessage

case class UnSubscribe(subscriber: Actor, symbol: Option[String]) extends QuoteMessage

