package com.davezhu.blah

import org.fusesource.scalate.TemplateEngine
import org.fusesource.scalate.util.{Resource, FileResourceLoader}
import org.fusesource.scalate.layout.DefaultLayoutStrategy
import org.joda.time.{DateTimeConstants, DateTime, DateTimeZone}

package object web {

  val TIME_ZONE = DateTimeZone.forID("Australia/Brisbane")

  val BIANCA_EPOCH = new DateTime(2011, DateTimeConstants.DECEMBER, 1, 15, 30, 0, TIME_ZONE)

  val CAREER_EPOCH = new DateTime(2005, DateTimeConstants.MAY, 1, 9, 0, 0, TIME_ZONE)

  implicit val engine = new TemplateEngine

  engine.resourceLoader = new FileResourceLoader {
    override def resource(uri: String): Option[Resource] = {
      Some(Resource.fromURL(this.getClass.getResource("/templates/" + uri)))
      //Some(Resource.fromFile("/Users/dzhu/work/blah/web/src/main/resources/templates/" + uri))
    }
  }

  engine.layoutStrategy = new DefaultLayoutStrategy(engine, "layouts/default.jade")

}
