package cn.ac.tcj.interviewer;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.Promise;
import io.vertx.core.json.*;

public class MainVerticle extends AbstractVerticle {
  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    var options = new DeploymentOptions().setWorker(true);
    vertx.deployVerticle("scala:cn.ac.tcj.interviewer.QueueVerticle", options, res -> {
      if (res.succeeded()) {
        System.out.println(String.format("Deployment id is: %s", res.result()));
      var interviewer = new JsonArray().add(1).add(2);
      var config = new JsonObject().put("interviewers", interviewer).put("port", 1);
      vertx.deployVerticle("scala:cn.ac.tcj.interviewer.HttpRoomVerticle", new DeploymentOptions().setConfig(config));
      interviewer = new JsonArray().add(3).add(4);
      config = new JsonObject().put("interviewers", interviewer).put("port", 2);
      vertx.deployVerticle("scala:cn.ac.tcj.interviewer.HttpRoomVerticle", new DeploymentOptions().setConfig(config));
      vertx.deployVerticle("scala:cn.ac.tcj.interviewer.HttpVerticle");
      }
      else System.out.println("Deployment failed!");
    });
    startPromise.complete();
  }

}