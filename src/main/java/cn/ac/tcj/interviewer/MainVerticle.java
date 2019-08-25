package cn.ac.tcj.interviewer;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

public class MainVerticle extends AbstractVerticle {
  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    var options = new DeploymentOptions().setWorker(true);
    vertx.deployVerticle("scala:cn.ac.tcj.interviewer.QueueVerticle", options, res -> {
      if (res.succeeded()) System.out.println(String.format("Deployment id is: %s", res.result()));
      else System.out.println("Deployment failed!");
    });
    startPromise.complete();
  }

}