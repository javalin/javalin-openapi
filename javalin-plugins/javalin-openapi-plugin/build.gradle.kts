description = "Javalin OpenAPI Plugin | Serve raw OpenApi documentation under dedicated endpoint"

plugins {
    kotlin("kapt")
}

dependencies {
    api(project(":openapi-specification"))

    kaptTest(project(":openapi-annotation-processor"))
}
