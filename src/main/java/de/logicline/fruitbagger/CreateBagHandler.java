package de.logicline.fruitbagger;

import de.logicline.fruitbagger.domain.FruitBag;
import de.logicline.fruitbagger.domain.FruitUser;
import de.logicline.fruitbagger.domain.Session;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.mongodb.morphia.Datastore;

import java.util.List;
import java.util.OptionalInt;

public class CreateBagHandler implements Handler<RoutingContext> {
  private final Datastore datastore;

  public CreateBagHandler(Datastore datastore) {
    this.datastore = datastore;
  }

  @Override
  public void handle(RoutingContext ctx) {
    String sessionId = ctx.request().getParam("sessionId");
    FruitUser fruitUser = ctx.session().get("fruitUser");

    List<Session> fruitSessions = datastore.find(Session.class).field("user").equal(fruitUser).field("number").equal(Integer.valueOf(sessionId)).asList();

    if (fruitSessions.isEmpty()) {
      ctx.fail(new Exception("Session not found. Create a new one."));
      return;
    }

    Session fruitSession = fruitSessions.get(0);

    if(fruitSession.getFinishDate() != null){
      ctx.fail(new Exception("Session " + sessionId + " already closed!"));
      return;
    }

    // any open bags?
    List<FruitBag> openBags = datastore.find(FruitBag.class).field("session").equal(fruitSession).field("finishDate").equal(null).asList();
    if (!openBags.isEmpty()) {
      ctx.fail(new Exception("You have unfinished fruit bags left. Don't try to fool me! Your open bag: " + openBags.get(0).getNumber()));
      return;
    }


    OptionalInt max = datastore.find(FruitBag.class).field("session").equal(fruitSession).asList().stream()
      .mapToInt(s -> s.getNumber() == null ? 0 : s.getNumber())
      .max();
    FruitBag newBag = FruitBag.create(fruitSession, max.orElse(0) + 1);
    datastore.save(newBag);
    ctx.response().end(newBag.getNumber().toString());
  }
}
