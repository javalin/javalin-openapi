package net.dzikoysk.openapi.processor

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.javalin.plugin.openapi.annotations.ContentType.AUTODETECT
import io.javalin.plugin.openapi.annotations.NULL_CLASS
import io.javalin.plugin.openapi.annotations.NULL_STRING
import net.dzikoysk.openapi.processor.annotations.OpenApiContentInstance
import net.dzikoysk.openapi.processor.annotations.OpenApiInstance
import net.dzikoysk.openapi.processor.annotations.OpenApiParamInstance
import net.dzikoysk.openapi.processor.annotations.OpenApiParamInstance.In
import net.dzikoysk.openapi.processor.annotations.OpenApiParamInstance.In.COOKIE
import net.dzikoysk.openapi.processor.annotations.OpenApiParamInstance.In.HEADER
import net.dzikoysk.openapi.processor.annotations.OpenApiParamInstance.In.PATH
import net.dzikoysk.openapi.processor.annotations.OpenApiParamInstance.In.QUERY
import net.dzikoysk.openapi.processor.utils.JsonUtils
import net.dzikoysk.openapi.processor.utils.TypesUtils
import javax.annotation.processing.Messager
import javax.lang.model.element.ElementKind.METHOD
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.TypeMirror

internal class OpenApiGenerator(private val messager: Messager) {

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    private val componentReferences: MutableSet<TypeMirror> = mutableSetOf()

    /**
     * Based on https://swagger.io/specification/
     *
     * @param annotations annotation instances to map
     * @return OpenApi JSON response
     */
    fun generate(annotations: Collection<OpenApiInstance>): String {
        val openApi = JsonObject()
        openApi.addProperty("openapi", "3.0.3")

        // somehow fill info { description, version } properties
        val info = JsonObject()
        info.addProperty("title", "{openapi.title}")
        info.addProperty("description", "{openapi.description}")
        info.addProperty("version", "{openapi.version}")
        openApi.add("info", info)

        // fill paths
        val paths = JsonObject()
        openApi.add("paths", paths)

        for (routeAnnotation in annotations) {
            val path = JsonUtils.computeIfAbsent(paths, routeAnnotation.path()) { JsonObject() }

            // https://swagger.io/specification/#paths-object
            for (method in routeAnnotation.methods()) {
                val operation = JsonObject()

                // General
                operation.add("tags", JsonUtils.toArray(routeAnnotation.tags()))
                addString(operation, "summary", routeAnnotation.summary())
                addString(operation, "description", routeAnnotation.description())

                // ExternalDocs
                // ~ https://swagger.io/specification/#external-documentation-object
                // operation.addProperty("externalDocs", ); UNSUPPORTED

                // OperationId
                addString(operation, "operationId", routeAnnotation.operationId())

                // Parameters
                // ~ https://swagger.io/specification/#parameter-object
                val parameters = JsonArray()

                for (queryParameterAnnotation in routeAnnotation.queryParams()) {
                    parameters.add(fromParameter(QUERY, queryParameterAnnotation))
                }

                for (headerParameterAnnotation in routeAnnotation.headers()) {
                    parameters.add(fromParameter(HEADER, headerParameterAnnotation))
                }

                for (pathParameterAnnotation in routeAnnotation.pathParams()) {
                    parameters.add(fromParameter(PATH, pathParameterAnnotation))
                }

                for (cookieParameterAnnotation in routeAnnotation.cookies()) {
                    parameters.add(fromParameter(COOKIE, cookieParameterAnnotation))
                }

                operation.add("parameters", parameters)

                // RequestBody
                // ~ https://swagger.io/specification/#request-body-object
                val requestBodyAnnotation = routeAnnotation.requestBody()
                val requestBody = JsonObject()
                addString(requestBody, "description", requestBodyAnnotation.description())
                addContent(requestBody, requestBodyAnnotation.content())
                requestBody.addProperty("required", requestBodyAnnotation.required())
                operation.add("requestBody", requestBody)

                // Responses
                // ~ https://swagger.io/specification/#responses-object
                val responses = JsonObject()

                for (responseAnnotation in routeAnnotation.responses()) {
                    val response = JsonObject()
                    addString(response, "description", responseAnnotation.description())
                    addContent(response, responseAnnotation.content())
                    responses.add(responseAnnotation.status(), response)
                }

                requestBody.add("responses", responses)

                // Callbacks
                // ~ https://swagger.io/specification/#callback-object
                // operation.addProperty("callbacks, ); UNSUPPORTED

                // Deprecated
                operation.addProperty("deprecated", routeAnnotation.deprecated())

                // Security
                // ~ https://swagger.io/specification/#security-requirement-object
                val security = JsonObject()

                for (securityAnnotation in routeAnnotation.security()) {
                    val scopes = JsonArray()

                    for (scopeAnnotation in securityAnnotation.scopes()) {
                        scopes.add(scopeAnnotation)
                    }

                    security.add(securityAnnotation.name(), scopes)
                }

                operation.add("security", security)

                // servers
                path.add(method.name.toLowerCase(), operation)
            }
        }

        val components = JsonObject()
        val schemas = JsonObject()
        val generatedComponents: MutableSet<TypeMirror> = mutableSetOf()

        while (generatedComponents.size < componentReferences.size) {
            for (componentReference in componentReferences) {
                if (generatedComponents.contains(componentReference)) {
                    continue
                }

                val type = TypesUtils.getType(componentReference)
                val schema = JsonObject()
                val properties = JsonObject()

                for (property in type.element.enclosedElements) {
                    if (property is ExecutableElement && property.kind == METHOD) {
                        val simpleName = property.simpleName.toString()

                        val name = when {
                            simpleName.startsWith("get") -> simpleName.replaceFirst("get", "")
                            simpleName.startsWith("is") -> simpleName.replaceFirst("is", "")
                            else -> continue
                        }.decapitalize()

                        val propertyEntry = JsonObject()
                        addSchema(propertyEntry, property.returnType, false)
                        properties.add(name, propertyEntry)
                    }
                }

                schema.addProperty("type", "object")
                schema.add("properties", properties)
                schemas.add(type.getSimpleName(), schema)
                generatedComponents.add(componentReference)
            }
        }

        components.add("schemas", schemas)
        openApi.add("components", components)

        return gson.toJson(openApi)
    }

