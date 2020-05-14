import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@ExtendWith(VertxExtension.class)
class QueueTest {
  static String queueID;

  @Test
  @BeforeAll
  static void start_queue(Vertx vertx, VertxTestContext testContext, TestReporter testReporter) throws Throwable {
    vertx.deployVerticle("scala:cn.ac.tcj.interviewer.QueueVerticle", res -> {
      if (res.failed()) testContext.failNow(res.cause());
      assumeTrue(res.succeeded(), "True");
      testReporter.publishEntry("Deployment id is: " + res.result());
      queueID = res.result();
      testContext.completeNow();
    });

    testContext.awaitCompletion(2, TimeUnit.SECONDS);
  }

  @Test
  void deployed() {
    assertNotNull(queueID);
  }

  @Test
  void enqueue(Vertx vertx, VertxTestContext testContext) throws Throwable {
    var eventBus = vertx.eventBus();
    eventBus.request("queue.enqueue", "1500720134", replyEnqueue -> {
      eventBus.request("queue.get", null, (Handler<AsyncResult<Message<JsonArray>>>) replyGet -> {
        assertTrue(replyGet.succeeded());
        assertEquals("1500720134", replyGet.result().body().getString(0));
        eventBus.request("queue.dequeue", 1,  replyDequeue -> {
          assertTrue(replyDequeue.succeeded());
          assertEquals("1500720134", replyDequeue.result().body());
          replyDequeue.result().reply(1);
          testContext.completeNow();
        });
      });
    });

    testContext.awaitCompletion(2, TimeUnit.SECONDS);
  }

  static Future<Void> enqueueFutureGenerator(String i, EventBus eventBus) {
    return Future.future(promise -> eventBus.request("queue.enqueue", i, reply -> {
                promise.complete();
              }));
  }

  @Test
  void dequeue(Vertx vertx, VertxTestContext testContext) throws Throwable {
    var replyReceived = testContext.checkpoint(3);

    var eventBus = vertx.eventBus();
    var interviewer = new JsonArray().add(1).add(2);
    var config = new JsonObject().put("interviewers", interviewer).put("port", 2);
    vertx.deployVerticle("scala:cn.ac.tcj.interviewer.HttpRoomVerticle", new DeploymentOptions().setConfig(config),res -> {

      var enqueueFuture1 = enqueueFutureGenerator("1500720134", eventBus);
      var enqueueFuture2 = enqueueFutureGenerator("1500720130", eventBus);
      var enqueueFuture3 = enqueueFutureGenerator("1500720132", eventBus);
      CompositeFuture.all(enqueueFuture1, enqueueFuture2, enqueueFuture3).onComplete(ar -> {
        assertTrue(ar.succeeded());
        CompositeFuture.all(
          Future.future(promise -> eventBus.request("room2.interviewing", null, replyInt -> {
            replyReceived.flag();
            promise.complete();
          })),
          Future.future(promise -> eventBus.request("room2.next", null, replyInt -> {
            replyReceived.flag();
            promise.complete();
          }))).onComplete(arGet -> {
          assertTrue(arGet.succeeded());
          eventBus.request("queue.dequeue", 1, replyDequeue -> {
              replyReceived.flag();
            }
          );
        });
      });
    });

    testContext.awaitCompletion(2, TimeUnit.SECONDS);
  }

}
