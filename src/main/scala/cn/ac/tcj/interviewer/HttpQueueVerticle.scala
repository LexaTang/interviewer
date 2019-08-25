package cn.ac.tcj.interviewer

import io.vertx.lang.scala.ScalaVerticle
import cn.ac.tcj.vertx.scala.LoggerTrait
import io.vertx.scala.core.Future
import io.vertx.scala.ext.web.Router

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

      
    })

    server.requestHandler(router).listen(8080)
  }
  
  def eb = vertx.eventBus
}