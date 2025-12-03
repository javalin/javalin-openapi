package io.javalin.openapi.plugin.swagger.specification

import io.javalin.Javalin
import io.javalin.http.Context
import kong.unirest.HttpRequest
import kong.unirest.Unirest
import java.util.concurrent.CountDownLatch
import java.util.function.Supplier

internal class JavalinBehindProxy(
    javalinSupplier: Supplier<Javalin>,
    basePath: String
) : AutoCloseable {

    private val javalin = javalinSupplier
        .get()

    private val proxy = Javalin.create()
        .get("/") { it.html("Index") }
        .get(basePath) { Unirest.get(it.javalinLocation()).redirect(it) }
        .get("$basePath/<uri>") { Unirest.get(it.javalinLocation()).redirect(it) }
        .head("$basePath/<uri>") { Unirest.head(it.javalinLocation()).redirect(it) }
        .post("$basePath/<uri>") { Unirest.post(it.javalinLocation()).redirect(it) }
        .put("$basePath/<uri>") { Unirest.put(it.javalinLocation()).redirect(it) }
        .delete("$basePath/<uri>") { Unirest.delete(it.javalinLocation()).redirect(it) }
        .options("$basePath/<uri>") { Unirest.options(it.javalinLocation()).redirect(it) }

    init {
        start()
    }

    fun start(): JavalinBehindProxy = also {
        val awaitStart = CountDownLatch(2)

        proxy
            .events { it.serverStarted { awaitStart.countDown() } }
            .start(0)

        javalin
            .events { it.serverStarted { awaitStart.countDown() } }
            .start(0)

        awaitStart.await()
    }

    fun proxyPort(): Int = proxy.port()

    fun appPort(): Int = javalin.port()

    fun stop() {
        proxy.stop()
        javalin.stop()
    }

    override fun close() {
        stop()
    }

    private fun <R : HttpRequest<*>> R.redirect(ctx: Context) {
        ctx.headerMap().forEach { (key, value) -> header(key, value) }
        val response = this.asBytes()
        response.headers.all().forEach { ctx.header(it.name, it.value) }
        ctx.status(response.status).result(response.body)
    }

    private fun Context.javalinLocation(): String =
        "http://localhost:${appPort()}/${pathParamMap()["uri"] ?: ""}"

}