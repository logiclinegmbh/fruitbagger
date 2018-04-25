package de.logicline.fruitbagger;

import de.logicline.fruitbagger.domain.Session;
import de.logicline.fruitbagger.domain.SessionListItem;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.templ.HandlebarsTemplateEngine;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Sort;

import java.util.ArrayList;
import java.util.List;

public class DashboardHandler implements Handler<RoutingContext> {
    private final HandlebarsTemplateEngine engine = HandlebarsTemplateEngine.create();
    private final Datastore datastore;

    public DashboardHandler(Datastore datastore) {
        this.datastore = datastore;
    }

    public String calc(String id) {
        return String.valueOf(Integer.valueOf(id) + 1);
    }

    @Override
    public void handle(RoutingContext ctx) {
        List<Session> fruitsessions = datastore.find(Session.class)
            .field("finishDate").exists()
            .order(Sort.descending("bagCount"))
            .asList();
        ctx.put("entry", fruitsessions);
        engine.getHandlebars().registerHelper("addone", (context, options) -> String.valueOf(Integer.valueOf(options.fn(this).toString())+1));
        // and now delegate to the engine to render it.
        engine.render(ctx, "views", "/dashboard.hbs", res3 -> {
            if (res3.succeeded()) {
                ctx.response()
                    .putHeader("Content-Type", "text/html")
                    .end(res3.result());
            } else {
                ctx.fail(res3.cause());
            }
        });

    }
}
