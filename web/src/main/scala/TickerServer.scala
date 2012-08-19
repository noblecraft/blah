package com.davezhu.blah.web

import unfiltered.netty.Server
import unfiltered.netty.async.Plan
import unfiltered.request.{GET, Params}
import actors.TIMEOUT
import unfiltered.response.{ContentType, ResponseString, Ok}
import org.jboss.netty.channel.ChannelHandlerContext
import scala.actors.Actor._


object TickerServer {

  def configure(port: Int): Server = {
    unfiltered.netty.Http(port).plan(new TickerPlan)
  }

  class TickerPlan extends Plan {

    val provider = new MockQuoteProvider//new QuoteProvider(AppContext.getBean(classOf[QuoteService]))

    provider.start

    def intent = {

      case req @ GET(Params(params)) => {

        actor {

          provider ! LongPoll("ESM2", 1, self)

          reactWithin(ONE_MINUTE * 3) {

            case quotesReply: QuotesReply =>
              println("[INFO] received LongPoll response: " + quotesReply + ", sending to client")
              req.respond(Ok ~> ContentType("application/json") ~> ResponseString(JsonConvertor.toQuotesJson(quotesReply.quotes)))

            case TIMEOUT =>
              println("[INFO] did not receive any LongPoll responses in time...")
              req.respond(Ok ~> ResponseString("TIMEOUT, PLS RETRY\n\n"))

          }

        }

      }

    }

    def onException(ctx: ChannelHandlerContext, t: Throwable) {}

  }

}
