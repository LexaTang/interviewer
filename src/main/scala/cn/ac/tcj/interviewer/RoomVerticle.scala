package cn.ac.tcj.interviewer

import io.vertx.lang.scala.ScalaVerticle
import io.vertx.scala.core.eventbus.{ Message, MessageConsumer, DeliveryOptions }
import io.vertx.core.json.JsonObject

import scala.concurrent.Future
import scala.collection.mutable.Map

/** RoomVerticle trait.
 *  
 *  Should receive options of interviewer.
 *  Implement consumer of room.enqueue and requester of queue.dequeue, send db.comment
 *  A way to notice interviewer the new interviewee.
 */
trait RoomVerticle {
  var interviewing: String
  var nextinterview: String
  var interviewers: Array[Int]
  var enqueueConsumer: Option[MessageConsumer[String]]
  def dequeueFuture: Future[Message[String]]
  var comments: Map[Int, String]
  var shutdown: Boolean = false
  var port: Integer
}