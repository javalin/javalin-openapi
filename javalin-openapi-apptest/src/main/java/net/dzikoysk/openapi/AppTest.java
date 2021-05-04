package net.dzikoysk.openapi;

import io.javalin.plugin.openapi.annotations.HttpMethod;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiContent;
import io.javalin.plugin.openapi.annotations.OpenApiParam;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;

public final class AppTest {

    @OpenApi(
            path = "/main",
            operationId = "cli",
            method = HttpMethod.POST,
            summary = "Remote command execution",
            description = "Execute command using POST request. The commands are the same as in the console and can be listed using the 'help' command.",
            tags = { "Cli" },
            headers = {
                    @OpenApiParam(name = "Authorization", description = "Alias and token provided as basic auth credentials", required = true)
            },
            responses = {
                    @OpenApiResponse(status = "200", description = "Status of the executed command", content = {
                            @OpenApiContent(from = EntityDto.class)
                    }),
                    @OpenApiResponse(
                            status = "400",
                            description = "Error message related to the invalid command format (0 < command length < " + 10 + ")",
                            content = @OpenApiContent(from = EntityDto.class)
                    ),
                    @OpenApiResponse(status = "401", description = "Error message related to the unauthorized access", content = {
                            @OpenApiContent(from = EntityDto.class)
                    })
            }
    )
    public static void main(String[] args) {
        System.out.println("mainxx");
    }

}
