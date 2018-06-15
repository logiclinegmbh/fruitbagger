package de.logicline.fruitbagger

import de.logicline.fruitbagger.domain.FruitBag
import de.logicline.fruitbagger.domain.FruitUser
import de.logicline.fruitbagger.domain.Session
import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import org.mongodb.morphia.Datastore

class SessionCloseHandler(private val datastore: Datastore, private val fruits: List<Int>) : Handler<RoutingContext> {

    override fun handle(ctx: RoutingContext) {
        val sessionId = Integer.valueOf(ctx.request().getParam("sessionId"))
        val fruitUser = ctx.session()
            .get<FruitUser>("fruitUser")

        val fruitSessions = datastore.find(Session::class.java)
            .field("user")
            .equal(fruitUser)
            .field("number")
            .equal(sessionId)
            .asList()

        if (fruitSessions.isEmpty()) {
            ctx.fail(Exception("Session not found. Can't close."))
            return
        }

        val fruitSession = fruitSessions[0]

        if (fruitSession.finishDate != null) {
            ctx.fail(Exception("Session $sessionId already closed!"))
            return
        }

        fruitSession.closeNow()

        val bags = datastore.find(FruitBag::class.java)
            .field("session")
            .equal(fruitSession)
            .asList()
        bags.removeIf({ this.bagWeighsLessThan1000g(it) })
        bags.stream()
            .forEach { fruitBag -> fruitBag.closeNow() }
        fruitSession.bagCount = bags.size

        datastore.save(fruitSessions)
        datastore.save(bags)
        ctx.response()
            .end()
    }

    private fun bagWeighsLessThan1000g(bag: FruitBag): Boolean {
        var sum = 0
        for (fruit in bag.fruits) {
            sum += fruits[fruit]
        }
        return sum < 1000
    }
}
