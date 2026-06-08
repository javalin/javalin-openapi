package io.javalin.openapi.dynamic.fixtures;

public class Address {

    private final String city;
    private final String zip;

    public Address(String city, String zip) {
        this.city = city;
        this.zip = zip;
    }

    public String getCity() {
        return city;
    }

    public String getZip() {
        return zip;
    }
}
