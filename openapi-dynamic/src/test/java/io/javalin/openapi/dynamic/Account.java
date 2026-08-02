package io.javalin.openapi.dynamic;

import io.javalin.openapi.OpenApiDescription;
import io.javalin.openapi.OpenApiIgnore;
import io.javalin.openapi.OpenApiName;

import java.util.List;
import java.util.Map;

public class Account {

    @NotNull
    public String getId() {
        return "";
    }

    public int getAge() {
        return 0;
    }

    public String getName() {
        return "";
    }

    public Role getRole() {
        return Role.ADMIN;
    }

    public Address getAddress() {
        return null;
    }

    public List<String> getTags() {
        return List.of();
    }

    public Map<String, Integer> getMeta() {
        return Map.of();
    }

    @OpenApiName("e_mail")
    public String getEmail() {
        return "";
    }

    @OpenApiIgnore
    public String getSecret() {
        return "";
    }

    @OpenApiDescription("Human readable label")
    public String getLabel() {
        return "";
    }
}
