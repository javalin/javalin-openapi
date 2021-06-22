package net.dzikoysk.openapi;

import io.javalin.Javalin;
import net.dzikoysk.openapi.annotations.HttpMethod;
import net.dzikoysk.openapi.annotations.OpenApi;
import net.dzikoysk.openapi.annotations.OpenApiContent;
import net.dzikoysk.openapi.annotations.OpenApiIgnore;
import net.dzikoysk.openapi.annotations.OpenApiName;
import net.dzikoysk.openapi.annotations.OpenApiParam;
import net.dzikoysk.openapi.annotations.OpenApiPropertyType;
import net.dzikoysk.openapi.annotations.OpenApiRequestBody;
import net.dzikoysk.openapi.annotations.OpenApiResponse;
import net.dzikoysk.openapi.javalin.OpenApiConfiguration;
import net.dzikoysk.openapi.javalin.OpenApiPlugin;

import java.io.Serializable;
import java.util.UUID;

public final class AppTest {

    @OpenApi(
            path = "/main/{name}",
            operationId = "cli",
            method = HttpMethod.POST,
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
                    @OpenApiParam(name = "Authorization", description = "Alias and token provided as basic auth credentials", required = true, type = UUID.class)
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
            /*
            security = {
                    @OpenApiSecurity(name = "main_auth", scopes = { "read:tokens" })
            }
             */
    )
    public static void main(String[] args) {
        Javalin.create(config -> {
            OpenApiConfiguration openApiConfiguration = new OpenApiConfiguration();
            openApiConfiguration.setDocumentationPath("/swagger-docs");
            config.registerPlugin(new OpenApiPlugin(openApiConfiguration));
        })
        .start(80);
    }

    public static final class EntityDto implements Serializable {

        private final int status;
        private final String message;
        private final Foo foo;
        private Bar bar;

        public EntityDto(int status, String message, Foo foo, Bar bar) {
            this.status = status;
            this.message = message;
            this.foo = foo;
            this.bar = bar;
        }

        // should ignore
        public void setBar(Bar bar) {
            this.bar = bar;
        }

        // should by displayed as standard json section
        public Bar getBar() {
            return bar;
        }

        // should by displayed as string
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

    public static final class Foo {

        private String property;

        public String getProperty() {
            return property;
        }

    }

    public static final class Bar {

        private String property;

        public String getProperty() {
            return property;
        }

    }

}
