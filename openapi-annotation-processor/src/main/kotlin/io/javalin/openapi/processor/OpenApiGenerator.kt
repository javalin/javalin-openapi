package io.javalin.openapi.processor

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.javalin.openapi.ContentType.AUTODETECT
import io.javalin.openapi.NULL_CLASS
import io.javalin.openapi.NULL_STRING
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiContent
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.processor.OpenApiGenerator.In.COOKIE
import io.javalin.openapi.processor.OpenApiGenerator.In.FORM_DATA
import io.javalin.openapi.processor.OpenApiGenerator.In.HEADER
import io.javalin.openapi.processor.OpenApiGenerator.In.PATH
import io.javalin.openapi.processor.OpenApiGenerator.In.QUERY
import io.javalin.openapi.processor.shared.JsonExtensions.addString
import io.javalin.openapi.processor.shared.JsonExtensions.computeIfAbsent
import io.javalin.openapi.processor.shared.JsonExtensions.toJsonArray
import io.javalin.openapi.processor.shared.ProcessorUtils
import io.javalin.openapi.processor.shared.JsonTypes
import io.javalin.openapi.processor.shared.JsonTypes.getTypeMirror
import io.javalin.openapi.processor.shared.JsonTypes.toModel
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import javax.annotation.processing.FilerException
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic
import javax.tools.Diagnostic.Kind.WARNING
import javax.tools.StandardLocation

internal class OpenApiGenerator {

    private val componentReferences: MutableSet<TypeMirror> = mutableSetOf()

    fun generate(roundEnvironment: RoundEnvironment) {
        try {
            val openApiAnnotations = ArrayList<OpenApi>()

            roundEnvironment.getElementsAnnotatedWith(OpenApi::class.java).forEach { annotatedElement ->
                annotatedElement.getAnnotation(OpenApi::class.java)?.let { openApiAnnotations.add(it) }
            }

            val result = generateSchema(openApiAnnotations)
            val resource = OpenApiAnnotationProcessor.filer.createResource(StandardLocation.CLASS_OUTPUT, "", "openapi-plugin/openapi.json")
            val location = resource.toUri()

            resource.openWriter().use {
                it.write(result)
            }

            val parsedSchema = OpenAPIV3Parser().readLocation(location.toString(), emptyList(), ParseOptions())

            if (parsedSchema.messages.size > 0) {
                OpenApiAnnotationProcessor.messager.printMessage(Diagnostic.Kind.NOTE, "OpenApi Validation Warnings :: ${parsedSchema.messages.size}")
            }

            parsedSchema.messages.forEach {
                OpenApiAnnotationProcessor.messager.printMessage(WARNING, it)
            }
        } catch (filerException: FilerException) {
            // openapi-plugin/openapi.json has been created during previous compilation phase
        } catch (throwable: Throwable) {
            ProcessorUtils.printException(throwable)
        }
    }

    /**
     * Based on https://swagger.io/specification/
     *
     * @param annotations annotation instances to map
     * @return OpenApi JSON response
     */
    private fun generateSchema(annotations: Collection<OpenApi>): String {
        val openApi = JsonObject()
        openApi.addProperty("openapi", "3.0.3")

        // fill info
        val info = JsonObject()
        info.addProperty("title", "")
        info.addProperty("version", "")
        openApi.add("info", info)

        // fill paths
        val paths = JsonObject()
        openApi.add("paths", paths)

        for (routeAnnotation in annotations) {
            if (routeAnnotation.ignore) {
                continue
            }

            val path = paths.computeIfAbsent(routeAnnotation.path) { JsonObject() }

            // https://swagger.io/specification/#paths-object
            for (method in routeAnnotation.methods) {
                val operation = JsonObject()

                // General
                operation.add("tags", routeAnnotation.tags.toJsonArray())
                operation.addString("summary", routeAnnotation.summary)
                operation.addString("description", routeAnnotation.description)

                // ExternalDocs
                // ~ https://swagger.io/specification/#external-documentation-object
                // operation.addProperty("externalDocs", ); UNSUPPORTED

                // OperationId
                operation.addString("operationId", routeAnnotation.operationId)

                // Parameters
                // ~ https://swagger.io/specification/#parameter-object
                val parameters = JsonArray()

                for (queryParameterAnnotation in routeAnnotation.queryParams) {
                    parameters.add(fromParameter(QUERY, queryParameterAnnotation))
                }

                for (headerParameterAnnotation in routeAnnotation.headers) {
                    parameters.add(fromParameter(HEADER, headerParameterAnnotation))
                }

                for (pathParameterAnnotation in routeAnnotation.pathParams) {
                    parameters.add(fromParameter(PATH, pathParameterAnnotation))
                }

                for (cookieParameterAnnotation in routeAnnotation.cookies) {
                    parameters.add(fromParameter(COOKIE, cookieParameterAnnotation))
                }

                for (formParameterAnnotation in routeAnnotation.formParams) {
                    parameters.add(fromParameter(FORM_DATA, formParameterAnnotation))
                }

                operation.add("parameters", parameters)

                // RequestBody
                // ~ https://swagger.io/specification/#request-body-object
                val requestBodyAnnotation = routeAnnotation.requestBody
                val requestBody = JsonObject()
                requestBody.addString("description", requestBodyAnnotation.description)
                requestBody.addContent(requestBodyAnnotation.content)
                if (requestBody.size() > 0) {
                    operation.add("requestBody", requestBody)
                }
                requestBody.addProperty("required", requestBodyAnnotation.required)

                // Responses
                // ~ https://swagger.io/specification/#responses-object
                val responses = JsonObject()

                for (responseAnnotation in routeAnnotation.responses) {
                    val response = JsonObject()
                    response.addString("description", responseAnnotation.description)
                    response.addContent(responseAnnotation.content)
                    responses.add(responseAnnotation.status, response)
                }

                operation.add("responses", responses)

                // Callbacks
                // ~ https://swagger.io/specification/#callback-object
                // operation.addProperty("callbacks, ); UNSUPPORTED

                // Deprecated
                operation.addProperty("deprecated", routeAnnotation.deprecated)

                // Security
                // ~ https://swagger.io/specification/#security-requirement-object
                val security = JsonArray()

                for (securityAnnotation in routeAnnotation.security) {
                    val securityEntry = JsonObject()
                    val scopes = JsonArray()

                    for (scopeAnnotation in securityAnnotation.scopes) {
                        scopes.add(scopeAnnotation)
                    }

                    securityEntry.add(securityAnnotation.name, scopes)
                    security.add(securityEntry)
                }

                operation.add("security", security)

                // servers
                path.add(method.name.lowercase(), operation)
            }
        }

        val components = JsonObject()
        val schemas = JsonObject()
        val generatedComponents = mutableSetOf<TypeMirror>()

        while (generatedComponents.size < componentReferences.size) {
            for (componentReference in componentReferences.toMutableList()) {
                if (generatedComponents.contains(componentReference)) {
                    continue
                }

                val type = componentReference.toModel() ?: continue

                if (type.sourceElement.toString() == "java.lang.Object") {
                    generatedComponents.add(componentReference)
                    continue
                }

                val (schema, references) = createTypeSchema(type, false)
                schemas.add(type.simpleName, schema)
                componentReferences.addAll(references)
                generatedComponents.add(componentReference)
            }
        }

        components.add("schemas", schemas)
        openApi.add("components", components)

        return OpenApiAnnotationProcessor.gson.toJson(openApi)
    }

