import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.json.*;
import io.vertx.core.Future;

public class MainVerticle extends AbstractVerticle {
  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    var options = new DeploymentOptions().setWorker(true);
    vertx.deployVerticle("scala:cn.ac.tcj.interviewer.QueueVerticle", options, res -> {
      if (res.succeeded()) {
        System.out.println(String.format("Deployment id is: %s", res.result()));
        var interviewer = new JsonArray().add(1).add(2);
        var config = new JsonObject().put("interviewers", interviewer).put("port", 1);
        Future<Void> room1Future = Future.future(promise -> vertx.deployVerticle("scala:cn.ac.tcj.interviewer.HttpRoomVerticle",
            new DeploymentOptions().setConfig(config), id -> promise.complete()));
        interviewer = new JsonArray().add(3).add(4);
        var config2 = new JsonObject().put("interviewers", interviewer).put("port", 2);
        Future<Void> room2Future = Future.future(promise -> vertx.deployVerticle("scala:cn.ac.tcj.interviewer.HttpRoomVerticle",
            new DeploymentOptions().setConfig(config2), id -> promise.complete()));
        Future<Void> httpFuture = Future.future(
            promise -> vertx.deployVerticle("scala:cn.ac.tcj.interviewer.HttpVerticle", id -> promise.complete()));
        var dbConfig = new JsonObject().put("db", new JsonObject());
        Future<Void> dbFuture = Future.future(
            promise -> vertx.deployVerticle("scala:cn.ac.tcj.interviewer.DatabaseVerticle", new DeploymentOptions().setConfig(dbConfig),id -> promise.complete()));
        CompositeFuture.all(room1Future, room2Future, httpFuture, dbFuture).setHandler(all -> startPromise.complete());
      } else
        System.out.println("Deployment failed!");
    });
  }

}