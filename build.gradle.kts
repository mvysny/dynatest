import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
    compile("org.junit.jupiter:junit-jupiter-engine:5.0.3")
}
