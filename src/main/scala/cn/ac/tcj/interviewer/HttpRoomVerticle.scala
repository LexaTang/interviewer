package cn.ac.tcj.interviewer

import io.vertx.lang.scala.ScalaVerticle
import io.vertx.scala.core.eventbus.{ Message, MessageConsumer, DeliveryOptions }
import io.vertx.lang.scala.json.Json
import io.vertx.core.json.JsonObject

import scala.concurrent.Future
import scala.collection.mutable.{ Map, TreeMap }
import scala.collection.mutable
import scala.util.Failure
import scala.util.Success
import cn.ac.tcj.vertx.scala.LoggerTrait

class HttpRoomVerticle extends ScalaVerticle with RoomVerticle with LoggerTrait {
  override def start() {
    val jsonInterviewers = config.getJsonArray("interviewers")
    port = config.getInteger("port")
    val buffer = new mutable.ArrayBuffer[Int]
    for (i <- 0 until jsonInterviewers.size) buffer += jsonInterviewers.getInteger(i)
    interviewers = buffer.toArray
    enqueueConsumer = Some(enqueueConsumerGenerator)
    eb.consumer(s"room${port}.enqueue", enqueueHandler)
    commentIn(eb.consumer(s"room${port}.comm"))
    eb.consumer(s"room${port}.dequeue").handler((message: Message[Object]) => dequeue)
    eb.consumer(s"room${port}.interviewing").handler((message: Message[Object]) => {
      message.reply(interviewing)
    })
    eb.consumer(s"room${port}.next").handler((message: Message[Object]) => {
      message.reply(nextinterview)
    })
    eb.consumer(s"room${port}.vip").handler((message: Message[String]) => {
      enqueueConsumerUnregister
      if (interviewing.isEmpty) interviewing = message.body
      else if (nextinterview.isEmpty) nextinterview = message.body
      else logger.error("Room is not empty but send vip!")
    })
  }

  def dequeue {
    if (nextinterview.nonEmpty) {
      interviewing = nextinterview
      return
    }
    if (shutdown) return
    dequeueFuture onComplete {
      case Failure(t) => {
        logger.info("Room request for dequeue but gets nothing.")
        if (enqueueConsumer.isEmpty) enqueueConsumer = Some(enqueueConsumerGenerator)
      }
      case Success(message) => {
        if (interviewing.isEmpty) interviewing = message.body
        else if (nextinterview.isEmpty) {
          nextinterview = message.body
          enqueueConsumerUnregister
        } else {
          logger.error(s"Room is busy but dequeue still was called! ${message.body}")
        }
      }
    }
  }

  var interviewing = ""
  var nextinterview = ""
  var interviewers = Array()
  var enqueueConsumer: Option[MessageConsumer[String]] = None

  def enqueueConsumerGenerator: MessageConsumer[String] = 
    eb.consumer("room.enqueue", enqueueHandler)
  
  def enqueueHandler(message: Message[String]) {
    if (interviewing.isEmpty) {
      interviewing = message.body
      message.reply(port)
    }
    else if (nextinterview.isEmpty) {
      nextinterview = message.body
      enqueueConsumerUnregister
      message.reply(port)
    } else {
      message.fail(0, "Room is busy!")
      logger.error("Room is busy but enqueueConsumer have not unregistered!")
    }
  }

  def enqueueConsumerUnregister {
    if (enqueueConsumer.nonEmpty) enqueueConsumer.get.unregisterFuture onComplete {
      case Failure(t) => logger.error("Unexcept unregister error!")
      case Success(_) => enqueueConsumer = None
    }
  }

  def dequeueFuture = {
    eb.requestFuture("queue.dequeue", s"${port}")
  }
  
  var comments: Map[Int, String] = new TreeMap[Int, String]

  def commentIn(consumer: MessageConsumer[JsonObject]) {
    consumer.handler(message => {
      comments.put(message.body.getInteger("interviewer"), message.body.getString("comment"))
      if (interviewers.forall(comments contains _)) {
        val jsonComm = Json.emptyObj
        comments.foreach(i => jsonComm.put(s"${i._1}",i._2))
        eb.send("db.comm", jsonComm)
        if (nextinterview.nonEmpty) {
          interviewing = nextinterview
          nextinterview = ""
        } else dequeue
      }
    })
  }

  var port = 0

  lazy val eb = vertx.eventBus()
}