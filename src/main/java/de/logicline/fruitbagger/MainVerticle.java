package de.logicline.fruitbagger;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import io.vertx.core.AbstractVerticle;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.providers.GithubAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.templ.HandlebarsTemplateEngine;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import java.util.Map;


/*
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public class MainVerticle extends AbstractVerticle {

    private final HandlebarsTemplateEngine engine = HandlebarsTemplateEngine.create();

    @Override
    public void start() throws Exception {
        final Map<String, String> env = System.getenv();
        final Morphia morphia = new Morphia();
        morphia.mapPackage("de.logicline.fruitbagger.domain");
        MongoClient mongoClient = new MongoClient(new MongoClientURI(env.get("MONGODB_URI")));
        final Datastore datastore = morphia.createDatastore(mongoClient, env.get("MONGO_DBNAME"));
        datastore.ensureIndexes();

        // to organize our code in a reusable way.
        final Router router = Router.router(vertx);
        // We need cookies and sessions
        router.route().handler(CookieHandler.create());
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
        // Simple auth service which uses a GitHub to authenticate the user
        OAuth2Auth authProvider = GithubAuth.create(vertx, env.get("CLIENT_ID"), env.get("CLIENT_SECRET"));
        // We need a user session handler too to make sure the user is stored in the session between requests
        router.route().handler(UserSessionHandler.create(authProvider));

        router.route("/static/*").handler(StaticHandler.create().setCachingEnabled(false));

        router.route("/").handler(ctx -> {
            engine.render(ctx, "views", "/index.hbs", res3 -> {
                if (res3.succeeded()) {
                    ctx.response()
                        .putHeader("Content-Type", "text/html")
                        .end(res3.result());
                } else {
                    ctx.fail(res3.cause());
                }
            });
        });

        // we now protect the resource under the path "/protected"
        router.route("/profile").handler(
            OAuth2AuthHandler.create(authProvider, env.get("OAUTH_GITHUB_CALLBACK_URL"))
                // we now configure the oauth2 handler, it will setup the callback handler
                // as expected by your oauth2 provider.
                .setupCallback(router.route("/callback"))
                // for this resource we require that users have the authority to retrieve the user emails
                .addAuthority("user:email")
        ).handler(new UserRetriever(datastore));
        // Entry point to the application, this will render a custom template.
        router.get("/profile").handler(new ProfileHandler(datastore));
        router.get("/profile/resetapikey").handler(new ResetApiKeyHandler(datastore));

        router.route("/api/*").handler(new ApiUserRetriever(datastore));
        router.post("/api/session").handler(new CreateSessionHandler(datastore));
        router.get("/api/fruits/:sessionId").handler(new RetrieveFruitHandler(datastore));
        router.post("/api/bag/:sessionId").handler(new CreateBagHandler(datastore));
        router.post("/api/bagging/:sessionId/:bagId/:fruitIndex").handler(new BaggingHandler(datastore));
        router.put("/api/bag/:sessionId/:bagId").handler(new BagCloseHandler(datastore));
        router.put("/api/session/:sessionId").handler(new SessionCloseHandler(datastore));


        router.route().failureHandler(new FailureHandler());

        vertx.createHttpServer().requestHandler(router::accept).listen(Integer.valueOf(env.get("PORT")));
    }
}
