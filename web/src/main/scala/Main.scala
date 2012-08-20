package com.davezhu.blah.web

import org.springframework.context.annotation.AnnotationConfigApplicationContext


object Main {

  def main(args: Array[String]) {

    MainServer.configure(8226).start

    if (sys.props.get("tickerServer").exists(_ == "on")) {
      println("[INFO] starting ticker server on port 8121...")
      TickerServer.configure(8121).start
    }

  }

}
