package io.javalin.openapi.processor

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.javalin.openapi.ContentType.AUTODETECT
import io.javalin.openapi.NULL_CLASS
import io.javalin.openapi.NULL_STRING
import io.javalin.openapi.OpenApiExample
import io.javalin.openapi.OpenApiIgnore
import io.javalin.openapi.OpenApiName
import io.javalin.openapi.processor.annotations.OpenApiContentInstance
import io.javalin.openapi.processor.annotations.OpenApiInstance
import io.javalin.openapi.processor.annotations.OpenApiParamInstance
import io.javalin.openapi.processor.annotations.OpenApiParamInstance.In
import io.javalin.openapi.processor.annotations.OpenApiParamInstance.In.COOKIE
import io.javalin.openapi.processor.annotations.OpenApiParamInstance.In.FORM_DATA
import io.javalin.openapi.processor.annotations.OpenApiParamInstance.In.HEADER
import io.javalin.openapi.processor.annotations.OpenApiParamInstance.In.PATH
import io.javalin.openapi.processor.annotations.OpenApiParamInstance.In.QUERY
import io.javalin.openapi.processor.annotations.OpenApiPropertyTypeInstance
import io.javalin.openapi.processor.utils.JsonUtils
import io.javalin.openapi.processor.utils.TypesUtils
import io.javalin.openapi.processor.utils.TypesUtils.DataModel
import io.javalin.openapi.processor.utils.TypesUtils.DataType.ARRAY
import io.javalin.openapi.processor.utils.TypesUtils.DataType.DICTIONARY
import io.javalin.openapi.processor.utils.TypesUtils.toModel
import javax.lang.model.element.ElementKind.METHOD
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.TypeMirror

internal class OpenApiGenerator {

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

        // fill info
        val info = JsonObject()
        info.addProperty("title", "")
        info.addProperty("version", "")
        openApi.add("info", info)

        // fill paths
        val paths = JsonObject()
        openApi.add("paths", paths)

