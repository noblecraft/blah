package com.davezhu.blah.web

import com.davezhu.blah.core.Pricing
import org.scalatest.{FlatSpec, GivenWhenThen}
import org.scalatest.matchers.ShouldMatchers
import org.joda.time.{DateTimeConstants, DateTime}

class JsonConvertorTest extends FlatSpec with GivenWhenThen with ShouldMatchers {

  val SYMBOL = "ESM2"

  val DTS_NOW = new DateTime(2012, DateTimeConstants.JANUARY, 1, 0, 0)

  val EXPECTED_BOOK_JSON_1: String = "{\"symbol\":\"ESM2\",\"dts\":\"2012-01-01T00:00:00.000+10:00\"}"

  val EXPECTED_BOOK_JSON_2: String =
    "{" +
      "\"symbol\":\"ESM2\"," +
      "\"dts\":\"2012-01-01T00:00:00.000+10:00\"," +
      "\"bids\":[{\"price\":10.0,\"qty\":1},{\"price\":11.1,\"qty\":2}]," +
      "\"asks\":[{\"price\":12.2,\"qty\":1},{\"price\":13.3,\"qty\":2}]" +
    "}"

  val EXPECTED_TICK_JSON = "{\"symbol\":\"ESM2\",\"dts\":\"2012-01-01T00:00:00.000+10:00\",\"pricing\":{\"price\":11.1,\"qty\":1}}"

  val EXPECTED_QUOTES_JSON = "[" +
    "{\"symbol\":\"ESM2\",\"dts\":\"2012-01-01T00:00:00.000+10:00\",\"pricing\":{\"price\":11.1,\"qty\":1}}," +
    "{\"symbol\":\"ESM2\",\"dts\":\"2012-01-01T00:00:00.000+10:00\"," +
      "\"bids\":[{\"price\":10.0,\"qty\":1},{\"price\":11.1,\"qty\":2}]," +
      "\"asks\":[{\"price\":12.2,\"qty\":1},{\"price\":13.3,\"qty\":2}]" +
    "}]"

  val BIDS = Seq(Pricing(10.0, 1), Pricing(11.1, 2))
  val ASKS = Seq(Pricing(12.2, 1), Pricing(13.3, 2))

  "JsonConvert" should "convert arrays" in {
    JsonConvertor.toArrayJson(Seq(Pricing(10.0, 2)), JsonConvertor.toPricingJson) should equal ("[{\"price\":10.0,\"qty\":2}]")
    JsonConvertor.toArrayJson(Seq(Pricing(10.0, 2), Pricing(11.1, 3)), JsonConvertor.toPricingJson) should equal ("[{\"price\":10.0,\"qty\":2},{\"price\":11.1,\"qty\":3}]")
  }

  it should "convert pricing" in {
    JsonConvertor.toPricingJson(Pricing(10.0, 2)) should equal ("{\"price\":10.0,\"qty\":2}")
  }

  it should "convert book" in {
    JsonConvertor.toBookJson(Book(SYMBOL, DTS_NOW, Seq(), Seq())) should equal (EXPECTED_BOOK_JSON_1)
    JsonConvertor.toBookJson(Book(SYMBOL, DTS_NOW, BIDS, ASKS)) should equal (EXPECTED_BOOK_JSON_2)
  }

  it should "convert tick" in {
    JsonConvertor.toTickJson(Tick(SYMBOL, DTS_NOW, Pricing(11.1, 1))) should equal (EXPECTED_TICK_JSON)
  }

  it should "convert quotes" in {
    val quotes: Seq[Quote] = Seq(Tick(SYMBOL, DTS_NOW, Pricing(11.1, 1)), Book(SYMBOL, DTS_NOW, BIDS, ASKS))
    JsonConvertor.toQuotesJson(quotes) should equal (EXPECTED_QUOTES_JSON)
  }

}
