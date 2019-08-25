package cn.ac.tcj.vertx.scala

import io.vertx.core.spi.logging.LogDelegate

trait LoggerTrait {
  val logger = new Logger(io.vertx.core.logging.LoggerFactory.getLogger(s"scala:${this.getClass.getName}"))
}

/** Encapsulation of io.vertx.core.logging.Logger to avoid scala ambiguous reference*/
class Logger(logger: io.vertx.core.logging.Logger) {
  def debug(message: Object) =
    ((l: {def debug(m: Object): Unit}) => l.debug(message))(logger)

  def debug(message: Object, t: Throwable) =
    ((l: {def debug(m: Object, t: Throwable): Unit}) => l.debug(message, t))(logger)

  def error(message: Object) =
    ((l: {def error(m: Object): Unit}) => l.error(message))(logger)

  def error(message: Object, t: Throwable) =
    ((l: {def error(m: Object, t: Throwable): Unit}) => l.error(message, t))(logger)

  def fatal(message: Object) = logger.fatal(message)
  def fatal(message: Object, t: Throwable) = logger.fatal(message, t)

  def info(message: Object) =
    ((l: {def info(m: Object): Unit}) => l.info(message))(logger)

  def info(message: Object, t: Throwable) =
    ((l: {def info(m: Object, t: Throwable): Unit}) => l.info(message, t))(logger)

  def trace(message: Object) =
    ((l: {def trace(m: Object): Unit}) => l.trace(message))(logger)

  def trace(message: Object, t: Throwable) =
    ((l: {def trace(m: Object, t: Throwable): Unit}) => l.trace(message, t))(logger)

  def warn(message: Object) =
    ((l: {def warn(m: Object): Unit}) => l.warn(message))(logger)

  def warn(message: Object, t: Throwable) =
    ((l: {def warn(m: Object, t: Throwable): Unit}) => l.warn(message, t))(logger)
  
  def getDelegate = logger.getDelegate
  def isDebugEnabled = logger.isDebugEnabled
  def isInfoEnabled = logger.isInfoEnabled
  def isTraceEnabled = logger.isTraceEnabled
  def isWarnEnabled = logger.isWarnEnabled
}