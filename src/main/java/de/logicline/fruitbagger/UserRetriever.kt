package de.logicline.fruitbagger

import de.logicline.fruitbagger.domain.FruitUser
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.oauth2.AccessToken
import io.vertx.ext.web.RoutingContext
import org.mongodb.morphia.Datastore

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
            if (session.get<Any>("fruitUser") != null) {
                ctx.next()
                return@userInfo
            }
            val userInfo = res.result()
            user.fetch("https://api.github.com/user/emails") { res2 ->
                if (res2.failed()) {
                    ctx.session().destroy()
                    ctx.fail(res2.cause())
                    return@fetch
                }
                val emails = res2.result().jsonArray()
                userInfo.put("private_emails", emails)

                val first = emails.stream().filter { email ->
                    val email1 = email as JsonObject
                    email1.containsKey("primary") && email1.getBoolean("primary")!!
                }.findFirst()
                if (first.isPresent) {
                    val primaryEmail = first.get() as JsonObject
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
                    return@fetch
                }
                ctx.fail(403)
            }
        }

    }
}