        for (routeAnnotation in annotations) {
            if (routeAnnotation.ignore()) {
                continue
            }

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

                for (formParameterAnnotation in routeAnnotation.formParams()) {
                    parameters.add(fromParameter(FORM_DATA, formParameterAnnotation))
                }

                operation.add("parameters", parameters)

                // RequestBody
                // ~ https://swagger.io/specification/#request-body-object
                val requestBodyAnnotation = routeAnnotation.requestBody()
                val requestBody = JsonObject()
                addString(requestBody, "description", requestBodyAnnotation.description())
                addContent(requestBody, requestBodyAnnotation.content())
                if (requestBody.size() > 0) {
                    operation.add("requestBody", requestBody)
                }
                requestBody.addProperty("required", requestBodyAnnotation.required())

                // Responses
                // ~ https://swagger.io/specification/#responses-object
                val responses = JsonObject()

                for (responseAnnotation in routeAnnotation.responses()) {
                    val response = JsonObject()
                    addString(response, "description", responseAnnotation.description())
                    addContent(response, responseAnnotation.content())
                    responses.add(responseAnnotation.status(), response)
                }

                operation.add("responses", responses)

                // Callbacks
                // ~ https://swagger.io/specification/#callback-object
                // operation.addProperty("callbacks, ); UNSUPPORTED

                // Deprecated
                operation.addProperty("deprecated", routeAnnotation.deprecated())

                // Security
                // ~ https://swagger.io/specification/#security-requirement-object
                val security = JsonArray()

                for (securityAnnotation in routeAnnotation.security()) {
                    val securityEntry = JsonObject()
                    val scopes = JsonArray()

                    for (scopeAnnotation in securityAnnotation.scopes()) {
                        scopes.add(scopeAnnotation)
                    }

                    securityEntry.add(securityAnnotation.name(), scopes)
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

                val schema = JsonObject()
                val properties = JsonObject()
                val requiredProperties = mutableListOf<String>()

                for (property in type.sourceElement.enclosedElements) {
                    if (property is ExecutableElement && property.kind == METHOD) {
                        if (property.getAnnotation(OpenApiIgnore::class.java) != null) {
                            continue
                        }

                        val simpleName = property.simpleName.toString()
                        val customName = property.getAnnotation(OpenApiName::class.java)

                        val name = when {
                            customName != null -> customName.value
                            simpleName.startsWith("get") -> simpleName.replaceFirst("get", "").replaceFirstChar { it.lowercase() }
                            simpleName.startsWith("is") -> simpleName.replaceFirst("is", "").replaceFirstChar { it.lowercase() }
                            else -> continue
                        }

                        val propertyType = property.annotationMirrors
                            .firstOrNull { it.annotationType.asElement().simpleName.contentEquals("OpenApiPropertyType") }
                            ?.let { OpenApiPropertyTypeInstance(it).definedBy() }
                            ?: property.returnType

                        val exampleProperty = property.getAnnotation(OpenApiExample::class.java)
                            ?.value

                        if (propertyType.kind.isPrimitive || property.annotationMirrors.any { it.annotationType.asElement().simpleName.contentEquals("NotNull") }) {
                            requiredProperties.add(name)
                        }

                        val propertyEntry = JsonObject()
                        addSchema(propertyEntry, propertyType, false, exampleProperty)
                        properties.add(name, propertyEntry)
                    }
                }

                schema.addProperty("type", "object")
                schema.add("properties", properties)
                schemas.add(type.simpleName, schema)

                if (requiredProperties.isNotEmpty()) {
                    val required = JsonArray()
                    requiredProperties.forEach { required.add(it) }
                    schema.add("required", required)
                }

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
        addString(parameter, "in", `in`.identifier)
        addString(parameter, "description", parameterInstance.description())
        parameter.addProperty("required", parameterInstance.required())
        parameter.addProperty("deprecated", parameterInstance.deprecated())
        parameter.addProperty("allowEmptyValue", parameterInstance.allowEmptyValue())

        val schema = JsonObject()
        addSchema(schema, parameterInstance.type() /*OpenApiAnnotationProcessor.elements.getTypeElement(String::class.java.name).asType() */, false)
        parameterInstance.example()
            .takeIf { it.isNotEmpty() }
            .let { schema.addProperty("example", it) }
        parameter.add("schema", schema)

        return parameter
    }

    private fun addContent(parent: JsonObject, annotations: List<OpenApiContentInstance>) {
        val requestBodyContent = JsonObject()

        for (contentAnnotation in annotations) {
            val from = contentAnnotation.from()
            val type = contentAnnotation.type()
            val format = contentAnnotation.format().takeIf { it != NULL_STRING }
            val properties = contentAnnotation.properties()
            var mimeType = contentAnnotation.mimeType()

            if (AUTODETECT == mimeType) {
                mimeType = TypesUtils.detectContentType(contentAnnotation.from())
            }

            val mediaType = JsonObject()
            val schema = JsonObject()

            when {
                properties.isEmpty() && from.toString() != NULL_CLASS::class.java.name -> addSchema(schema, from, false)
                properties.isEmpty() -> {
                    schema.addProperty("type", type)
                    format?.also { schema.addProperty("format", it) }
                }
                else -> {
                    val propertiesSchema = JsonObject()
                    schema.addProperty("type", "object")

                    for (contentProperty in properties) {
                        val propertyScheme = JsonObject()
                        val propertyFormat = contentProperty.format().takeIf { it != NULL_STRING }

                        if (contentProperty.isArray()) {
                            propertyScheme.addProperty("type", "array")

                            val items = JsonObject()
                            items.addProperty("type", contentProperty.type())
                            propertyFormat?.let { items.addProperty("format", it) }
                            propertyScheme.add("items", items)
                        } else {
                            propertyScheme.addProperty("type", contentProperty.type())
                            propertyFormat?.let { propertyScheme.addProperty("format", it) }
                        }

                        propertiesSchema.add(contentProperty.name(), propertyScheme)
                    }

                    schema.add("properties", propertiesSchema)
                }
            }

            mediaType.add("schema", schema)
            requestBodyContent.add(mimeType, mediaType)
        }

        if (requestBodyContent.size() > 0) {
            parent.add("content", requestBodyContent)
        }
    }

    private fun addSchema(schema: JsonObject, typeMirror: TypeMirror, isArray: Boolean, exampleValue: String? = null) {
        val model = typeMirror.toModel() ?: return

        when {
            (model.type == ARRAY || isArray) && model.simpleName == "Byte" -> {
                schema.addProperty("type", "string")
                schema.addProperty("format", "binary")
            }
            isArray || model.type == ARRAY -> {
                schema.addProperty("type", "array")
                val items = JsonObject()
                addType(items, model)
                schema.add("items", items)
            }
            model.type == DICTIONARY -> {
                schema.addProperty("type", "object")
                val additionalProperties = JsonObject()
                addType(additionalProperties, model.generics.get(1))
                schema.add("additionalProperties", additionalProperties)
            }
            else -> addType(schema, model)
        }

        if (exampleValue != null) {
            schema.addProperty("example", exampleValue)
        }
    }

    private fun addType(parent: JsonObject, model: DataModel) {
        val nonRefType = TypesUtils.NON_REF_TYPES[model.simpleName]

        if (nonRefType == null) {
            componentReferences.add(model.typeMirror)
            parent.addProperty("\$ref", "#/components/schemas/${model.simpleName}")
            return
        }

        parent.addProperty("type", nonRefType.type)

        nonRefType.format
            .takeIf { it.isNotEmpty() }
            ?.also { parent.addProperty("format", it) }
    }

    private fun addString(parent: JsonObject, key: String, value: String?) {
        if (NULL_STRING != value) {
            parent.addProperty(key, value)
        }
    }

}