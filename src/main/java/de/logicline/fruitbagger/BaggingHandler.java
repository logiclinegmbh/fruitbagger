package de.logicline.fruitbagger;

import de.logicline.fruitbagger.domain.FruitBag;
import de.logicline.fruitbagger.domain.FruitUser;
import de.logicline.fruitbagger.domain.Session;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.Datastore;

import java.util.List;
import java.util.Optional;

public class BaggingHandler implements Handler<RoutingContext> {

    private final Datastore datastore;

    public BaggingHandler(Datastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public void handle(RoutingContext ctx) {
        Integer sessionId = Integer.valueOf(ctx.request().getParam("sessionId"));
        Integer bagId = Integer.valueOf(ctx.request().getParam("bagId"));
        Integer fruitIndex = Integer.valueOf(ctx.request().getParam("fruitIndex"));
        FruitUser fruitUser = ctx.session().get("fruitUser");

        List<Session> fruitSessions = datastore.find(Session.class).field("user").equal(fruitUser).field("number").equal(sessionId).asList();

        if (fruitSessions.isEmpty()) {
            ctx.fail(new Exception("Session not found. Create a new one."));
            return;
        }

        Session fruitSession = fruitSessions.get(0);

        if (fruitSession.getFinishDate() != null) {
            ctx.fail(new Exception("Session " + sessionId + " already closed!"));
            return;
        }

        if (fruitIndex >= fruitSession.getFruitIndex()) {
            ctx.fail(new Exception("You're trying to bag fruit that I haven't even given to you. Thought I wouldn't realize??"));
            return;
        }

        List<FruitBag> bags = datastore.find(FruitBag.class).field("session").equal(fruitSession).asList();

        Optional<FruitBag> currentBagOpt = bags.stream().filter(b -> b.getNumber() != null && b.getNumber().equals(bagId)).findAny();
        if (!currentBagOpt.isPresent()) {
            ctx.fail(new Exception("Bag not found!"));
            return;
        }

        FruitBag bag = currentBagOpt.get();

        if (bag.getFinishDate() != null) {
            ctx.fail(new Exception("Bag " + bagId + " already closed!"));
            return;
        }

        Optional<FruitBag> bagWithFruit = bags.stream().filter(b -> b.getFruits() != null && b.getFruits().contains(fruitIndex)).findAny();
        if (bagWithFruit.isPresent()) {
            ctx.fail(new Exception("Bag " + bagWithFruit.get().getNumber() + " already contains that fruit. Get up earlier to fool me!"));
            return;
        }

        bag.getFruits().add(Integer.valueOf(fruitIndex));
        datastore.save(bag);
        ctx.response().end("[" + StringUtils.join(bag.getFruits(), ",") + "]");
    }
}
