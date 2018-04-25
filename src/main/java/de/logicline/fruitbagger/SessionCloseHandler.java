package de.logicline.fruitbagger;

import de.logicline.fruitbagger.domain.FruitBag;
import de.logicline.fruitbagger.domain.FruitQueue;
import de.logicline.fruitbagger.domain.FruitUser;
import de.logicline.fruitbagger.domain.Session;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.mongodb.morphia.Datastore;

import java.util.List;

public class SessionCloseHandler implements Handler<RoutingContext> {
    private final Datastore datastore;

    public SessionCloseHandler(Datastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public void handle(RoutingContext ctx) {
        Integer sessionId = Integer.valueOf(ctx.request().getParam("sessionId"));
        FruitUser fruitUser = ctx.session().get("fruitUser");

        List<Session> fruitSessions = datastore.find(Session.class).field("user").equal(fruitUser).field("number").equal(sessionId).asList();

        if (fruitSessions.isEmpty()) {
            ctx.fail(new Exception("Session not found. Can't close."));
            return;
        }

        Session fruitSession = fruitSessions.get(0);

        if (fruitSession.getFinishDate() != null) {
            ctx.fail(new Exception("Session " + sessionId + " already closed!"));
            return;
        }

        fruitSession.closeNow();

        List<FruitBag> bags = datastore.find(FruitBag.class).field("session").equal(fruitSession).asList();
        bags.removeIf(this::bagWeighsLessThan1000g);
        bags.stream().forEach(fruitBag -> fruitBag.closeNow());
        fruitSession.setBagCount(bags.size());

        datastore.save(fruitSessions);
        datastore.save(bags);
        ctx.response().end();
    }

    private boolean bagWeighsLessThan1000g(FruitBag bag) {
        int sum = 0;
        for (int fruit : bag.getFruits()) {
            sum += FruitQueue.QUEUE[fruit];
        }
        return sum < 1000;
    }
}
