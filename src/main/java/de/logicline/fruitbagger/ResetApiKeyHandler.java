package de.logicline.fruitbagger;

import de.logicline.fruitbagger.domain.FruitUser;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import org.mongodb.morphia.Datastore;

public class ResetApiKeyHandler implements Handler<RoutingContext> {
  private final Datastore datastore;

  public ResetApiKeyHandler(Datastore datastore) {
    this.datastore = datastore;
  }

  @Override
  public void handle(RoutingContext ctx) {
    Session session = ctx.session();
    FruitUser user = session.get("fruitUser");
    user.resetApiKey();
    datastore.save(user);
    ctx.response().putHeader("location", "/profile").setStatusCode(302).end();
  }
}
