package io.javalin.openapi.plugin.test;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiIgnore;
import io.javalin.openapi.OpenApiName;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiPropertyType;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;
import io.javalin.openapi.plugin.OpenApiConfiguration;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.redoc.ReDocConfiguration;
import io.javalin.openapi.plugin.redoc.ReDocPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerConfiguration;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import org.jetbrains.annotations.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * Starts Javalin server with OpenAPI plugin
 */
public final class JavalinTest implements Handler {

    /**
     * Runs server on localhost:8080
     *
     * @param args args
     */
    public static void main(String[] args) {
        Javalin.create(config -> {
            String deprecatedDocsPath = "/swagger-docs";

            OpenApiConfiguration openApiConfiguration = new OpenApiConfiguration();
            openApiConfiguration.setTitle("AwesomeApp");
            openApiConfiguration.setDocumentationPath(deprecatedDocsPath); // by default it's /openapi
            openApiConfiguration.setDocumentProcessor(docs -> docs); // you can add whatever you want to this document using your favourite json api
            config.registerPlugin(new OpenApiPlugin(openApiConfiguration));

            SwaggerConfiguration swaggerConfiguration = new SwaggerConfiguration();
            swaggerConfiguration.setDocumentationPath(deprecatedDocsPath);
            config.registerPlugin(new SwaggerPlugin(swaggerConfiguration));

            ReDocConfiguration reDocConfiguration = new ReDocConfiguration();
            reDocConfiguration.setDocumentationPath(deprecatedDocsPath);
            config.registerPlugin(new ReDocPlugin(reDocConfiguration));
        })
        .start(8080);
    }

    private static final String ROUTE = "/main/{name}";

    @Override
    @OpenApi(
            path = ROUTE,
            operationId = "cli",
            methods = HttpMethod.POST,
            summary = "Remote command execution",
            description = "Execute command using POST request. The commands are the same as in the console and can be listed using the 'help' command.",
            tags = { "Cli" },
            requestBody = @OpenApiRequestBody(
                    content = {
                            @OpenApiContent(from = String.class),
                            @OpenApiContent(from = EntityDto[].class)
                    }
            ),
            headers = {
                    @OpenApiParam(name = "Authorization", description = "Alias and token provided as basic auth credentials", required = true, type = UUID.class),
                    @OpenApiParam(name = "Optional")
            },
            pathParams = {
                    @OpenApiParam(name = "name", description = "Name", required = true, type = UUID.class)
            },
            responses = {
                    @OpenApiResponse(status = "200", description = "Status of the executed command", content = {
                            @OpenApiContent(from = EntityDto[].class)
                    }),
                    @OpenApiResponse(
                            status = "400",
                            description = "Error message related to the invalid command format (0 < command length < " + 10 + ")",
                            content = @OpenApiContent(from = EntityDto[].class)
                    ),
                    @OpenApiResponse(status = "401", description = "Error message related to the unauthorized access", content = {
                            @OpenApiContent(from = EntityDto[].class)
                    })
            }
    )
    public void handle(@NotNull Context ctx) { }

    static final class EntityDto implements Serializable {

        private final int status;
        private final String message;
        private final Foo foo;
        private final List<Foo> foos;
        private Bar bar;

        public EntityDto(int status, String message, Foo foo, List<Foo> foos, Bar bar) {
            this.status = status;
            this.message = message;
            this.foo = foo;
            this.foos = foos;
            this.bar = bar;
        }

        // should ignore
        public void setBar(Bar bar) {
            this.bar = bar;
        }

        // should be displayed as standard json section
        public Bar getBar() {
            return bar;
        }

        // should be represented by array
        public List<Foo> getFoos() {
            return foos;
        }

        // should be displayed as string
        @OpenApiPropertyType(definedBy = String.class)
        public Foo getFoo() {
            return foo;
        }

        // should support primitive types
        public int getStatus() {
            return status;
        }

        // should rename
        @OpenApiName("message")
        public String getMessageValue() {
            return message;
        }

        // should ignore
        @OpenApiIgnore
        public String getFormattedMessage() {
            return status + message;
        }

    }

    static final class Foo {

        private String property;

        public String getProperty() {
            return property;
        }

    }

    static final class Bar {

        private String property;

        public String getProperty() {
            return property;
        }

    }

}
