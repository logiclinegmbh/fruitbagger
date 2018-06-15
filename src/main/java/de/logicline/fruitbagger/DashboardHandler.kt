package de.logicline.fruitbagger

import de.logicline.fruitbagger.domain.Session
import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.templ.HandlebarsTemplateEngine
import org.mongodb.morphia.Datastore
import org.mongodb.morphia.query.Sort

class DashboardHandler(private val datastore: Datastore) : Handler<RoutingContext> {
    private val engine = HandlebarsTemplateEngine.create()


    override fun handle(ctx: RoutingContext) {
        val fruitSessions = getSessions()
        ctx.put("entry", fruitSessions)
        addAddOneFilter()
        // and now delegate to the engine to render it.
        renderView(ctx)

    }

    private fun getSessions(): List<Session>? {
        val fruitsessions = datastore.find<Session>(Session::class.java)
            .field("finishDate")
            .exists()
            .field("bagCount")
            .notEqual(0)
            .order(Sort.descending("bagCount"))
            .asList()
        return fruitsessions
    }

    private fun renderView(ctx: RoutingContext) {
        engine.render(ctx, "views", "/dashboard.hbs") { res3 ->
            if (res3.succeeded()) {
                ctx.response()
                    .putHeader("Content-Type", "text/html")
                    .end(res3.result())
            } else {
                ctx.fail(res3.cause())
            }
        }
    }

    /**
     * Registers a handlebar helper to add one to a number
     */
    private fun addAddOneFilter() {
        engine.handlebars
            .registerHelper<Any>("addone") { _, options ->
                (Integer.valueOf(options.fn(this).toString()) + 1).toString()
            }
    }
}
