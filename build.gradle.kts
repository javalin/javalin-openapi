import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    `java-library`
    kotlin("jvm") version "2.2.20"
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

description = "Javalin OpenAPI Parent | Parent"

allprojects {
    apply(plugin = "java-library")
    apply(plugin = "signing")
    apply(plugin = "maven-publish")

    group = "io.javalin.community.openapi"
    version = "7.0.0-beta.3"

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

    afterEvaluate {
        description
            ?.takeIf { it.isNotEmpty() }
            ?.split("|")
            ?.let { (projectName, projectDescription) ->
                publishing {
                    publications {
                        create<MavenPublication>("library") {
                            pom {
                                name.set(projectName)
                                description.set(projectDescription)
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

                            from(components.getByName("java"))
                        }
                    }
                }

                if (findProperty("signing.keyId").takeIf { it != null && it.toString().trim().isNotEmpty() } != null) {
                    signing {
                        sign(publishing.publications.getByName("library"))
                    }
                }
            }
    }

    java {
        withJavadocJar()
        withSourcesJar()
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

subprojects {
    apply(plugin = "application")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            languageVersion.set(KotlinVersion.KOTLIN_2_2)
            javaParameters.set(true)
        }
    }

    dependencies {
        val javalin = "7.0.0"
        compileOnly("io.javalin:javalin:$javalin")
        testImplementation("io.javalin:javalin:$javalin")

        val junit = "5.9.3"
        testImplementation("org.junit.jupiter:junit-jupiter-params:$junit")
        testImplementation("org.junit.jupiter:junit-jupiter-api:$junit")
        testImplementation("org.junit.jupiter:junit-jupiter-engine:$junit")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")

        testImplementation("org.assertj:assertj-core:3.24.2")
        testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.38.0")
        testImplementation("com.konghq:unirest-java:3.14.2")

        testImplementation("ch.qos.logback:logback-classic:1.4.14")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            username.set(getEnvOrProperty("SONATYPE_USER", "sonatypeUser"))
            password.set(getEnvOrProperty("SONATYPE_PASSWORD", "sonatypePassword"))
        }
    }
}

fun getEnvOrProperty(env: String, property: String): String? =
    System.getenv(env) ?: findProperty(property)?.toString()
