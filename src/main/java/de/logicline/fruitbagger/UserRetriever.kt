package de.logicline.fruitbagger

import de.logicline.fruitbagger.domain.FruitUser
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.oauth2.AccessToken
import io.vertx.ext.web.RoutingContext
import org.mongodb.morphia.Datastore
import java.util.*

class UserRetriever(private val datastore: Datastore) : Handler<RoutingContext> {

    override fun handle(ctx: RoutingContext) {
        val session = ctx.session()
        val user = ctx.user() as AccessToken
        user.userInfo { res ->
            if (res.failed()) {
                ctx.session().destroy()
                ctx.fail(res.cause())
                ctx.next()
                return@userInfo
            }
            // if the user already has registered for this browser session
            if (session.get<Any>("fruitUser") != null) {
                ctx.next()
                return@userInfo
            }
            registerUser(user, ctx)
        }

    }

    private fun registerUser(user: AccessToken, ctx: RoutingContext) {
        user.fetch("https://api.github.com/user/emails") { res ->
            if (res.failed()) {
                ctx.session().destroy()
                ctx.fail(res.cause())
                return@fetch
            }
            val emails = res.result().jsonArray()
            val getPrimaryUserMail = emails.stream().filter { email ->
                val email1 = email as JsonObject
                email1.containsKey("primary") && email1.getBoolean("primary")!!
            }.findFirst()

            if (getPrimaryUserMail.isPresent) {
                addUserToDb(getPrimaryUserMail, ctx)
                return@fetch
            }
            ctx.fail(403)
        }
    }

    private fun addUserToDb(getPrimaryUserMail: Optional<Any>, ctx: RoutingContext) {
        val primaryEmail = getPrimaryUserMail.get() as JsonObject
        val email = primaryEmail.getString("email")
        val users = datastore.find(FruitUser::class.java).field("email").equalIgnoreCase(email).asList()
        var dbuser: FruitUser?
        if (users.isEmpty()) {
            dbuser = FruitUser(email)
            datastore.save(dbuser)
        } else
            dbuser = users[0]
        ctx.session().put("fruitUser", dbuser)
        ctx.next()
    }
}
