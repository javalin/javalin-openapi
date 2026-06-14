package io.javalin.introspection.test.sub;

/** Has a package-private field (not inherited across packages) and a public one (inherited everywhere). */
public class PackagePrivateBase {
    String packagePrivateField = "";
    public String publicField = "";
}
