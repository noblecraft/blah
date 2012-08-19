package com.davezhu.blah.web

import unfiltered.netty.Server
import unfiltered.netty.async.Plan
import unfiltered.request.{GET, Params}
import actors.{TIMEOUT, Actor, Futures}
import unfiltered.response.{ContentType, ResponseString, Ok}
import org.jboss.netty.channel.ChannelHandlerContext
import scala.actors.Actor._
import com.davezhu.blah.core.QuoteService


object TickerServer {

  def configure(port: Int): Server = {
    unfiltered.netty.Http(port).plan(new TickerPlan)
  }

  class TickerPlan extends Plan {

    val provider = new QuoteProvider(AppContext.getBean(classOf[QuoteService]))

    provider.start

    def toJson(quotes: Seq[Quote]): String = ""

    def intent = {

      case req @ GET(Params(params)) => {

        actor {

          provider ! LongPoll("ESM2", 1, self)

          reactWithin(5000L) {

            case quotesReply: QuotesReply =>
              req.respond(Ok ~> ContentType("application/json") ~> ResponseString(toJson(quotesReply.quotes)))

            case TIMEOUT => ResponseString("TIMEOUT, PLS RETRY\n\n")

          }

        }

      }

    }

    def onException(ctx: ChannelHandlerContext, t: Throwable) {}

  }

}
