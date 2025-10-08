group = "org.comictools"
version = "1.0-SNAPSHOT"

val ktorVersion: String by project

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    alias(libs.plugins.kotlin.jvm)

    // Apply the application plugin to add support for building a CLI application in Java.
    application

    kotlin("plugin.serialization") version "1.9.22"

    id("com.gradleup.shadow") version "9.2.2"
}

buildscript {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
        gradlePluginPortal()
    }
}

dependencies {
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-cli-jvm:0.3.6")
    implementation("org.apache.jena:apache-jena:4.10.0")
    implementation("io.ktor:ktor-client-cio:3.0.0")
    implementation("org.json:json:20231013")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // Use JUnit Jupiter for testing.
    testImplementation(libs.junit.jupiter)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(18)
}

application {
    // Define the main class for the application.
    mainClass = "nl.knaw.huc.di.cli.JsonLd2TtlKt"
    applicationName = "jsonld2ttl"
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = application.mainClass
    }
}
