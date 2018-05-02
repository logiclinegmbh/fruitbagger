package de.logicline.fruitbagger

import de.logicline.fruitbagger.domain.FruitBag
import de.logicline.fruitbagger.domain.FruitUser
import de.logicline.fruitbagger.domain.Session
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import org.mongodb.morphia.Datastore

import java.util.HashMap

class RetrieveFruitHandler(private val datastore: Datastore, private val fruits: List<Int>) : Handler<RoutingContext> {

    override fun handle(ctx: RoutingContext) {
        val sessionId = ctx.request().getParam("sessionId")
        val fruitUser = ctx.session().get<FruitUser>("fruitUser")

        val fruitSessions = datastore.find(Session::class.java).field("user").equal(fruitUser).field("number")
                .equal(Integer.valueOf(sessionId)).asList()

        if (fruitSessions.isEmpty()) {
            ctx.fail(Exception("Session not found. Create a new one."))
            return
        }

        val fruitSession = fruitSessions[0]

        val allBags = datastore.find(FruitBag::class.java).field("session").equal(fruitSession).asList()
        val totalSessionFruits = allBags.stream()
                .filter { bag -> bag.fruits != null && !bag.fruits.isEmpty() }
                .mapToInt { bag -> bag.fruits.size }
                .sum()
        val currentIndex = fruitSession.fruitIndex

        if (currentIndex!! - totalSessionFruits >= fruitSession.lookAhead) {
            ctx.fail(Exception("You have reached the lookahead. Put some fruits in a bag, fruitbagger!"))
            return
        }

        if (currentIndex >= fruits.size) {
            ctx.fail(204)
            return
        }

        val weight = fruits[currentIndex]
        fruitSession.incrementIndex()
        datastore.save(fruitSession)
        val responseMap = HashMap<String, Any>()
        responseMap[currentIndex.toString()] = weight

        ctx.response().end(JsonObject.mapFrom(responseMap).encodePrettily())
    }

}
