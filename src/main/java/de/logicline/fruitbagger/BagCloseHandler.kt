package de.logicline.fruitbagger

import de.logicline.fruitbagger.domain.FruitBag
import de.logicline.fruitbagger.domain.FruitUser
import de.logicline.fruitbagger.domain.Session
import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import org.mongodb.morphia.Datastore

class BagCloseHandler(private val datastore: Datastore, private val fruits: List<Int>) : Handler<RoutingContext> {

    override fun handle(ctx: RoutingContext) {
        val sessionId = Integer.valueOf(ctx.request().getParam("sessionId"))
        val bagId = Integer.valueOf(ctx.request().getParam("bagId"))
        val fruitUser = ctx.session()
            .get<FruitUser>("fruitUser")

        val fruitSessions = datastore.find(Session::class.java)
            .field("user")
            .equal(fruitUser)
            .field("number")
            .equal(sessionId)
            .asList()

        if (fruitSessions.isEmpty()) {
            ctx.fail(Exception("Session not found."))
            return
        }

        val fruitSession = fruitSessions[0]

        val bags = datastore.find(FruitBag::class.java)
            .field("session")
            .equal(fruitSession)
            .field("number")
            .equal(bagId)
            .asList()
        if (bags.isEmpty()) {
            ctx.fail(Exception("Bag not found!"))
            return
        }

        val bag = bags[0]

        if (bag.finishDate != null) {
            ctx.fail(Exception("Bag $bagId already closed!"))
            return
        }
        val fruitIds = bag.fruits
        var sum = 0
        for (fruitId in fruitIds) {
            sum += fruits[fruitId]
        }
        if (sum < 1000) {
            ctx.fail(Exception("Bag weight is below 1000 grams!!!!"))
            return
        }

        bag.closeNow()
        datastore.save(bag)
        ctx.response()
            .end()
    }
}
