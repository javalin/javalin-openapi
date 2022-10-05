plugins {
    `java-library`
    kotlin("jvm") version "1.7.10"
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
}

allprojects {
    apply(plugin = "java-library")
    apply(plugin = "signing")
    apply(plugin = "maven-publish")

    group = "io.javalin.community.openapi"
    version = "5.0.0"

    repositories {
        mavenCentral()
        maven("https://maven.reposilite.com/snapshots")
    }

    publishing {
        repositories {
            maven {
                name = "reposilite-repository"
                url = uri("https://maven.reposilite.com/${if (version.toString().endsWith("-SNAPSHOT")) "snapshots" else "releases"}")

                credentials {
                    username = getEnvOrProperty("MAVEN_NAME", "mavenUser")
                    password = getEnvOrProperty("MAVEN_TOKEN", "mavenPassword")
                }
            }
        }
    }

    publishing {
        publications {
            create<MavenPublication>("library") {
                from(components.getByName("java"))

                pom {
                    name.set("Javalin OpenAPI Plugin")
                    description.set("Compile-time OpenAPI integration for Javalin 5.x")
                    url.set("https://github.com/javalin/javalin-openapi")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("dzikoysk")
                            name.set("dzikoysk")
                            email.set("dzikoysk@dzikoysk.net")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/javalin/javalin-openapi.git")
                        developerConnection.set("scm:git:ssh://github.com/javalin/javalin-openapi.git")
                        url.set("https://github.com/javalin/javalin-openapi.git")
                    }
                }
            }
        }
    }

    signing {
        if (findProperty("signing.keyId") != null) {
            sign(publishing.publications.getByName("library"))
        }
    }

    java {
        withJavadocJar()
        withSourcesJar()
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

subprojects {
    apply(plugin = "application")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        val javalin = "5.0.1"
        compileOnly("io.javalin:javalin:$javalin")

        val junit = "5.8.2"
        testImplementation("org.junit.jupiter:junit-jupiter-api:$junit")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

nexusPublishing {
    repositories {
        sonatype {
            username.set(getEnvOrProperty("SONATYPE_USER", "sonatypeUser"))
            password.set(getEnvOrProperty("SONATYPE_PASSWORD", "sonatypePassword"))
        }
    }
}

fun getEnvOrProperty(env: String, property: String): String? =
    System.getenv(env) ?: findProperty(property)?.toString()