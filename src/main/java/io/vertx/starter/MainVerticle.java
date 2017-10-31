package io.vertx.starter;

import io.vertx.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

  Logger log = LoggerFactory.getLogger(this.getClass());

  @Override
  public void start() {
    vertx.createHttpServer()
        .requestHandler(req -> {
          log.debug("request host: " + req.host());
          req.response().end("Hello Vert.x!");
        })
        .listen(8080);
  }

}
