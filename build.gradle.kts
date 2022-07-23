plugins {
    `java-library`
    `maven-publish`
    kotlin("jvm") version "1.7.10"
}

allprojects {
    apply(plugin = "maven-publish")

    group = "io.javalin"
    version = "5.0.0-SNAPSHOT"

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
                    username = System.getenv("MAVEN_NAME") ?: property("mavenUser").toString()
                    password = System.getenv("MAVEN_TOKEN") ?: property("mavenPassword").toString()
                }
            }
        }
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "application")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        val javalin = "5.0.0-SNAPSHOT"
        compileOnly("io.javalin:javalin:$javalin")

        val junit = "5.8.2"
        testImplementation("org.junit.jupiter:junit-jupiter-api:$junit")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit")
    }

    java {
        withJavadocJar()
        withSourcesJar()
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    publishing {
        publications {
            create<MavenPublication>("library") {
                from(components.getByName("java"))
            }
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}