import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class VipTest {
  static Future<Void> enqueueFutureGenerator(String i, EventBus eventBus) {
    return Future.future(promise -> eventBus.request("queue.enqueue", i, reply -> {
                promise.complete();
              }));
  }
  
  @Test
  void vip(Vertx vertx, VertxTestContext testContext) throws Throwable {
    var eventBus = vertx.eventBus();
    var interviewer = new JsonArray().add(1).add(2);
    var config = new JsonObject().put("interviewers", interviewer).put("port", 2);
    vertx.deployVerticle(new MainVerticle(), new DeploymentOptions().setConfig(config),res -> {
      
      var enqueueFuture1 = enqueueFutureGenerator("1500720134", eventBus);
      var enqueueFuture2 = enqueueFutureGenerator("1500720130", eventBus);
      var vipFuture1 = Future.future(promise -> {
        eventBus.request("queue.envip", new JsonObject("{\"id\":\"1900100101\",\"port\":1}"), reply -> promise.complete());
      });
      var vipFuture2 = Future.future(promise -> {
        eventBus.request("queue.envip", new JsonObject("{\"id\":\"1900100102\",\"port\":2}"), reply -> promise.complete());
      });
      CompositeFuture.all(enqueueFuture1, enqueueFuture2, vipFuture1, vipFuture2).setHandler(ar -> {
        assertTrue(ar.succeeded());
        CompositeFuture.all(
          Future.future( promise -> eventBus.request("room1.next", null, replyInt -> {
            assertEquals("1900100101", replyInt.result().body());
            promise.complete();
          })),
          Future.future( promise -> eventBus.request("room2.next", null, replyInt -> {
            assertEquals("1900100102", replyInt.result().body());
            promise.complete();
          }))).setHandler(arGet -> {
            assertTrue(arGet.succeeded());
            testContext.completeNow();
          });
      });
    });

    testContext.awaitCompletion(2, TimeUnit.SECONDS);
  }
}