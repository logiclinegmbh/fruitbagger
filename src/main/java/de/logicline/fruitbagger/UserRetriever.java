package de.logicline.fruitbagger;

import de.logicline.fruitbagger.domain.FruitUser;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import org.mongodb.morphia.Datastore;

import java.util.List;
import java.util.Optional;

public class UserRetriever implements Handler<RoutingContext> {
    // retrieve the user profile, this is a common feature but not from the official OAuth2 spec
    private final Datastore datastore;

    public UserRetriever(Datastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public void handle(RoutingContext ctx) {
        Session session = ctx.session();
        AccessToken user = (AccessToken) ctx.user();
        user.userInfo(res -> {
            if (res.failed()) {
                ctx.session().destroy();
                ctx.fail(res.cause());
                ctx.next();
                return;
            }
            if (session.get("fruitUser") != null) {
                ctx.next();
                return;
            }
            final JsonObject userInfo = res.result();
            user.fetch("https://api.github.com/user/emails", res2 -> {
                if (res2.failed()) {
                    ctx.session().destroy();
                    ctx.fail(res2.cause());
                    return;
                }
                JsonArray emails = res2.result().jsonArray();
                userInfo.put("private_emails", emails);

                Optional<Object> first = emails.stream().filter(email -> {
                    JsonObject email1 = (JsonObject) email;
                    return email1.containsKey("primary") && email1.getBoolean("primary");
                }).findFirst();
                if (first.isPresent()) {
                    JsonObject primaryEmail = (JsonObject) first.get();
                    String email = primaryEmail.getString("email");
                    List<FruitUser> users = datastore.find(FruitUser.class).field("email").equalIgnoreCase(email).asList();
                    FruitUser dbuser = null;
                    if (users.isEmpty()) {
                        dbuser = new FruitUser(email);
                        datastore.save(dbuser);
                    } else
                        dbuser = users.get(0);
                    session.put("fruitUser", dbuser);
                    ctx.next();
                    return;
                }
                ctx.fail(403);
            });
        });

    }
}
