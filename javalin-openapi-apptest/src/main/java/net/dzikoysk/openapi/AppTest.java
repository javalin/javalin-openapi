package net.dzikoysk.openapi;

import io.javalin.plugin.openapi.annotations.HttpMethod;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiContent;
import io.javalin.plugin.openapi.annotations.OpenApiParam;
import io.javalin.plugin.openapi.annotations.OpenApiRequestBody;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;
import io.javalin.plugin.openapi.annotations.OpenApiSecurity;

import java.io.Serializable;
import java.util.UUID;

public final class AppTest {

    @OpenApi(
            path = "/main/:name",
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
                    @OpenApiParam(name = "Authorization", description = "Alias and token provided as basic auth credentials", required = true, type = String.class)
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
            },
            security = {
                    @OpenApiSecurity(name = "main_auth", scopes = { "read:tokens" })
            }
    )
    public static void main(String[] args) {
        System.out.println("main");
    }

    public static final class EntityDto implements Serializable {

        private final int status;
        private final String message;
        private final Foo foo;
        private final Bar bar;

        public EntityDto(int status, String message, Foo foo, Bar bar) {
            this.status = status;
            this.message = message;
            this.foo = foo;
            this.bar = bar;
        }

        public Bar getBar() {
            return bar;
        }

        public Foo getFoo() {
            return foo;
        }

        public int getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
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
