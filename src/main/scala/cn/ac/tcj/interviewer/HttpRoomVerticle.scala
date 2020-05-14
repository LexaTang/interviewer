package cn.ac.tcj.interviewer

import cn.ac.tcj.vertx.scala.LoggerTrait
import io.vertx.core.json.JsonObject
import io.vertx.lang.scala.ScalaVerticle
import io.vertx.lang.scala.json.Json
import io.vertx.scala.core.eventbus.{EventBus, Message, MessageConsumer}

import scala.collection.mutable
import scala.collection.mutable.{Map, TreeMap}
import scala.concurrent.Future
import scala.util.{Failure, Success}

class HttpRoomVerticle extends ScalaVerticle with RoomVerticle with LoggerTrait {
  lazy val eb: EventBus = vertx.eventBus()
  var interviewers: Array[Int] = Array()

  var interviewing = ""
  var nextinterview = ""

  override def start() {
    val jsonInterviewers = config.getJsonArray("interviewers")
    port = config.getInteger("port")
    val buffer = new mutable.ArrayBuffer[Int]
    for (i <- 0 until jsonInterviewers.size) buffer += jsonInterviewers.getInteger(i)
    interviewers = buffer.toArray
    enqueueConsumer = Some(enqueueConsumerGenerator)
    eb.consumer(s"room$port.enqueue", enqueueHandler)
    commentIn(eb.consumer(s"room$port.comm"))
    eb.consumer(s"room$port.dequeue").handler((_: Message[Object]) => dequeue())
    eb.consumer(s"room$port.interviewing").handler((message: Message[Any]) => {
      message.reply(interviewing)
    })
    eb.consumer(s"room$port.next").handler((message: Message[Any]) => {
      message.reply(nextinterview)
    })
    eb.consumer(s"room$port.vip").handler((message: Message[String]) => {
      enqueueConsumerUnregister()
      if (interviewing.isEmpty) interviewing = message.body
      else if (nextinterview.isEmpty) nextinterview = message.body
      else logger.error("Room is not empty but send vip!")
    })
    eb.consumer(s"room$port.shutdown").handler((message: Message[String]) => {
      shutdown = true
      message.reply("shutdown")
    })
  }

  var enqueueConsumer: Option[MessageConsumer[String]] = None

  def dequeue() {
    if (nextinterview.nonEmpty) {
      interviewing = nextinterview
      nextinterview = ""
    }
    if (shutdown) return
    dequeueFuture onComplete {
      case Failure(_) =>
        logger.info("Room request for dequeue but gets nothing.")
        if (enqueueConsumer.isEmpty) enqueueConsumer = Some(enqueueConsumerGenerator)
      case Success(message) =>
        if (interviewing.isEmpty) interviewing = message.body
        else if (nextinterview.isEmpty) {
          nextinterview = message.body
          message.reply(port)
          enqueueConsumerUnregister()
        } else {
          logger.error(s"Room is busy but dequeue still was called! ${message.body}")
        }
    }
  }

  def enqueueConsumerGenerator: MessageConsumer[String] =
    eb.consumer("room.enqueue", enqueueHandler)

  def enqueueHandler(message: Message[String]) {
    if (interviewing.isEmpty) {
      interviewing = message.body
      message.reply(port)
    }
    else if (nextinterview.isEmpty) {
      nextinterview = message.body
      enqueueConsumerUnregister()
      message.reply(port)
    } else {
      message.fail(0, "Room is busy!")
      logger.error("Room is busy but enqueueConsumer have not unregistered!")
    }
  }

  def enqueueConsumerUnregister() {
    if (enqueueConsumer.nonEmpty) enqueueConsumer.get.unregisterFuture onComplete {
      case Failure(_) => logger.error("Unexpect unregister error!")
      case Success(_) => enqueueConsumer = None
    }
  }

  var comments: Map[Int, String] = new TreeMap[Int, String]

  def dequeueFuture: Future[Message[String]] = {
    eb.requestFuture("queue.dequeue", Option(port))
  }

  var port = 0

  def commentIn(consumer: MessageConsumer[JsonObject]) {
    consumer.handler(message => {
      comments.put(message.body.getInteger("interviewer"), message.body.getString("comment"))
      if (interviewers.forall(comments contains _)) {
        val jsonComm = Json.emptyObj
        comments.foreach(i => jsonComm.put(s"${i._1}", i._2))
        comments.clear()
        eb.requestFuture[Object]("db.comm", Option(Json.obj(("id", interviewing), ("comm", jsonComm)))) onComplete {
          case Failure(t) =>
            logger.error(t)
            message.fail(0, "db error")
          case Success(_) => message.reply("saved")
        }
        dequeue()
      } else message.reply("cached")
    })
  }
}
