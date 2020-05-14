package cn.ac.tcj.interviewer

import cn.ac.tcj.vertx.scala.LoggerTrait
import io.vertx.core.json.JsonObject
import io.vertx.lang.scala.ScalaVerticle
import io.vertx.lang.scala.json.Json
import io.vertx.scala.core.eventbus.{DeliveryOptions, EventBus, MessageConsumer}

import scala.collection.mutable.{ListBuffer, TreeMap}
import scala.util.{Failure, Success}

/** Implement of Queue. */
trait QueueTrait {
  val queue = new ListBuffer[String]
  val vipQueue = new TreeMap[String, Int]
}

trait HttpQueueTrait {
  def annoDequeue(interviewee: String, room: Int) {
    eb.send("http.dequeue", Option(Json.obj(("interviewee", interviewee), ("room", room))))
  }

  val eb: EventBus
}

/** Control how should interview queued.
 *
 * This Verticle initialize the queue and interview rooms.
 * Control how should interview queued.
 * Provide API to control the queue.
 * Consumers: enqueue dequeue get getVip
 */
class QueueVerticle extends ScalaVerticle with QueueTrait with HttpQueueTrait with LoggerTrait {
  lazy val eb: EventBus = vertx.eventBus

  /** Initialize the queue and interview rooms. */
  override def start(): Unit = {
    logger.info("Queue service is starting.")
    vipEnqueue(eb.consumer("queue.enVip"))
    enqueue(eb.consumer("queue.enqueue"))
    dequeue(eb.consumer("queue.dequeue"))
    get(eb.consumer("queue.get"))
    getVip(eb.consumer("queue.getVip"))
  }

  /** Consumer accept a send of interviewee */
  def enqueue(consumer: MessageConsumer[String]): MessageConsumer[String] = {
    consumer.handler(message => {
      val id = message.body()
      //Check if any room can accept this interviewee.
      eb.requestFuture[Integer]("room.enqueue", Option(id), DeliveryOptions().setSendTimeout(1000)) onComplete {
        case Failure(t) =>
          logger.info(s"Rooms busy, fallback interviewee $id into queue: $t")
          queue += id
          message.reply("enqueued")
        case Success(reply) =>
          logger.info(s"Send interviewee $id into ${reply.body}")
          annoDequeue(id, reply.body)
          message.reply(reply.body)
      }
    })
  }

  def vipEnqueue(consumer: MessageConsumer[JsonObject]): MessageConsumer[JsonObject] = {
    consumer.handler(message => {
      val id = message.body.getString("id")
      val port = message.body.getInteger("port")
      eb.requestFuture[String](s"room$port.next", Option(id), DeliveryOptions().setSendTimeout(1000)) onComplete {
        case Failure(t) =>
          logger.info(s"Rooms busy, fallback interviewee $id into queue: $t")
          eb.send("queue.enqueue", Option(id))
          message.reply("fallback")
        case Success(m) =>
          val next = m.body
          if (next.isEmpty) {
            eb.send(s"room$port.enqueue", Option(id))
            logger.info(s"Send vip $id into $port")
            message.reply("sent")
          } else {
            vipQueue += ((id, port))
            message.reply("success")
          }
      }
    })
  }

  def dequeue(consumer: MessageConsumer[Integer]): MessageConsumer[Integer] = {
    consumer.handler(message => {
      logger.info(s"Room ${message.body} require to dequeue.")
      if (vipQueue.exists(_._2 == message.body)) {
        val id = vipQueue.find(_._2 == message.body).get._1
        message.reply(id)
        annoDequeue(id, message.body)
        logger.info(s"Send VIP $id into ${message.body}")
        vipQueue.remove(id)
      }
      else if (queue.isEmpty) {
        logger.info("Dequeue failed, queue is empty.")
        message.fail(0, "Queue is empty.")
      }
      else {
        val id = queue.remove(0)
        message.replyFuture[String](Option(id)) onComplete {
          case Failure(_) =>
            logger.warn(s"Dequeue failed, fallback interviewee $id into queue.")
            id +=: queue
          case Success(_) =>
            logger.info(s"Send interviewee $id into ${message.body}")
            annoDequeue(id, message.body)
        }
      }
    })
  }

  def get(consumer: MessageConsumer[String]) {
    consumer.handler(message => {
      val arrQueue = Json.emptyArr
      queue.foreach(arrQueue.add)
      message.reply(arrQueue)
    })
  }

  def getVip(consumer: MessageConsumer[String]) {
    consumer.handler(message => {
      val jsonVips = Json.emptyArr
      vipQueue.foreach(vip => jsonVips.add(Json.emptyObj.put(vip._1, vip._2)))
      message.reply(jsonVips)
    })
  }

  /** Make sure queue is empty. If not, extract the queue to logger. */
  override def stop(): Unit = {
    logger.info("Stopping the queue service...")
    if (queue.nonEmpty) logger.warn(s"Queue is not empty! ${queue.toString}")
    logger.info("Queue service stop.")
  }
}
