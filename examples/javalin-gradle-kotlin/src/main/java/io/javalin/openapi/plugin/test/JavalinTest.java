package io.javalin.openapi.plugin.test;

import com.fasterxml.jackson.databind.node.TextNode;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.openapi.*;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.redoc.ReDocPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import io.javalin.security.RouteRole;
import lombok.Data;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.validation.constraints.NotEmpty;
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

/**
 * Starts Javalin server with OpenAPI plugin
 */
@SuppressWarnings({"unused", "LombokGetterMayBeUsed", "LombokSetterMayBeUsed", "ProtectedMemberInFinalClass", "InnerClassMayBeStatic"})
public final class JavalinTest implements Handler {

    enum Rules implements RouteRole {
        ANONYMOUS,
        USER,
    }

    /**
     * Runs server on localhost:8080
     *
     * @param args args
     */
    public static void main(String[] args) {
        Javalin.createAndStart(config -> {
            // config.routing.contextPath = "/custom";
            String deprecatedDocsPath = "/api/openapi.json"; // by default it's /openapi

            config.registerPlugin(new OpenApiPlugin(openApiConfig ->
                openApiConfig
                    .withDocumentationPath(deprecatedDocsPath)
                    .withRoles(Rules.ANONYMOUS)
                    .withPrettyOutput()
                    .withDefinitionConfiguration((version, openApiDefinition) ->
                        openApiDefinition
                            .withInfo(openApiInfo ->
                                openApiInfo
                                    .description("App description goes right here")
                                    .termsOfService("https://example.com/tos")
                                    .contact("API Support", "https://www.example.com/support", "support@example.com")
                                    .license("Apache 2.0", "https://www.apache.org/licenses/", "Apache-2.0")
                            )
                            .withServer(openApiServer ->
                                openApiServer
                                    .description("Server description goes here")
                                    .url("http://localhost:{port}{basePath}/" + version + "/")
                                    .variable("port", "Server's port", "8080", "8080", "7070")
                                    .variable("basePath", "Base path of the server", "", "", "/v1")
                            )
                            // Based on official example: https://swagger.io/docs/specification/authentication/oauth2/
                            .withSecurity(openApiSecurity ->
                                openApiSecurity
                                    .withBasicAuth()
                                    .withBearerAuth()
                                    .withApiKeyAuth("ApiKeyAuth", "X-Api-Key")
                                    .withCookieAuth("CookieAuth", "JSESSIONID")
                                    .withOpenID("OpenID", "https://example.com/.well-known/openid-configuration")
                                    .withOAuth2("OAuth2", "This API uses OAuth 2 with the implicit grant flow.", oauth2 ->
                                        oauth2
                                            .withClientCredentials("https://api.example.com/credentials/authorize")
                                            .withImplicitFlow("https://api.example.com/oauth2/authorize", flow ->
                                                flow
                                                    .withScope("read_pets", "read your pets")
                                                    .withScope("write_pets", "modify pets in your account")
                                            )
                                    )
                                    .withGlobalSecurity("OAuth2", globalSecurity ->
                                        globalSecurity
                                            .withScope("write_pets")
                                            .withScope("read_pets")
                                    )
                                    .withGlobalSecurity("BearerAuth")
                            )
                            .withDefinitionProcessor(content -> { // you can add whatever you want to this document using your favourite json api
                                content.set("test", new TextNode("Value"));
                                return content.toPrettyString();
                            })
                    )));

            config.registerPlugin(new SwaggerPlugin(swaggerConfiguration -> {
                swaggerConfiguration.setDocumentationPath(deprecatedDocsPath);
            }));

            config.registerPlugin(new ReDocPlugin(reDocConfiguration -> {
                reDocConfiguration.setDocumentationPath(deprecatedDocsPath);
            }));

            for (JsonSchemaResource generatedJsonSchema : new JsonSchemaLoader().loadGeneratedSchemes()) {
                System.out.println(generatedJsonSchema.getName());
                System.out.println(generatedJsonSchema.getContentAsString());
            }
        });
    }

