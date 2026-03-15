import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
    signing
    alias(libs.plugins.nexus.publish)
}

description = "Javalin OpenAPI Parent | Parent"

allprojects {
    apply(plugin = "java-library")
    apply(plugin = "signing")
    apply(plugin = "maven-publish")

    group = "io.javalin.community.openapi"
    version = "7.1.0"

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

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

val mavenExamples = listOf("javalin-maven-java", "javalin-maven-kotlin")

mavenExamples.forEach { example ->
    tasks.register<Exec>("test-maven-example-$example") {
        description = "Compile Maven example: $example"
        group = "verification"
        workingDir = file("examples/$example")
        commandLine("./mvnw", "compile", "-B", "-q")
        environment("JAVA_HOME", System.getProperty("java.home"))
        dependsOn(subprojects.map { it.tasks.named("publishToMavenLocal") })
    }

    tasks.register<Exec>("run-maven-example-$example") {
        description = "Run Maven example: $example"
        group = "application"
        workingDir = file("examples/$example")
        commandLine("./mvnw", "compile", "exec:java", "-B", "-q")
        environment("JAVA_HOME", System.getProperty("java.home"))
        dependsOn(subprojects.map { it.tasks.named("publishToMavenLocal") })
    }
}

tasks.register("test-maven-examples") {
    description = "Compile all Maven examples"
    group = "verification"
    dependsOn(mavenExamples.map { "test-maven-example-$it" })
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
