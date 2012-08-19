package com.davezhu.blah.web

import org.springframework.context.annotation.AnnotationConfigApplicationContext


object Main {

  def main(args: Array[String]) {

    MainServer.configure(8226).start

    TickerServer.configure(8121).start

  }

}