    enum class In(val identifier: String) {
        QUERY("query"),
        HEADER("header"),
        PATH("path"),
        COOKIE("cookie"),
        FORM_DATA("formData")
    }

    // Parameter
    // https://swagger.io/specification/#parameter-object
    private fun fromParameter(`in`: In, parameterInstance: OpenApiParam?): JsonObject {
        val parameter = JsonObject()
        parameter.addString("name", parameterInstance!!.name)
        parameter.addString("in", `in`.identifier)
        parameter.addString("description", parameterInstance.description)
        parameter.addProperty("required", parameterInstance.required)
        parameter.addProperty("deprecated", parameterInstance.deprecated)
        parameter.addProperty("allowEmptyValue", parameterInstance.allowEmptyValue)

        val schema = createTypeDescriptionWithReferences(parameterInstance.getTypeMirror { type })
        parameterInstance.example
            .takeIf { it.isNotEmpty() }
            .let { schema.addProperty("example", it) }
        parameter.add("schema", schema)

        return parameter
    }

    private fun JsonObject.addContent(annotations: Array<OpenApiContent>) {
        val requestBodyContent = JsonObject()

        for (contentAnnotation in annotations) {
            val from = contentAnnotation.getTypeMirror { from }
            val type = contentAnnotation.type
            val format = contentAnnotation.format.takeIf { it != NULL_STRING }
            val properties = contentAnnotation.properties
            var mimeType = contentAnnotation.mimeType

            if (AUTODETECT == mimeType) {
                mimeType = JsonTypes.detectContentType(from)
            }

            val schema: JsonObject = when {
                properties.isEmpty() && from.toString() != NULL_CLASS::class.java.name -> createTypeDescriptionWithReferences(from)
                properties.isEmpty() -> {
                    val schema = JsonObject()
                    schema.addProperty("type", type)
                    format?.also { schema.addProperty("format", it) }
                    schema
                }
                else -> {
                    val schema = JsonObject()
                    val propertiesSchema = JsonObject()
                    schema.addProperty("type", "object")

                    for (contentProperty in properties) {
                        val propertyScheme = JsonObject()
                        val propertyFormat = contentProperty.format.takeIf { it != NULL_STRING }

                        if (contentProperty.isArray) {
                            propertyScheme.addProperty("type", "array")

                            val items = JsonObject()
                            items.addProperty("type", contentProperty.type)
                            propertyFormat?.let { items.addProperty("format", it) }
                            propertyScheme.add("items", items)
                        } else {
                            propertyScheme.addProperty("type", contentProperty.type)
                            propertyFormat?.let { propertyScheme.addProperty("format", it) }
                        }

                        propertiesSchema.add(contentProperty.name, propertyScheme)
                    }

                    schema.add("properties", propertiesSchema)
                    schema
                }
            }

            val mediaType = JsonObject()
            mediaType.add("schema", schema)
            requestBodyContent.add(mimeType, mediaType)
        }

        if (requestBodyContent.size() > 0) {
            add("content", requestBodyContent)
        }
    }

    private fun createTypeDescriptionWithReferences(type: TypeMirror): JsonObject {
        val model = type.toModel()!!
        val (json, references) = createTypeDescription(model)
        componentReferences.addAll(references)
        return json
    }

}