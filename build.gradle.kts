import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.0.3")
    }
}

apply {
    plugin("org.junit.platform.gradle.plugin")
}

plugins {
    kotlin("jvm") version "1.2.21"
}

defaultTasks("clean", "build")

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile(kotlin("test"))
    compile("org.junit.jupiter:junit-jupiter-api:5.0.3")
    compile("org.junit.platform:junit-platform-engine:1.0.3")
}