    // Parameter
    // https://swagger.io/specification/#parameter-object
    private fun fromParameter(`in`: In, parameterInstance: OpenApiParamInstance?): JsonObject {
        val parameter = JsonObject()
        addString(parameter, "name", parameterInstance!!.name())
        addString(parameter, "in", `in`.name.toLowerCase())
        addString(parameter, "description", parameterInstance.description())
        parameter.addProperty("required", parameterInstance.required())
        parameter.addProperty("deprecated", parameterInstance.deprecated())
        parameter.addProperty("allowEmptyValue", parameterInstance.allowEmptyValue())
        return parameter
    }

    private fun addContent(parent: JsonObject, annotations: List<OpenApiContentInstance>) {
        val requestBodyContent = JsonArray()

        for (contentAnnotation in annotations) {
            val contentEntry = JsonObject()

            if (contentAnnotation.from().toString() != NULL_CLASS::class.java.name) {
                addMediaType(contentEntry, contentAnnotation.type(), contentAnnotation.from(), contentAnnotation.isArray())
            }

            requestBodyContent.add(contentEntry)
        }

        parent.add("content", requestBodyContent)
    }

    private fun addSchema(schema: JsonObject, typeMirror: TypeMirror, isArray: Boolean) {
        val type = TypesUtils.getType(typeMirror)

        if (isArray || type.isArray()) {
            schema.addProperty("type", "array")
            val items = JsonObject()
            addType(items, typeMirror)
            schema.add("items", items)
        }
        else {
            addType(schema, typeMirror)
        }
    }

    private fun addType(parent: JsonObject, typeMirror: TypeMirror) {
        val type = TypesUtils.getType(typeMirror)
        val nonRefType = TypesUtils.NON_REF_TYPES[type.getSimpleName()]

        if (nonRefType == null) {
            componentReferences.add(typeMirror)
            parent.addProperty("\$ref", "#/components/schemas/${type.getSimpleName()}")
            return
        }

        parent.addProperty("type", nonRefType.type)
        parent.addProperty("format", nonRefType.format)
    }

    private fun addString(parent: JsonObject, key: String, value: String?) {
        if (NULL_STRING != value) {
            parent.addProperty(key, value)
        }
    }

    private fun addMediaType(parent: JsonObject, mimeType: String, type: TypeMirror, isArray: Boolean) {
        var detectedContentType = mimeType

        if (AUTODETECT == detectedContentType) {
            detectedContentType = TypesUtils.detectContentType(type)
        }

        val mediaType = JsonObject()
        val schema = JsonObject()
        addSchema(schema, type, isArray)
        mediaType.add("schema", schema)
        parent.add(detectedContentType, mediaType)
    }

}