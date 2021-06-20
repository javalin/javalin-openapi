package net.dzikoysk.openapi.processor

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.javalin.plugin.openapi.annotations.ContentType.AUTODETECT
import io.javalin.plugin.openapi.annotations.ContentType.JSON
import io.javalin.plugin.openapi.annotations.NULL_CLASS
import io.javalin.plugin.openapi.annotations.NULL_STRING
import net.dzikoysk.openapi.processor.annotations.OpenApiInstance
import net.dzikoysk.openapi.processor.annotations.OpenApiParamInstance
import net.dzikoysk.openapi.processor.annotations.OpenApiParamInstance.In
import net.dzikoysk.openapi.processor.annotations.OpenApiParamInstance.In.COOKIE
import net.dzikoysk.openapi.processor.annotations.OpenApiParamInstance.In.HEADER
import net.dzikoysk.openapi.processor.annotations.OpenApiParamInstance.In.PATH
import net.dzikoysk.openapi.processor.annotations.OpenApiParamInstance.In.QUERY
import net.dzikoysk.openapi.processor.utils.JsonUtils
import javax.annotation.processing.Messager
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic.Kind.WARNING

internal class OpenApiGenerator(private val messager: Messager) {

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    /**
     * Based on https://swagger.io/specification/
     *
     * @param annotations annotation instances to map
     * @return OpenApi JSON response
     */
    fun generate(annotations: Collection<OpenApiInstance?>?): JsonObject? {
        val openApi = JsonObject()
        openApi.addProperty("openapi", "3.0.3")

        // somehow fill info { description, version } properties
        val info = JsonObject()
        info.addProperty("title", "{openapi.title}")
        info.addProperty("description", "{openapi.description}")
        info.addProperty("version", "{openapi.version}")
        openApi.add("info", info)

        // initialize components
        val components = JsonObject()
        openApi.add("components", components)

        // fill paths
        val paths = JsonObject()
        openApi.add("paths", paths)
        val referenceId = 0
        val componentReferences: Map<Int, TypeMirror> = HashMap()
        for (routeAnnotation in annotations!!) {
            val path = JsonUtils.computeIfAbsent(paths, routeAnnotation!!.path()) { JsonObject() }

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
                val requestBodyContent = JsonArray()

                for (contentAnnotation in requestBodyAnnotation.content()) {
                    val contentEntry = JsonObject()

                    if (contentAnnotation.from().toString() != NULL_CLASS::class.java.name) {

                    }
                }

                requestBody.add("content", requestBodyContent)
                requestBody.addProperty("required", requestBodyAnnotation.required())
                operation.add("requestBody", requestBody)

                // Responses
                //
                for (responseAnnotation in routeAnnotation.responses()) {
                }

                // Callbacks
                //

                // Deprecated
                operation.addProperty("deprecated", routeAnnotation.deprecated())

                // security

                // servers
                path.add(method.name.toLowerCase(), operation)
            }
        }

        messager.printMessage(WARNING, gson.toJson(openApi))
        return null
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

    private fun toTypeObject(key: String, type: TypeMirror): JsonObject? {
        return null
    }

    private fun addString(`object`: JsonObject, key: String, value: String?) {
        if (NULL_STRING != value) {
            `object`.addProperty(key, value)
        }
    }

    private fun addContentType(`object`: JsonObject, key: String, value: String) {
        var value = value

        if (AUTODETECT != value) {
            value = JSON
        }

        `object`.addProperty(key, value)
    }
}