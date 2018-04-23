package de.logicline.fruitbagger;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class FailureHandler implements Handler<RoutingContext> {
    @Override
    public void handle(RoutingContext ctx) {
        switch (ctx.statusCode()) {
            case 204:
                ctx.response().setStatusCode(204);
                ctx.response().end();
                break;
            default:
                Throwable t = ctx.failure();
                ctx.response().setStatusCode(400);
                ctx.response().end(t.getMessage() == null ? t.getClass().toString() : t.getMessage());
                break;
        }
    }
}
