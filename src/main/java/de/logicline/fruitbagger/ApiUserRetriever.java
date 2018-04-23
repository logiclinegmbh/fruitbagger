package de.logicline.fruitbagger;

import de.logicline.fruitbagger.domain.FruitUser;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.Datastore;

import java.util.List;

public class ApiUserRetriever implements Handler<RoutingContext> {
    private final Datastore datastore;

    public ApiUserRetriever(Datastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public void handle(RoutingContext ctx) {
        String auth = ctx.request().getHeader("auth");
        if (StringUtils.isEmpty(auth)) {
            String msg = "No auth header set. RTFM under /profile";
            fail403(ctx, msg);
            return;
        }

        List<FruitUser> users = datastore.find(FruitUser.class).field("apiToken").equal(auth).asList();
        if (users.isEmpty()) {
            String msg = "Wrong auth header. Check API token at /profile";
            fail403(ctx, msg);
            return;
        }
        ctx.session().put("fruitUser", users.get(0));
        ctx.next();
    }

    public void fail403(RoutingContext ctx, String msg) {
        ctx.response().headers().add("Content-Length", String.valueOf(msg.length()));
        ctx.response().write(msg);
        ctx.response().setStatusCode(403);
        ctx.response().end();
    }
}
