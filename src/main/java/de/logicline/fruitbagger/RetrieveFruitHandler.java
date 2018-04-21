package de.logicline.fruitbagger;

import de.logicline.fruitbagger.domain.FruitBag;
import de.logicline.fruitbagger.domain.FruitQueue;
import de.logicline.fruitbagger.domain.FruitUser;
import de.logicline.fruitbagger.domain.Session;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.mongodb.morphia.Datastore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RetrieveFruitHandler implements Handler<RoutingContext> {
  private final Datastore datastore;

  public RetrieveFruitHandler(Datastore datastore) {
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

    List<FruitBag> allBags = datastore.find(FruitBag.class).field("session").equal(fruitSession).asList();
    int totalSessionFruits = allBags.stream()
      .filter(bag -> bag.getFruits() != null && !bag.getFruits().isEmpty())
      .mapToInt(bag -> bag.getFruits().size())
      .sum();
    Integer currentIndex = fruitSession.getFruitIndex();

    if((currentIndex - totalSessionFruits ) >= fruitSession.getLookAhead()) {
      ctx.fail(new Exception("You have reached the lookahead. Put some fruits in a bag, fruitbagger!"));
      return;
    }

    Integer weight = FruitQueue.QUEUE[currentIndex];
    fruitSession.incrementIndex();
    datastore.save(fruitSession);
    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put(String.valueOf(currentIndex), weight);

    ctx.response().end(JsonObject.mapFrom(responseMap).encodePrettily());
  }

}
