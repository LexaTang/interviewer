package cn.ac.tcj

import io.vertx.scala.core.Vertx
import interviewer.QueueVerticle

/**
 * @author Alexandra Tang
 */
object App {
  
  def main(args : Array[String]) {
    println( "Hello World!" )

    var vertx = Vertx.vertx()
    vertx.deployVerticle(new QueueVerticle)
  }

}
