package com.davezhu.blah.core

import org.slf4j.Logger

object Logging {

  def info(logger: Logger, msg : => String) {
    if (logger.isInfoEnabled) {
      logger.info(msg)
    }
  }

  def error(logger: Logger, msg: => String, t: Throwable = null) {
    if (logger.isErrorEnabled) {
      if (t == null) {
        logger.error(msg)
      } else {
        logger.error(msg, t)
      }
    }
  }

  def debug(logger: Logger, msg : => String, t: Throwable = null) {
    if (logger.isDebugEnabled) {
      if (t == null) {
        logger.debug(msg)
      } else {
        logger.debug(msg, t)
      }
    }
  }

  def trace(logger: Logger, msg : => String, t: Throwable = null) {
    if (logger.isTraceEnabled) {
      if (t == null) {
        logger.trace(msg)
      } else {
        logger.trace(msg, t)
      }
    }
  }

}