    @OpenApi(
        path = "/main/{name}",
        methods = HttpMethod.POST,
        operationId = "cli",
        summary = "Remote command execution",
        description = "Execute command using POST request. The commands are the same as in the console and can be listed using the 'help' command.",
        tags = { "Default", "Cli" },
        security = {
            @OpenApiSecurity(name = "BasicAuth")
        },
        headers = {
            @OpenApiParam(name = "Authorization", description = "Alias and token provided as basic auth credentials", required = true, type = UUID.class),
            @OpenApiParam(name = "Optional"),
            @OpenApiParam(name = "X-Rick", example = "Rolled"),
            @OpenApiParam(name = "X-SomeNumber", required = true, type = Integer.class, example = "500")
        },
        pathParams = {
            @OpenApiParam(name = "name", description = "Name", required = true, type = UUID.class)
        },
        queryParams = {
            @OpenApiParam(name = "query", description = "Some query", required = true, type = Integer.class)
        },
        requestBody = @OpenApiRequestBody(
            description = "Supports multiple request bodies",
            content = {
                @OpenApiContent(from = String.class, example = "value"), // simple type
                @OpenApiContent(from = String[].class, example = "value"), // array of simple types
                @OpenApiContent( // map of simple types
                    mimeType = "application/map-string-string",
                    additionalProperties = @OpenApiAdditionalContent(
                        from = String.class,
                        exampleObjects = {
                            @OpenApiExampleProperty(name = "en", value = "hey"),
                            @OpenApiExampleProperty(name = "pl", value = "hejka tu lenka"),
                        }
                    )
                ),
                @OpenApiContent( // map of complex types
                    mimeType = "application/map-string-object",
                    additionalProperties = @OpenApiAdditionalContent(from = Foo.class)
                ),
                @OpenApiContent(from = KotlinEntity.class, mimeType = "app/barbie", exampleObjects = {
                    @OpenApiExampleProperty(name = "name", value = "Margot Robbie")
                }), // kotlin
                @OpenApiContent(from = LombokEntity.class, mimeType = "app/lombok"), // lombok
                @OpenApiContent(from = EntityWithGenericType.class), // generics
                @OpenApiContent(from = RecordEntity.class, mimeType = "app/record"), // record class
                @OpenApiContent(from = DtoWithFields.class, mimeType = "app/dto-fields"), // map only fields
                @OpenApiContent(from = DtoWithFieldsAndMethods.class, mimeType = "app/dto-fields-and-methods"), // map fields and methods
                @OpenApiContent(from = EnumEntity.class, mimeType = "app/enum"), // enum,
                @OpenApiContent(from = CustomNameEntity.class, mimeType = "app/custom-name-entity") // custom name
            }
        ),
        responses = {
            @OpenApiResponse(status = "200", description = "Status of the executed command", content = {
                @OpenApiContent(from = String.class, example = "Value"),
                @OpenApiContent(from = EntityDto[].class)
            }),
            @OpenApiResponse(
                status = "400",
                description = "Error message related to the invalid command format (0 < command length < " + 10 + ")",
                content = @OpenApiContent(from = EntityDto[].class),
                headers = {
                    @OpenApiParam(name = "X-Error-Message", description = "Error message", type = String.class)
                }
            ),
            @OpenApiResponse(status = "401", description = "Error message related to the unauthorized access", content = {
                @OpenApiContent(from = EntityDto[].class, exampleObjects = {
                    @OpenApiExampleProperty(name = "error", value = "ERROR-CODE-401"),
                })
            }),
            @OpenApiResponse(status = "500") // fill description with HttpStatus message
        },
        callbacks = {
            @OpenApiCallback(
                name = "onData",
                url = "{$request.query.callbackUrl}/data",
                method = HttpMethod.GET,
                requestBody = @OpenApiRequestBody(
                    description = "Callback request body",
                    content = @OpenApiContent(from = String.class)
                ),
                responses = {
                    @OpenApiResponse(
                        status = "200",
                        description = "Callback response",
                        content = { @OpenApiContent(from = String.class) }
                    )
                }
            ),
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
                @OpenApiContent(), // empty
                @OpenApiContent(mimeType = "image/png", type = "string", format = "base64"), // single file upload,
                @OpenApiContent(mimeType = "multipart/form-data", properties = {
                    @OpenApiContentProperty(name = "form-element", type = "integer"), // random element in form-data
                    @OpenApiContentProperty(name = "reference", from = KotlinEntity.class), // reference to another object
                    @OpenApiContentProperty(name = "file-name", isArray = true, type = "string", format = "base64") // multi-file upload
                })
            }
        )
    )
    @Override
    public void handle(@NotNull Context ctx) {}

    @OpenApi(
        path = "/standalone",
        methods = HttpMethod.DELETE,
        versions = "v2",
        headers = { @OpenApiParam(name = "V2") }
    )
    @OpenApi(
        path = "standalone",
        methods = HttpMethod.DELETE,
        versions = "v1",
        headers = { @OpenApiParam(name = "V1") }
    )
    static final class EntityDto implements Serializable {

        private final int status;
        private final @NotNull String message;
        private final @NotNull String timestamp;
        private final @NotNull Foo foo;
        private final @NotNull List<Foo> foos;
        private final @NotNull Map<String, Map<String, Bar>> bars = new HashMap<>();
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
        public @NotNull Map<String, Map<String, Bar>> getBars() {
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

        // should contain examples
        @OpenApiExample(objects = {
            @OpenApiExampleProperty(value = "2022-08-14T21:13:03.546Z"),
            @OpenApiExampleProperty(value = "2022-08-14T21:13:03.546Z")
        })
        public @NotNull String[] getTimestamps() {
            return new String[] { timestamp };
        }

        // should contain dedicated foo example
        @OpenApiExample(objects = {
            @OpenApiExampleProperty(name = "name", value = "Margot Robbie"),
            @OpenApiExampleProperty(name = "link", value = "Dedicated link")
        })
        public @NotNull Foo getExampleFoo() {
            return new Foo();
        }

        // should contain object example
        @OpenApiExample(objects = {
            @OpenApiExampleProperty(name = "name", value = "Margot Robbie"),
            @OpenApiExampleProperty(name = "link", value = "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        })
        public @NotNull Object getExampleObject() {
            return new String[] { timestamp };
        }

        // should contain objects example
        @OpenApiExample(objects = {
            @OpenApiExampleProperty(name = "Barbie", objects = {
                @OpenApiExampleProperty(name = "name", value = "Margot Robbie"),
                @OpenApiExampleProperty(name = "link", value = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
                @OpenApiExampleProperty(name = "metadata", raw = "{'name': 'Barbie'}"),
                @OpenApiExampleProperty(name = "additionalInfo", raw = "{}"),
            }),
        })
        public @NotNull Object[] getExampleObjects() {
            return new String[] { timestamp };
        }

        // should contain example for primitive types, SwaggerUI will automatically display this as an Integer
        @OpenApiExample("5050")
        @OpenApiNumberValidation(
                minimum = "5000",
                exclusiveMinimum = true,
                maximum = "6000",
                exclusiveMaximum = true,
                multipleOf = "50"
        )
        @OpenApiStringValidation(
                minLength = "4",
                maxLength = "4",
                pattern = "^[0-9]{4}$",
                format = "int32"
        )
        @OpenApiArrayValidation(
                minItems = "1",
                maxItems = "1",
                uniqueItems = true
        )
        @OpenApiObjectValidation(
                minProperties = "1",
                maxProperties = "1"
        )
        public int getVeryImportantNumber() {
            return status + 1;
        }

        @OpenApiDescription("Some description")
        public String getDescription() {
            return "Description";
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

        // by default nullable fields are not required, but we can force it
        //@OpenApiRequired
        public JsonSchemaEntity getNullableIsRequired() {
            return null;
        }

    }

    static final class Foo {

        @OpenApiExample("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        public String getLink() {
            return "";
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
    @OpenApiByFields(Visibility.PRIVATE)
    static final class LombokEntity {
        @OpenApiExample("example")
        @OpenApiDescription("description")
        @OpenApiPropertyType(definedBy = Integer.class)
        @OpenApiRequired
        @OpenApiName("customPropertyName")
        @OpenApiStringValidation(
            minLength = "2",
            maxLength = "10",
            pattern = "^[a-zA-Z]+$"
        )
        private String property;

        @OpenApiIgnore
        public String test;
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
    record RecordEntity(
        @NotEmpty String name,
        @NotEmpty String surname
    ) {}

    // should query fields
    @OpenApiByFields(value = Visibility.PROTECTED, only = true) // by default: PUBLIC
    static final class DtoWithFields {

        public String publicName;
        String defaultName;
        protected String protectedName;
        private String privateName;

        public String getCustom() {
            return "custom";
        }
    }

    // should query fields and methods
    @OpenApiByFields(Visibility.PROTECTED) // by default: PUBLIC
    static final class DtoWithFieldsAndMethods {

        public String publicName;
        String defaultName;
        protected String protectedName;
        private String privateName;

        public String getCustom() {
            return "custom";
        }
    }

    enum EnumEntity {

        ONE("A"),
        TWO("B");

        private final String name;

        EnumEntity(String name) {
            this.name = name;
        }

        public String getName() {
            Enum<?> e = EnumEntity.TWO;
            return name;
        }

    }

    @JsonSchema(requireNonNulls = false)
    static final class JsonSchemaEntity {

        @OpenApiRequired
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

    @Target({ ElementType.METHOD, ElementType.TYPE })
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

    @OpenApiName("EntityWithCustomName")
    class CustomNameEntity {}

}
