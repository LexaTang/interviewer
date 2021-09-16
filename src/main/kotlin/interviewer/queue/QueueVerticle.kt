package interviewer.queue

import interviewer.common.LoggerVerticle
import io.vertx.core.impl.logging.Logger
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle

class QueueVerticle: CoroutineVerticle(), LoggerVerticle{
  override val logger: Logger by lazy { LoggerFactory.getLogger(this.javaClass.name) }

  private val prefix by lazy {
    val name = config.getString("name") ?: "queue"
    logger.info("Queue named $name started")
    name
  }

  private var top = 0
  private val linkedHashMap by lazy {
    logger.info("Creating Queue LinkedHashMap.")
    LinkedHashMap<String, Int>(config.getInteger("initialCapacity") ?: 15)
  }

  // 加入队列并返回当前队列容量
  private fun enqueue(id: String): Int {
    linkedHashMap[id] = top + linkedHashMap.size
    return linkedHashMap.size
  }
  private val enqueueConsumer by lazy {
    vertx.eventBus().consumer<String>("${prefix}.enqueue").handler {
      val body = it.body()
      logger.info("$body enqueue $prefix.")
      if (linkedHashMap.contains(body)) it.fail(0, "Duplicate insert in $prefix.")
      else it.reply(enqueue(body))
    }
  }

  private fun where(id: String): Int? {
    val site = linkedHashMap[id]
    return if (site == null) null
    else site - top
  }
  private val whereConsumer by lazy {
    vertx.eventBus().consumer<String>("$prefix.where") {
      val body = it.body()
      logger.info("Ask $body place in $prefix.")
      val place = where(body)
      if (place == null) it.fail(0, "Ask place of $body not in queue.")
      else it.reply(place)
    }
  }

  private fun dequeue(): String? {
    val iter = linkedHashMap.iterator()
    if (!iter.hasNext()) return null
    top++
    val key = iter.next().key
    linkedHashMap.remove(key)
    return key
  }
  private val dequeueConsumer by lazy {
    vertx.eventBus().consumer<String>("$prefix.dequeue") {
      logger.info("Dequeue $prefix.")
      val id = dequeue()
      if (id == null) it.fail(0, "Dequeue empty queue $prefix.")
      else it.reply(id)
    }
  }

  private fun dump(): JsonObject {
    return JsonObject(linkedHashMap.toMap())
  }
  private val dumpConsumer by lazy {
    vertx.eventBus().consumer<String>("$prefix.dump") {
      logger.info("Dump $prefix.")
      it.reply(dump())
    }
  }

  override suspend fun start() {
    logger.info("Listening ${prefix}.enqueue")
    enqueueConsumer
    whereConsumer
    dequeueConsumer
    dumpConsumer
  }

  override suspend fun stop() {
    if (linkedHashMap.size > 0) {
      logger.error("Queue $prefix stopped with non-empty map: $linkedHashMap")
    } else logger.info("Queue $prefix stopped.")
  }
}
