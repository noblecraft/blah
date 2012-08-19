package com.davezhu.blah.web

import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.core.io.{ClassPathResource, FileSystemResource, Resource}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment


object ModuleConfig {

  @Bean def properties: PropertySourcesPlaceholderConfigurer = {
    println("properties...")
    val c = new PropertySourcesPlaceholderConfigurer
    c.setLocations(Array(configResource))
    c.setIgnoreUnresolvablePlaceholders(false)
    c
  }

  def configResource: Resource = {
    sys.props.get("appConfig").map(new FileSystemResource(_)).getOrElse(new ClassPathResource("classpath:app.properties"))
  }

}

@Configuration
class ModuleConfig {

  @Autowired
  protected var env: Environment = null

}
