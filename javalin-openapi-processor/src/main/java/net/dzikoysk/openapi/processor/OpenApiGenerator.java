package net.dzikoysk.openapi.processor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.javalin.plugin.openapi.annotations.HttpMethod;
import net.dzikoysk.openapi.processor.annotations.OpenApiInstance;
import net.dzikoysk.openapi.processor.annotations.OpenApiParamInstance;
import net.dzikoysk.openapi.processor.annotations.OpenApiParamInstance.In;
import net.dzikoysk.openapi.processor.annotations.OpenApiRequestBodyInstance;
import net.dzikoysk.openapi.processor.annotations.OpenApiResponseInstance;
import net.dzikoysk.openapi.processor.utils.JsonUtils;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic.Kind;
import java.util.Collection;
import java.util.function.Function;

final class OpenApiGenerator {

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private final Messager messager;

    OpenApiGenerator(Messager messager) {
        this.messager = messager;
    }

    /**
     * Based on https://swagger.io/specification/
     *
     * @param annotations annotation instances to map
     * @return OpenApi JSON response
     */
    JsonObject generate(Collection<OpenApiInstance> annotations) {
        JsonObject openApi = new JsonObject();
        openApi.addProperty("openapi", "3.0.3");

        // somehow fill info { description, version } properties
        JsonObject info = new JsonObject();
        info.addProperty("title", "{openapi.title}");
        info.addProperty("description", "{openapi.description}");
        info.addProperty("version", "{openapi.version}");
        openApi.add("info", info);

        // initialize components
        JsonObject components = new JsonObject();
        openApi.add("components", components);

        // fill paths
        JsonObject paths = new JsonObject();
        openApi.add("paths", paths);

        for (OpenApiInstance routeAnnotation : annotations) {
            JsonObject path = JsonUtils.computeIfAbsent(paths, routeAnnotation.path(), JsonObject::new);

            // https://swagger.io/specification/#paths-object
            for (HttpMethod method : routeAnnotation.methods()) {
                JsonObject operation = new JsonObject();

                // General
                operation.add("tags", JsonUtils.toArray(routeAnnotation.tags(), Function.identity()));
                operation.addProperty("summary", routeAnnotation.summary());
                operation.addProperty("description", routeAnnotation.description());

                // ExternalDocs
                // ~ https://swagger.io/specification/#external-documentation-object
                // operation.addProperty("externalDocs", ); UNSUPPORTED

                // OperationId
                operation.addProperty("operationId", routeAnnotation.operationId());

                // Parameters
                // ~ https://swagger.io/specification/#parameter-object
                JsonArray parameters = new JsonArray();

                for (OpenApiParamInstance queryParameterAnnotation : routeAnnotation.queryParams()) {
                    parameters.add(fromParameter(In.QUERY, queryParameterAnnotation));
                }

                for (OpenApiParamInstance headerParameterAnnotation : routeAnnotation.headers()) {
                    parameters.add(fromParameter(In.HEADER, headerParameterAnnotation));
                }

                for (OpenApiParamInstance pathParameterAnnotation : routeAnnotation.pathParams()) {
                    parameters.add(fromParameter(In.PATH, pathParameterAnnotation));
                }

                for (OpenApiParamInstance cookieParameterAnnotation : routeAnnotation.cookies()) {
                    parameters.add(fromParameter(In.COOKIE, cookieParameterAnnotation));
                }

                operation.add("parameters", parameters);

                // RequestBody
                //

                OpenApiRequestBodyInstance requestBodyAnnotation = routeAnnotation.requestBody();

                // Responses
                //

                for (OpenApiResponseInstance responseAnnotation : routeAnnotation.responses()) {

                }

                // Callbacks
                //

                // Deprecated
                operation.addProperty("deprecated", routeAnnotation.deprecated());

                // security

                // servers

                path.add(method.name().toLowerCase(), operation);
            }
        }

        messager.printMessage(Kind.WARNING, gson.toJson(openApi));

        return null;
    }

    // Parameter
    // https://swagger.io/specification/#parameter-object
    private JsonObject fromParameter(In in, OpenApiParamInstance parameterInstance) {
        JsonObject parameter = new JsonObject();
        parameter.addProperty("name", parameterInstance.name());
        parameter.addProperty("in", in.name().toLowerCase());
        parameter.addProperty("description", parameterInstance.description());
        parameter.addProperty("required", parameterInstance.required());
        parameter.addProperty("deprecated", parameterInstance.deprecated());
        parameter.addProperty("allowEmptyValue", parameterInstance.allowEmptyValue());
        return parameter;
    }

}
