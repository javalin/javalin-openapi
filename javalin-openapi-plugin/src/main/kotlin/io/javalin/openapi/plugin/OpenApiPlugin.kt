package io.javalin.openapi.plugin

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.javalin.Javalin
import io.javalin.plugin.Plugin
import io.javalin.plugin.PluginLifecycleInit
import io.javalin.plugin.json.JavalinJackson.Companion.defaultMapper
import org.slf4j.LoggerFactory

class OpenApiConfiguration {
    var info: OpenApiInfo = OpenApiInfo()
    var documentationPath = "/openapi"
    var documentProcessor: ((ObjectNode) -> String)? = null
    var security: SecurityConfiguration? = null
}

// https://github.com/OAI/OpenAPI-Specification/blob/3.1.0/versions/3.1.0.md#infoObject
class OpenApiInfo {
    // REQUIRED. The title of the API
    var title = "OpenApi Title"

    // A short summary of the API
    var summary: String? = null

    // A description of the API. CommonMark syntax MAY be used for rich text representation
    var description: String? = null

    // A URL to the Terms of Service for the API. This MUST be in the form of a URL.
    var termsOfService: String? = null

    // The contact information for the exposed API
    var contact: OpenApiContact? = null

    // The license information for the exposed API
    var license: OpenApiLicense? = null

    // REQUIRED. The version of the OpenAPI document (which is distinct from the OpenAPI Specification version or the API implementation version).
    var version = ""
}

// https://github.com/OAI/OpenAPI-Specification/blob/3.1.0/versions/3.1.0.md#contactObject
class OpenApiContact {
    // The identifying name of the contact person/organization.
    var name: String? = null

    // The URL pointing to the contact information. This MUST be in the form of a URL.
    var url: String? = null

    // The email address of the contact person/organization. This MUST be in the form of an email address.
    var email: String? = null
}

// https://github.com/OAI/OpenAPI-Specification/blob/3.1.0/versions/3.1.0.md#licenseObject
class OpenApiLicense {
    // REQUIRED. The license name used for the API
    var name = ""

    // An SPDX license expression for the API. The identifier field is mutually exclusive of the url field.
    var identifier: String? = null

    // A URL to the license used for the API. This MUST be in the form of a URL. The url field is mutually exclusive of the identifier field
    var url: String? = null
}

class OpenApiPlugin(private val configuration: OpenApiConfiguration) : Plugin, PluginLifecycleInit {

    private var documentation: String? = null
    private val logger = LoggerFactory.getLogger(OpenApiPlugin::class.java)

    override fun init(app: Javalin) {
        this.documentation = readResource("/openapi.json")
            ?.let { modifyDocumentation(it) }
    }

    private fun modifyDocumentation(rawDocs: String): String =
        with(configuration) {
            val docsNode = defaultMapper().readTree(rawDocs) as ObjectNode

            //process OpenAPI field "info"
            docsNode.replace("info", defaultMapper().convertValue(info, JsonNode::class.java))

            //process OpenAPI field "components"
            val componentsNode = docsNode.get("components") as? ObjectNode? ?: defaultMapper().createObjectNode().also { docsNode.replace("components", it) }

            //process OpenAPI field "securitySchemes"
            val securitySchemes = security?.securitySchemes ?: emptyMap()
            componentsNode.replace("securitySchemes", defaultMapper().convertValue(securitySchemes, JsonNode::class.java))

            //process OpenAPI field "security"
            val securityMap = security?.globalSecurity?.associate { it.name to it.scopes.toTypedArray() }
            docsNode.replace("security", defaultMapper().convertValue(securityMap, JsonNode::class.java))

            return configuration.documentProcessor
                ?.invoke(docsNode)
                ?: docsNode.toPrettyString()
        }

    private fun readResource(path: String): String? =
        OpenApiPlugin::class.java.getResource(path)?.readText()

    override fun apply(app: Javalin) {
        if (documentation == null) {
            logger.warn("OpenApi documentation not found")
            return
        }

        app.get(configuration.documentationPath, OpenApiHandler(documentation!!))
    }

}