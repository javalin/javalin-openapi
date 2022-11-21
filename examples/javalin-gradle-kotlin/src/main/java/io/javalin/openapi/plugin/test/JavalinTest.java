package io.javalin.openapi.plugin.test;

import com.fasterxml.jackson.databind.node.TextNode;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.openapi.ApiKeyAuth;
import io.javalin.openapi.BasicAuth;
import io.javalin.openapi.BearerAuth;
import io.javalin.openapi.CookieAuth;
import io.javalin.openapi.Custom;
import io.javalin.openapi.CustomAnnotation;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.ImplicitFlow;
import io.javalin.openapi.JsonSchema;
import io.javalin.openapi.JsonSchemaLoader;
import io.javalin.openapi.JsonSchemaResource;
import io.javalin.openapi.OAuth2;
import io.javalin.openapi.OneOf;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiByFields;
import io.javalin.openapi.OpenApiContact;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiContentProperty;
import io.javalin.openapi.OpenApiExample;
import io.javalin.openapi.OpenApiIgnore;
import io.javalin.openapi.OpenApiInfo;
import io.javalin.openapi.OpenApiLicense;
import io.javalin.openapi.OpenApiName;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiPropertyType;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;
import io.javalin.openapi.OpenApiSecurity;
import io.javalin.openapi.OpenApiServer;
import io.javalin.openapi.OpenApiServerVariable;
import io.javalin.openapi.OpenID;
import io.javalin.openapi.Security;
import io.javalin.openapi.Visibility;
import io.javalin.openapi.plugin.OpenApiConfiguration;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.SecurityConfiguration;
import io.javalin.openapi.plugin.redoc.ReDocConfiguration;
import io.javalin.openapi.plugin.redoc.ReDocPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerConfiguration;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import lombok.Data;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static kotlin.annotation.AnnotationTarget.PROPERTY_GETTER;

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
            String deprecatedDocsPath = "/openapi.json";

            OpenApiContact openApiContact = new OpenApiContact();
            openApiContact.setName("API Support");
            openApiContact.setUrl("https://www.example.com/support");
            openApiContact.setEmail("support@example.com");

            OpenApiLicense openApiLicense = new OpenApiLicense();
            openApiLicense.setName("Apache 2.0");
            openApiLicense.setIdentifier("Apache-2.0");

            OpenApiInfo openApiInfo = new OpenApiInfo();
            openApiInfo.setTitle("Awesome App");
            openApiInfo.setSummary("App summary");
            openApiInfo.setDescription("App description goes right here");
            openApiInfo.setTermsOfService("https://example.com/tos");
            openApiInfo.setContact(openApiContact);
            openApiInfo.setLicense(openApiLicense);
            openApiInfo.setVersion("1.0.0");

            OpenApiServerVariable portServerVariable = new OpenApiServerVariable();
            portServerVariable.setValues(new String[] { "7070", "8080" });
            portServerVariable.setDefault("8080");
            portServerVariable.setDescription("Port of the server");

            OpenApiServerVariable basePathServerVariable = new OpenApiServerVariable();
            basePathServerVariable.setValues(new String[] { "v1" });
            basePathServerVariable.setDefault("v1");
            basePathServerVariable.setDescription("Base path of the server");

            OpenApiServer openApiServer = new OpenApiServer();
            openApiServer.setUrl("https://example.com:{port}/{basePath}");
            openApiServer.setDescription("Server description goes here");
            openApiServer.addVariable("port", portServerVariable);
            openApiServer.addVariable("basePath", basePathServerVariable);

            OpenApiServer[] servers = new OpenApiServer[] { openApiServer };

            OpenApiConfiguration openApiConfiguration = new OpenApiConfiguration();
            openApiConfiguration.setInfo(openApiInfo);
            openApiConfiguration.setServers(servers);
            openApiConfiguration.setDocumentationPath(deprecatedDocsPath); // by default it's /openapi

            // Based on official example: https://swagger.io/docs/specification/authentication/oauth2/
            openApiConfiguration.setSecurity(new SecurityConfiguration()
                .withSecurityScheme("BasicAuth", new BasicAuth())
                .withSecurityScheme("BearerAuth", new BearerAuth())
                .withSecurityScheme("ApiKeyAuth", new ApiKeyAuth())
                .withSecurityScheme("CookieAuth", new CookieAuth("JSESSIONID"))
                .withSecurityScheme("OpenID", new OpenID("https://example.com/.well-known/openid-configuration"))
                .withSecurityScheme("OAuth2", new OAuth2("This API uses OAuth 2 with the implicit grant flow.")
                    .withFlow(new ImplicitFlow("https://api.example.com/oauth2/authorize")
                        .withScope("read_pets", "read your pets")
                        .withScope("write_pets", "modify pets in your account")))
                .withSecurity(new Security("oauth2")
                    .withScope("write_pets")
                    .withScope("read_pets"))
            );

