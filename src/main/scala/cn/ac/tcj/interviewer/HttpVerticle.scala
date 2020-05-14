package cn.ac.tcj.interviewer

import cn.ac.tcj.vertx.scala.LoggerTrait
import io.vertx.core.json.{JsonArray, JsonObject}
import io.vertx.lang.scala.ScalaVerticle
import io.vertx.lang.scala.json.Json
import io.vertx.scala.core.eventbus.{DeliveryOptions, EventBus}
import io.vertx.scala.ext.web.Router

import scala.util.{Failure, Success}

/** Provide http api to communicate with queue and database.
 *
 * GET /api/get Get the status of queue.
 * ANY /api/enqueue/:id Enqueue a new interviewee.
 * ANY /api/vip/:id/room/:port Enqueue a new VIP.
 * PUT /api/room/:port Make a comment and send it to room.
 * GET /api/room/:port Get this room's status.
 * DELETE /api/room/:port Shut this room down .
 */
class HttpVerticle extends ScalaVerticle with LoggerTrait {
  lazy val eb: EventBus = vertx.eventBus

  override def start() {
    val server = vertx.createHttpServer()

    val router = Router.router(vertx)

    router.get("/api/get").handler(routingContext => {
      val response = routingContext.response()
      val getQueue = eb.requestFuture[JsonArray]("queue.get", None)
      val getVip = eb.requestFuture[JsonObject]("queue.getVip", None)
      val content = new JsonObject()
      val getFuture = for {
        queue <- getQueue
        vip <- getVip
      } yield {
        content.put("queue", queue.body)
        content.put("vip", vip.body)
      }
      getFuture onComplete {
        case Failure(t) =>
          logger.error(t)
          response.setStatusCode(500).end()
        case Success(_) => response.end(content.encode)
      }
    })

    router.route("/api/enqueue/:id").handler(routingContext => {
      val request = routingContext.request()
      val response = routingContext.response()
      if (request.getParam("id").isEmpty) {
        response.setStatusCode(400).end()
        logger.warn("Http enqueue request with empty id!")
      } else {
        eb.requestFuture[Object]("queue.enqueue", request.getParam("id")) onComplete {
          case Failure(t) =>
            logger.error(t)
            response.setStatusCode(500).end(t.getMessage)
          case Success(res) => response.end(s"${res.body}")
        }
      }
    })

    router.get("/api/room/:port").handler(routingContext => {
      val response = routingContext.response()
      if (routingContext.request.getParam("port").isEmpty) {
        response.setStatusCode(400).end()
        logger.warn("Http room request with empty id!")
      } else {
        val roomPort = routingContext.request.getParam("port").get
        val getInterviewing = eb.requestFuture[String](s"room$roomPort.interviewing", None, DeliveryOptions().setSendTimeout(1000))
        val getNext = eb.requestFuture[String](s"room$roomPort.next", None, DeliveryOptions().setSendTimeout(1000))
        val content = new JsonObject()
        val resFuture = for {
          interviewing <- getInterviewing
          next <- getNext
        } yield {
          content.put("interviewing", interviewing.body).put("next", next.body)
        }
        resFuture onComplete {
          case Failure(t) =>
            logger.error(t)
            response.setStatusCode(500).end()
          case Success(_) => response.end(content.encode)
        }
      }
    })

    router.route("/api/vip/:id/room/:port").handler(routingContext => {
      val request = routingContext.request()
      val response = routingContext.response()
      if (request.getParam("id").isEmpty && request.getParam("port").isEmpty) {
        response.setStatusCode(400).end()
        logger.warn("Http vip request with empty param!")
      } else {
        val roomPort = request.getParam("port").get
        val intervieweeId = request.getParam("id").get
        eb.requestFuture[String]("queue.enVip", Option(Json.obj(("id", intervieweeId), ("port", roomPort.toInt)))) onComplete {
          case Failure(t) =>
            logger.error(t)
            response.setStatusCode(500).end()
          case Success(reply) => response.end(reply.body)
        }
      }
    })

    router.put("/api/room/:port").handler(routingContext => {
      val request = routingContext.request()
      val response = routingContext.response()
      if (request.getParam("port").isEmpty) {
        response.setStatusCode(400).end()
        logger.warn("Http comment request with empty param!")
      } else {
        val roomPort = request.getParam("port").get
        request.bodyHandler(body => {
          eb.requestFuture[String](s"room$roomPort.comm", Option(new JsonObject(body))) onComplete {
            case Failure(t) =>
              response.setStatusCode(500).end()
              logger.error(t)
            case Success(res) => response.end(res.body)
          }
        })
      }
    })

    router.delete("/api/room/:port").handler(routingContext => {
      val request = routingContext.request()
      val response = routingContext.response()
      if (request.getParam("port").isEmpty) {
        response.setStatusCode(400).end()
        logger.warn("Http shutdown request with empty param!")
      } else {
        val roomPort = request.getParam("port")
        eb.requestFuture[String](s"room${roomPort.get}.shutdown", roomPort) onComplete {
          case Failure(t) =>
            response.setStatusCode(500).end()
            logger.error(t)
          case Success(res) => response.end(res.body)
        }
      }
    })

    server.requestHandler(router).listen(8080)
  }
}
