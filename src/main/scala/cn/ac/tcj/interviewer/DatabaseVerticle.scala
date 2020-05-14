package cn.ac.tcj.interviewer

import cn.ac.tcj.vertx.scala.LoggerTrait
import io.vertx.core.json.JsonObject
import io.vertx.lang.scala.ScalaVerticle
import io.vertx.scala.core.eventbus.{EventBus, Message}
import io.vertx.scala.ext.mongo.MongoClient

import scala.util.{Failure, Success}

class DatabaseVerticle extends ScalaVerticle with LoggerTrait {
  lazy val client: MongoClient = MongoClient.createNonShared(vertx, config.getJsonObject("db"))
  lazy val eb: EventBus = vertx.eventBus

  override def start() {
    eb.consumer("db.comm").handler((message: Message[JsonObject]) => {
      client.saveFuture("comments", message.body) onComplete {
        case Failure(t) =>
          message.fail(0, t.getMessage)
          logger.error(t)
        case Success(res) =>
          message.reply(res)
          logger.info(message.body)
      }
    })
  }
}
