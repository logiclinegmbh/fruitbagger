package de.logicline.fruitbagger

import de.logicline.fruitbagger.domain.FruitBag
import de.logicline.fruitbagger.domain.FruitUser
import de.logicline.fruitbagger.domain.Session
import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import org.mongodb.morphia.Datastore

class CreateBagHandler(private val datastore: Datastore) : Handler<RoutingContext> {

    override fun handle(ctx: RoutingContext) {
        val sessionId = ctx.request()
            .getParam("sessionId")
        val fruitUser = ctx.session()
            .get<FruitUser>("fruitUser")

        val fruitSessions = datastore.find(Session::class.java)
            .field("user")
            .equal(fruitUser)
            .field("number")
            .equal(Integer.valueOf(sessionId))
            .asList()
        if (fruitSessions.isEmpty()) {
            ctx.fail(Exception("Session not found. Create a new one."))
            return
        }

        val fruitSession = fruitSessions[0]
        if (fruitSession.finishDate != null) {
            ctx.fail(Exception("Session $sessionId already closed!"))
            return
        }

        // get all bags
        val allBags = datastore.find(FruitBag::class.java)
            .field("session")
            .equal(fruitSession)
            .asList()

        // filter bags that are note finished
        var emptyBags = allBags.filter { bag -> bag.finishDate == null }
        if (!emptyBags.isEmpty()) {
            ctx.fail(Exception("You have unfinished fruit bags left. Don't try to fool me! Your open bag: " + allBags[0].number!!))
            return
        }


        // calculate the new id
        val max = allBags.stream()
            .mapToInt { s -> if (s.number == null) 0 else s.number }
            .max()
        val bagCount = max.orElse(0) + 1
        val newBag = FruitBag.create(fruitSession, bagCount)
        fruitSession.bagCount = bagCount
        datastore.save(newBag)
        datastore.save(fruitSession)
        ctx.response()
            .end(newBag.number!!.toString())
    }
}
