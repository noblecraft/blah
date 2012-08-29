package com.davezhu.blah.web

import unfiltered.netty.Server
import unfiltered.netty.async.Plan
import unfiltered.request._
import actors.TIMEOUT
import unfiltered.response._
import org.jboss.netty.channel.ChannelHandlerContext
import scala.actors.Actor._
import com.davezhu.blah.core.{Logging, QuoteService}
import org.slf4j.LoggerFactory
import unfiltered.response.ContentType
import unfiltered.response.ResponseString
import unfiltered.kit.NoOpResponder


object TickerServer {

  def configure(port: Int): Server = {
    unfiltered.netty.Http(port).plan(new TickerPlan)
  }

  class TickerPlan extends Plan {

    val LOG = LoggerFactory.getLogger(classOf[TickerPlan])

    val provider = new QuoteProvider(AppContext.getBean(classOf[QuoteService]))

    provider.start

    def intent = {

      case req @ GET(Params(params)) => {

        Logging.debug(LOG, "Request params: " + params)

        actor {

          provider ! LongPoll("ESM2", params.get("since").map(_(0).toLong).getOrElse(-1L), self)

          reactWithin(ONE_MINUTE * 3) {

            case quotesReply: QuotesReply =>
              Logging.debug(LOG, "Received LongPoll response: " + quotesReply + ", sending to client")
              req.respond(Ok ~> ContentType("application/json") ~> gzipEncoding(req) ~> ResponseString(JsonConvertor.toQuotesJson(quotesReply.quotes)))

            case TIMEOUT =>
              Logging.debug(LOG, "Did not receive any LongPoll responses in time...")
              req.respond(Ok ~> ResponseString("TIMEOUT, PLS RETRY\n\n"))

            case _ => Unit

          }

        }

      }

    }

    def onException(ctx: ChannelHandlerContext, t: Throwable) {}

    private def gzipEncoding(request: HttpRequest[Any]): ResponseFunction[Any] = {
      request match {
        case Decodes.GZip(req) => ContentEncoding.GZip ~> ResponseFilter.GZip
        case _ => NoOpResponder
      }
    }

  }

}
