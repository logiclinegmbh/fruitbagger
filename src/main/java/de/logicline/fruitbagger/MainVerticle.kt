package de.logicline.fruitbagger

import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import io.vertx.core.AbstractVerticle
import io.vertx.ext.auth.oauth2.OAuth2Auth
import io.vertx.ext.auth.oauth2.providers.GithubAuth
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.*
import io.vertx.ext.web.sstore.LocalSessionStore
import io.vertx.ext.web.templ.HandlebarsTemplateEngine
import org.apache.logging.log4j.core.util.IOUtils
import org.mongodb.morphia.Datastore
import org.mongodb.morphia.Morphia
import java.io.InputStreamReader
import java.util.function.Consumer


/*
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
class MainVerticle() : AbstractVerticle() {

    private val engine = HandlebarsTemplateEngine.create()
    var datastore: Datastore? = null
    var router: Router? = null
    var env: MutableMap<String, String> = HashMap()
    var fruits: List<Int> = ArrayList()
    @Throws(Exception::class)
    override fun start() {
        env = System.getenv()
        val morphia = Morphia()
        morphia.mapPackage("de.logicline.fruitbagger.domain")

        val mongoClient = MongoClient(MongoClientURI(env["MONGODB_URI"]))
        createDataStore(morphia, mongoClient)
        val authProvider = createRouter()
        loadFruits()
        setUpBaseRoute()
        setUpProfileRoutes(authProvider)
        setUpDashboardHandler()
        setUpApiHandlers()
        vertx.createHttpServer()
            .requestHandler { router!!.accept(it) }
            .listen(Integer.valueOf(env["PORT"]))
    }

    private fun loadFruits() {
        fruits = ArrayList()
        var file = this.javaClass.getResourceAsStream("/fruits.txt")
        var lines = IOUtils.toString(InputStreamReader(file))
            .split("\n")
            .toList()
        var tmp = ArrayList<Int>()
        lines.forEach(Consumer { t -> if (t != "") tmp.add(Integer(t).toInt()) })
        fruits = tmp.toList()
    }

    private fun createDataStore(morphia: Morphia, mongoClient: MongoClient) {
        datastore = morphia.createDatastore(mongoClient, env["MONGO_DBNAME"])
        datastore?.ensureIndexes()
    }

    private fun createRouter(): OAuth2Auth? {
        router = Router.router(vertx)
        // We need cookies and sessions
        router?.route()
            ?.handler(CookieHandler.create())
        router?.route()
            ?.handler(SessionHandler.create(LocalSessionStore.create(vertx)))
        // Simple auth service which uses a GitHub to authenticate the user
        val authProvider = GithubAuth.create(vertx, env["CLIENT_ID"], env["CLIENT_SECRET"])
        // We need a user session handler too to make sure the user is stored in the session between requests
        router?.route()
            ?.handler(UserSessionHandler.create(authProvider))
        router?.route("/static/*")
            ?.handler(StaticHandler.create().setCachingEnabled(false))
        router?.route()
            ?.failureHandler(FailureHandler())
        return authProvider
    }

    private fun setUpDashboardHandler() {
        router?.get("/dashboard")
            ?.handler(DashboardHandler(datastore!!))
    }

    private fun setUpApiHandlers() {
        router?.run {
            route("/api/*").blockingHandler(ApiUserRetriever(datastore), false)
            post("/api/session").blockingHandler(CreateSessionHandler(datastore), false)
            get("/api/fruits/:sessionId").blockingHandler(RetrieveFruitHandler(datastore!!, fruits), false)
            post("/api/bag/:sessionId").blockingHandler(CreateBagHandler(datastore!!), false)
            post("/api/bagging/:sessionId/:bagId/:fruitIndex").blockingHandler(BaggingHandler(datastore), false)
            put("/api/bag/:sessionId/:bagId").blockingHandler(BagCloseHandler(datastore!!, fruits), false)
            put("/api/session/:sessionId").blockingHandler(SessionCloseHandler(datastore!!, fruits), false)
        }
    }

    private fun setUpProfileRoutes(authProvider: OAuth2Auth?) {
        router?.run {
            route("/profile").blockingHandler(
                OAuth2AuthHandler.create(authProvider, env["OAUTH_GITHUB_CALLBACK_URL"])
                    // we now configure the oauth2 handler, it will setup the callback handler
                    // as expected by your oauth2 provider.
                    .setupCallback(router?.route("/callback"))
                    // for this resource we require that users have the authority to retrieve the user emails
                    .addAuthority("user:email"), false
            )
                .blockingHandler(UserRetriever(datastore!!), false)
            // Entry point to the application, this will render a custom template.
            get("/profile").blockingHandler(ProfileHandler(datastore), false)
            get("/profile/resetapikey").handler(ResetApiKeyHandler(datastore))
        }
    }

    private fun setUpBaseRoute() {
        router!!.route("/")
            .handler { ctx ->
                engine.render(ctx, "views", "/index.hbs") { res3 ->
                    if (res3.succeeded()) {
                        ctx.response()
                            .putHeader("Content-Type", "text/html")
                            .end(res3.result())
                    } else {
                        ctx.fail(res3.cause())
                    }
                }
            }
    }
}
