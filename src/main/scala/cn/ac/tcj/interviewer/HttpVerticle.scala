package cn.ac.tcj.interviewer

import io.vertx.lang.scala.ScalaVerticle
import cn.ac.tcj.vertx.scala.LoggerTrait
import io.vertx.scala.core.Future
import io.vertx.scala.ext.web.Router
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import scala.util.Success
import scala.util.Failure
import io.vertx.scala.core.eventbus.DeliveryOptions

/** Provide http api to communicate with queue and database.
 * 
 *  /api/get Get the status of queue.
 *  /api/enqueue/:id Enqueue a new interviewee.
 *  /api/comment/:id Make a comment and send it to room.
 *  /api/room/:id Get a room's status.
 */
class HttpVerticle extends ScalaVerticle with LoggerTrait{
  override def start(): Unit = {
    val server = vertx.createHttpServer()

    val router = Router.router(vertx);

    router.get("/api/get").handler(routingContext => {
      val response = routingContext.response()
      val getQueue = eb.requestFuture[JsonArray]("queue.get", null)
      val getVip = eb.requestFuture[JsonObject]("queue.getVip", null)
      val content = new JsonObject()
      val getFuture = for {
        queue <- getQueue
        vip <- getVip
      } yield {
        content.put("queue", queue.body)
        content.put("vip", vip.body)
      }
      getFuture onComplete {
        case Failure(t) => {
          logger.error(t)
          response.setStatusCode(500).end()
        }
        case Success(_) => response.end(content.encode)
      }
    })

    router.get("/api/enqueue/:id").handler(routingContext => {
      val request = routingContext.request()
      val response = routingContext.response()
      if (request.getParam("id").isEmpty) {
        response.setStatusCode(400).end()
        logger.warn("Http enqueue request with empty id!")
      } else {
        eb.requestFuture[Object]("queue.enqueue", request.getParam("id").get) onComplete {
          case Failure(t) => {
            logger.error(t)
            response.setStatusCode(500).end(t.getMessage())
          }
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
        val getInterviewing = eb.requestFuture[String](s"room${roomPort}.interviewing", null, DeliveryOptions().setSendTimeout(1000))
        val getNext = eb.requestFuture[String](s"room${roomPort}.next", null, DeliveryOptions().setSendTimeout(1000))
        val content = new JsonObject()
        val resFuture = for {
          interviewing <- getInterviewing
          next <- getNext
        } yield {
          content.put("interviewing", interviewing.body).put("next", next.body)
        }
        resFuture onComplete {
          case Failure(t) => {
            logger.error(t)
            response.setStatusCode(500).end()
          }
          case Success(_) => response.end(content.encode)
        }
      }
    })

    server.requestHandler(router).listen(8080)
  }
  
  lazy val eb = vertx.eventBus
}