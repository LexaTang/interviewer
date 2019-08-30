package cn.ac.tcj.interviewer

import io.vertx.lang.scala.ScalaVerticle
import cn.ac.tcj.vertx.scala.LoggerTrait
import io.vertx.scala.ext.mongo.MongoClient
import io.vertx.scala.core.eventbus.Message
import io.vertx.core.json.JsonObject
import scala.util.Failure
import scala.util.Success

class DatabaseVerticle extends ScalaVerticle with LoggerTrait {
  override def start() {
    eb.consumer("db.comm").handler((message: Message[JsonObject]) => {
      client.saveFuture("comments", message.body) onComplete {
        case Failure(t) => {
          message.fail(0, t.getMessage)
          logger.error(t)
        }
        case Success(res) => {
          message.reply(res)
          logger.info(message.body)
        }
      }
    })
  }

  lazy val client = MongoClient.createNonShared(vertx, config.getJsonObject("db"))

  lazy val eb = vertx.eventBus
}