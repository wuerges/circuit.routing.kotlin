import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.31"
}

version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { setUrl("https://dl.bintray.com/hotkeytlt/maven") }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    compile("com.github.h0tk3y.betterParse:better-parse-jvm:0.4.0-alpha-3")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}