package com.davezhu.blah.web

import unfiltered.request.{Mime, HttpRequest, Seg, Path}
import unfiltered.response._
import org.fusesource.scalate.TemplateEngine
import unfiltered.jetty.{Server, Http}
import javax.servlet.http.HttpServletRequest
import org.joda.time.{Period, DateTimeZone, DateTimeConstants, DateTime}
import org.joda.time.format.PeriodFormatterBuilder
import org.fusesource.scalate.util.IOUtil
import unfiltered.filter.Plan
import unfiltered.response.Redirect
import unfiltered.response.ContentType
import unfiltered.response.ResponseString
import unfiltered.scalate.Scalate


object Main {

  def main(args: Array[String]) {

    server(8226).start

  }

  private def server(port: Int): Server = {

    Http(port).context("/public") { ctx =>

      ctx.resources(this.getClass.getResource("/public"))
//      ctx.resources(new java.net.URL("file:///Users/dzhu/work/blah/web/src/main/resources/public"))

      ctx.filter(new org.eclipse.jetty.servlets.GzipFilter())

    }.filter(plan)

  }

  private def plan = {

    new Plan {

      def intent = unfiltered.kit.GZip {

        case req @ Path(Seg("health" :: Nil)) => ResponseString("OK")

        case req @ Path("/") => Redirect("/home")

        case req @ Path(Seg("no-cache" :: path)) => Ok ~> ContentType(mime(path.last)) ~> bytesResponse(path)

        case req @ Path(Seg(page :: Nil)) => jadeResponse(req, page)

      }

    }

  }

  private def jadeResponse(req: HttpRequest[HttpServletRequest], page: String)(implicit engine: TemplateEngine) =
      Ok ~> ContentType("text/html") ~> Scalate(req, page + ".jade", commonPageParams(req) : _*)(engine)

  private def bytesResponse(path: Seq[String]) =
    ResponseBytes(IOUtil.loadBytes(this.getClass.getResourceAsStream("/public" + path.foldLeft("") {_ + "/" + _})))

  private def mime(fileName: String) = fileName match {
    case Mime(name) => name
    case _ => "application/octet-stream"
  }

  private def commonPageParams(r: HttpRequest[HttpServletRequest]): Array[(String, Any)] =
    Array(("msie", isInternetExplorer(r)), ("biancaAge", calculateBiancaAge), ("careerAge", calculateCareerAge))

  private def calculateBiancaAge = {
    val p = new Period(BIANCA_EPOCH, new DateTime(TIME_ZONE))
    if (p.getYears > 0) {
      formatPeriod(p)(_.appendYears.appendSuffix(" year"))
    } else {
      formatPeriod(p)(_.appendMonths.appendSuffix(" month"))
    }
  }

  private def calculateCareerAge = {
    val p = new Period(CAREER_EPOCH, new DateTime(TIME_ZONE))
    val formatter = new PeriodFormatterBuilder().appendYears.appendSuffix(" year", " years").toFormatter
    formatter.print(p)
  }

  private def formatPeriod(p: Period)(fun: PeriodFormatterBuilder => PeriodFormatterBuilder) = {
    val shortFormatter = fun(new PeriodFormatterBuilder()).toFormatter
    shortFormatter.print(p) + " (" + fullPeriodFormatter.print(p) + ")"
  }

  private def fullPeriodFormatter = new PeriodFormatterBuilder()
    .appendYears.appendSuffix(" year", " years")
    .appendSeparator(" and ")
    .appendMonths.appendSuffix(" month", " months")
    .appendSeparator(", ")
    .appendDays.appendSuffix(" day", " days")
    .toFormatter

  private def isInternetExplorer(r: HttpRequest[HttpServletRequest]) =
    r.headers("User-Agent").exists(_.contains("MSIE"))

}
