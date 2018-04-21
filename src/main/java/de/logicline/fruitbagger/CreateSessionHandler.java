package de.logicline.fruitbagger;

import de.logicline.fruitbagger.domain.FruitUser;
import de.logicline.fruitbagger.domain.Session;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.mongodb.morphia.Datastore;

import java.util.OptionalInt;

public class CreateSessionHandler implements Handler<RoutingContext> {
  private final Datastore datastore;

  public CreateSessionHandler(Datastore datastore) {
    this.datastore = datastore;
  }

  @Override
  public void handle(RoutingContext ctx) {
    FruitUser user = ctx.session().get("fruitUser");
    OptionalInt max = datastore.find(Session.class).field("user").equal(user).asList().stream()
      .mapToInt(s -> s.getNumber() == null ? 0 : s.getNumber())
      .max();
    Session newSession = Session.create(user, max.orElse(0) + 1);
    datastore.save(newSession);
    ctx.response().end(newSession.getNumber().toString());
  }
}
