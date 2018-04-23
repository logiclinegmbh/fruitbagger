package de.logicline.fruitbagger;

import de.logicline.fruitbagger.domain.FruitBag;
import de.logicline.fruitbagger.domain.FruitQueue;
import de.logicline.fruitbagger.domain.FruitUser;
import de.logicline.fruitbagger.domain.Session;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.mongodb.morphia.Datastore;

import java.util.List;
import java.util.Set;

public class BagCloseHandler implements Handler<RoutingContext> {
    private final Datastore datastore;

    public BagCloseHandler(Datastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public void handle(RoutingContext ctx) {
        Integer sessionId = Integer.valueOf(ctx.request().getParam("sessionId"));
        Integer bagId = Integer.valueOf(ctx.request().getParam("bagId"));
        FruitUser fruitUser = ctx.session().get("fruitUser");

        List<Session> fruitSessions = datastore.find(Session.class).field("user").equal(fruitUser).field("number").equal(sessionId).asList();

        if (fruitSessions.isEmpty()) {
            ctx.fail(new Exception("Session not found."));
            return;
        }

        Session fruitSession = fruitSessions.get(0);

        List<FruitBag> bags = datastore.find(FruitBag.class).field("session").equal(fruitSession).field("number").equal(bagId).asList();
        if (bags.isEmpty()) {
            ctx.fail(new Exception("Bag not found!"));
            return;
        }

        FruitBag bag = bags.get(0);

        if (bag.getFinishDate() != null) {
            ctx.fail(new Exception("Bag " + bagId + " already closed!"));
            return;
        }
        Set<Integer> fruitIds = bag.getFruits();
        int sum = 0;
        for (Integer fruitId : fruitIds) {
            sum += FruitQueue.QUEUE[fruitId];
        }
        if (sum < 1000) {
            ctx.fail(new Exception("Bag weight is below 1000 grams!!!!"));
            return;
        }

        bag.closeNow();
        datastore.save(bag);
        ctx.response().end();
    }
}