//            config.routing.contextPath = "/custom";

            openApiConfiguration.setDocumentProcessor(docs -> { // you can add whatever you want to this document using your favourite json api
                docs.set("test", new TextNode("Value"));
                return docs.toPrettyString();
            });
            config.plugins.register(new OpenApiPlugin(openApiConfiguration));

            SwaggerConfiguration swaggerConfiguration = new SwaggerConfiguration();
            swaggerConfiguration.setDocumentationPath(deprecatedDocsPath);
            config.plugins.register(new SwaggerPlugin(swaggerConfiguration));

            ReDocConfiguration reDocConfiguration = new ReDocConfiguration();
            reDocConfiguration.setDocumentationPath(deprecatedDocsPath);
            config.plugins.register(new ReDocPlugin(reDocConfiguration));

            for (JsonSchemaResource generatedJsonSchema : new JsonSchemaLoader().loadGeneratedSchemes()) {
                System.out.println(generatedJsonSchema.getName());
                System.out.println(generatedJsonSchema.getContentAsString());
            }
        })
        .start(8080);
    }

    private static final String ROUTE = "/main/{name}";

    @OpenApi(
        path = ROUTE,
        methods = HttpMethod.POST,
        operationId = "cli",
        summary = "Remote command execution",
        description = "Execute command using POST request. The commands are the same as in the console and can be listed using the 'help' command.",
        tags = { "Cli" },
        security = {
            @OpenApiSecurity(name = "BasicAuth")
        },
        requestBody = @OpenApiRequestBody(
            description = "Supports multiple request bodies",
            content = {
                @OpenApiContent(from = String.class), // simple type
                @OpenApiContent(from = LombokEntity.class), // lombok
                @OpenApiContent(from = KotlinEntity.class), // kotlin
                @OpenApiContent(from = EntityWithGenericType.class), // generics
                @OpenApiContent(from = RecordEntity.class), // record class
                @OpenApiContent(from = DtoWithFields.class), // map only fields
                @OpenApiContent(from = EnumEntity.class), // enum
            }
        ),
        headers = {
            //@OpenApiParam(name = "Authorization", description = "Alias and token provided as basic auth credentials", required = true, type = UUID.class),
            @OpenApiParam(name = "Optional"),
            @OpenApiParam(name = "X-Rick", example = "Rolled"),
            @OpenApiParam(name = "X-SomeNumber", required = true, type = Integer.class, example = "500")
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
            }),
            @OpenApiResponse(status = "500") // fill description with HttpStatus message
        }
    )
    @OpenApi(
        path = "/repeatable",
        methods = { HttpMethod.POST },
        versions = "v1",
        requestBody = @OpenApiRequestBody(
            description = "Complex bodies",
            content = {
                @OpenApiContent(from = EntityDto[].class), // array
                @OpenApiContent(from = File.class), // file
                @OpenApiContent(type = "application/json"), // empty
                @OpenApiContent(mimeType = "image/png", type = "string", format = "base64"), // single file upload,
                @OpenApiContent(mimeType = "multipart/form-data", properties = {
                    @OpenApiContentProperty(name = "form-element", type = "integer"), // random element in form-data
                    @OpenApiContentProperty(name = "file-name", isArray = true, type = "string", format = "base64") // multi-file upload
                })
            }
        )
    )
    @Override
    public void handle(@NotNull Context ctx) { }

    @OpenApi(
        path = "/standalone",
        methods = HttpMethod.DELETE,
        versions = "v2"
    )
    static final class EntityDto implements Serializable {

        private final int status;
        private final @NotNull String message;
        private final @NotNull String timestamp;
        private final @NotNull Foo foo;
        private final @NotNull List<Foo> foos;
        private final @NotNull Map<String, Bar> bars = new HashMap<>();
        private final @NotNull EnumEntity enumEntity = EnumEntity.TWO;
        private Bar bar;

        public EntityDto(int status, @NotNull String message, @NotNull Foo foo, @NotNull List<Foo> foos, @Nullable Bar bar) {
            this.status = status;
            this.message = message;
            this.foo = foo;
            this.foos = foos;
            this.bar = bar;
            this.timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        }

        // should be ignored
        public void setBar(Bar bar) {
            this.bar = bar;
        }

        // should be displayed as standard json section
        public Bar getBar() {
            return bar;
        }

        // should be represented as object
        public @NotNull Map<String, Bar> getBars() {
            return bars;
        }

        // should be represented by array
        public @NotNull List<Foo> getFoos() {
            return foos;
        }

        // should handle fallback to object type
        @SuppressWarnings("rawtypes")
        public List getUnknowns() {
            return foos;
        }

        // should display enum
        public @NotNull EnumEntity getEnumEntity() {
            return enumEntity;
        }

        // should be displayed as string
        @OpenApiPropertyType(definedBy = String.class)
        public @NotNull Foo getFoo() {
            return foo;
        }

        // HiddenEntity with @OpenApiPropertyType should be displayed as string
        public HiddenEntity getHiddenEntity() {
            return new HiddenEntity();
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

        // should be ignored
        @OpenApiIgnore
        public String getFormattedMessage() {
            return status + message;
        }

        // should contain example
        @OpenApiExample("2022-08-14T21:13:03.546Z")
        public @NotNull String getTimestamp() {
            return timestamp;
        }

        // should contain example for primitive types, SwaggerUI will automatically display this as an Integer
        @OpenApiExample("5050")
        public int getVeryImportantNumber() {
            return status + 1;
        }

        // should support @Custom from JsonSchema
        @Custom(name = "description", value = "Custom property")
        public String getCustom() {
            return "";
        }

        // should be displayed as string
        public ObjectId getObjectId() {
            return new ObjectId();
        }

        // static should be ignored
        public static String getStatic() {
            return "static";
        }

    }

    static final class Foo {

        private String property;
        private String link;

        public String getProperty() {
            return property;
        }

        @OpenApiExample("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        public String getLink() {
            return link;
        }

    }

    // subtype
    static final class Bar {

        private String property;

        public String getProperty() {
            return property;
        }

    }

    // should work with properties generated by another annotation processor
    @Data
    static final class LombokEntity {
        private String property;
    }

    // should pick upper/lower bound type for generics
    @SuppressWarnings("ClassCanBeRecord")
    static final class EntityWithGenericType<V extends Bar> {

        private final V value;

        public EntityWithGenericType(V value) {
            this.value = value;
        }

        public V getValue() {
            return value;
        }

    }

    // should match properties of record class
    record RecordEntity(String name, String surname) {}

    // should query fields
    @OpenApiByFields(Visibility.PROTECTED) // by default: PUBLIC
    static final class DtoWithFields {
        public String publicName;
        String defaultName;
        protected String protectedName;
        private String privateName;
    }

    enum EnumEntity {
        ONE,
        TWO
    }

    @JsonSchema
    static final class JsonSchemaEntity {

        public List<EntityDto> getEntities() {
            return Collections.emptyList();
        }

        @OneOf({ Panda.class, Cat.class })
        public Animal getAnimal() {
            return new Panda();
        }

    }

    interface Animal {

        default boolean isAnimal() {
            return true;
        }

    }

    static class Panda implements Animal {

        @Custom(name = "title", value = "Panda")
        @Custom(name = "description", value = "Only Panda")
        public boolean isPanda() {
            return true;
        }

    }

    @Target({ElementType.METHOD, ElementType.TYPE})
    @CustomAnnotation
    @interface Description {
        String title();
        String description();
        int statusCode();
    }

    static class Cat implements Animal {

        @Description(title = "Cat", description = "Is it cat?", statusCode = 200)
        public boolean isCat() {
            return true;
        }

    }

    @OpenApiPropertyType(definedBy = String.class)
    static class HiddenEntity {

    }

}
