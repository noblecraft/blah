package com.davezhu.blah.web

import org.springframework.context.annotation.AnnotationConfigApplicationContext


object AppContext {

  val context = new AnnotationConfigApplicationContext(classOf[ModuleConfig])

  context.scan("com.davezhu.core")

  def getBean[T](clazz: Class[T]) = context.getBean(clazz)

}
