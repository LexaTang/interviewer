package interviewer

import interviewer.queue.QueueVerticle
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@DelicateCoroutinesApi
@ExtendWith(VertxExtension::class)
class TestQueueVerticle {

  @BeforeEach
  fun deploy_verticle(vertx: Vertx, testContext: VertxTestContext) {
    vertx.deployVerticle(QueueVerticle(), testContext.succeeding {
      testContext.completeNow()
    })
  }

  @Test
  fun enqueue(vertx: Vertx, testContext: VertxTestContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {
        val result1 = vertx.eventBus().request<Int>("queue.enqueue", "1500720134").await()
        assert(result1.body() == 1)
        val result2 = vertx.eventBus().request<Int>("queue.enqueue", "1500720135").await()
        assert(result2.body() == 2)
        // Duplicate test.
        assertThrows<Throwable> { vertx.eventBus().request<Int>("queue.enqueue", "1500720135").await() }
      } catch (e: Throwable) {
        testContext.failNow(e)
      }

      testContext.completeNow()
    }
  }

  @Test
  fun where(vertx: Vertx, testContext: VertxTestContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {
        val result1 = vertx.eventBus().request<Int>("queue.enqueue", "1500720134").await()
        assert(result1.body() == 1)
        val result2 = vertx.eventBus().request<Int>("queue.enqueue", "1500720135").await()
        assert(result2.body() == 2)
        // Duplicate test.
        assertThrows<Throwable> { vertx.eventBus().request<Int>("queue.enqueue", "1500720134").await() }
        assert(vertx.eventBus().request<Int>("queue.where", "1500720134").await().body() == 0)
        assert(vertx.eventBus().request<Int>("queue.where", "1500720135").await().body() == 1)
      } catch (e: Throwable) {
        testContext.failNow(e)
      }

      testContext.completeNow()
    }
  }

  @Test
  fun dequeue(vertx: Vertx, testContext: VertxTestContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      try {
        val result1 = vertx.eventBus().request<Int>("queue.enqueue", "1500720134").await()
        assert(result1.body() == 1)
        val result2 = vertx.eventBus().request<Int>("queue.enqueue", "1500720135").await()
        assert(result2.body() == 2)
        val dequeue1 = vertx.eventBus().request<String>("queue.dequeue", "").await()
        assert(dequeue1.body() == "1500720134")
        val where1 = vertx.eventBus().request<Int>("queue.where", "1500720135").await()
        assert(where1.body() == 0)
        val result3 = vertx.eventBus().request<Int>("queue.enqueue", "1500720136").await()
        assert(result3.body() == 2)
        val where2 = vertx.eventBus().request<Int>("queue.where", "1500720136").await()
        assert(where2.body() == 1)
        val dequeue2 = vertx.eventBus().request<String>("queue.dequeue", "").await()
        assert(dequeue2.body() == "1500720135")
        val where3 = vertx.eventBus().request<Int>("queue.where", "1500720136").await()
        assert(where3.body() == 0)
        val dequeue3 = vertx.eventBus().request<String>("queue.dequeue", "").await()
        assert(dequeue3.body() == "1500720136")
        assertThrows<Throwable> { vertx.eventBus().request<String>("queue.dequeue", "").await() }
      } catch (e: Throwable) {
        testContext.failNow(e)
      }

      testContext.completeNow()
    }
  }

  @Test
  fun benchmark(vertx: Vertx, testContext: VertxTestContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      for (i in 0..1000) vertx.eventBus().request<Int>("queue.enqueue", "$i")
      testContext.completeNow()
    }
  }

  @AfterEach
  fun clear(vertx: Vertx, testContext: VertxTestContext) {
    GlobalScope.launch(vertx.dispatcher()) {
      println(vertx.eventBus().request<JsonObject>("queue.dump", "").await().body())
      try {
          while (true) vertx.eventBus().request<String>("queue.dequeue", "").await()
      } catch (e: Throwable) {
        testContext.completeNow()
      }
    }
  }
}
