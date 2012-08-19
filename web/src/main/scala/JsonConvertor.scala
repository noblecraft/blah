package com.davezhu.blah.web

import com.davezhu.blah.core.Pricing

object JsonConvertor {

  def toQuotesJson(quotes: Seq[Quote]) = toArrayJson(quotes, toQuoteJson)

  def toQuoteJson(quote: Quote): String = quote match {
    case b: Book => toBookJson(b)
    case t: Tick => toTickJson(t)
  }

  def toTickJson(tick: Tick) =
    "{\"symbol\":\"" + tick.symbol + "\",\"dts\":\"" + tick.dts + "\",\"pricing\":" + toPricingJson(tick.pricing) + "}"

  def toBookJson(book: Book) = {

    new StringBuilder("{\"symbol\":\"" + book.symbol + "\",\"dts\":\"" + book.dts + "\"").append(

      if (book.bids.size > 0) {
        ",\"bids\":" + toArrayJson(book.bids, toPricingJson)
      } else {
        ""
      }

    ).append(

      if (book.asks.size > 0) {
        ",\"asks\":" + toArrayJson(book.asks, toPricingJson)
      } else {
        ""
      }

    ).append("}").toString

  }

  def toPricingJson(p: Pricing) = "{\"price\":" + p.price + ",\"qty\":" + p.qty + "}"

  def toArrayJson[T](items: Seq[T], f: (T => String)) = {
    val s = items.foldLeft("") {
      (z, t) => z + (if (z.length > 0) "," else "") + f(t)
    }
    "[" + s + "]"
  }

}
