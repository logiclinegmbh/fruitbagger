package de.logicline.fruitbagger;

import de.logicline.fruitbagger.domain.FruitUser;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.templ.HandlebarsTemplateEngine;
import org.mongodb.morphia.Datastore;

public class ProfileHandler implements Handler<RoutingContext> {
  private final HandlebarsTemplateEngine engine = HandlebarsTemplateEngine.create();
  private final Datastore datastore;

  public ProfileHandler(Datastore datastore) {
    this.datastore = datastore;
  }

  @Override
  public void handle(RoutingContext ctx) {
    Session session = ctx.session();
    FruitUser user = session.get("fruitUser");

    ctx.put("email", user.getEmail());
    ctx.put("uuid", user.getApiToken());
    // and now delegate to the engine to render it.
    engine.render(ctx, "views", "/advanced.hbs", res3 -> {
      if (res3.succeeded()) {
        ctx.response()
          .putHeader("Content-Type", "text/html")
          .end(res3.result());
      } else {
        ctx.fail(res3.cause());
      }
    });

  }
}
