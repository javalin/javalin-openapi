package io.javalin.openapi.plugin

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.http.ContentType.Application
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import org.slf4j.LoggerFactory

class OpenApiFeature(
    private val configuration: OpenApiConfiguration
) {

    class OpenApiConfiguration {
        var title = "OpenApi Title"
        var description = "OpenApi Description"
        var version = "OpenApi Version"
        var documentationPath = "/openapi"
        var documentation: String? = null
    }

    private suspend fun intercept(context: PipelineContext<Unit, ApplicationCall>) {
        if (context.call.request.local.uri == configuration.documentationPath) {
            context.call.respondText(contentType = Application.Json, status = HttpStatusCode.OK) {
                configuration.documentation!!
            }
        }
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, OpenApiConfiguration, OpenApiFeature> {

        override val key = AttributeKey<OpenApiFeature>("OpenAPI")
        private val logger = LoggerFactory.getLogger(OpenApiFeature::class.java)

        override fun install(pipeline: ApplicationCallPipeline, configure: OpenApiConfiguration.() -> Unit): OpenApiFeature {
            val configuration = OpenApiConfiguration().apply(configure)
            val feature = OpenApiFeature(configuration)

            configuration.documentation = readResource("/openapi.json")
                ?.replaceFirst("{openapi.title}", configuration.title)
                ?.replaceFirst("{openapi.description}", configuration.description)
                ?.replaceFirst("{openapi.version}", configuration.version)

            if (configuration.documentation != null) {
                pipeline.intercept(ApplicationCallPipeline.Call) {
                    feature.intercept(this)
                }
            }
            else {
                logger.warn("OpenApi documentation not found")
            }

            return feature
        }

        private fun readResource(path: String): String? =
            OpenApiFeature::class.java.getResource(path)?.readText()

    }

}