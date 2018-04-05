import com.jfrog.bintray.gradle.BintrayExtension
import groovy.lang.Closure
import org.gradle.api.tasks.GradleBuild
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

val local = Properties()
if (project.rootProject.file("local.properties").exists()) {
    project.rootProject.file("local.properties").inputStream().use { local.load(it) }
}

group = "com.github.mvysny.dynatest"
version = "0.8"

plugins {
    kotlin("jvm") version "1.2.31"
    id("com.jfrog.bintray") version "1.7.3"
    `maven-publish`
}

defaultTasks("clean", "build")

repositories {
    mavenCentral()
    jcenter()
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        // to see the exceptions of failed tests in Travis-CI console.
        exceptionFormat = TestExceptionFormat.FULL
    }
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile(kotlin("test"))
    compile("org.junit.jupiter:junit-jupiter-api:5.1.0")
    compile("org.junit.platform:junit-platform-engine:1.1.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.1.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val sourceJar = task("sourceJar", Jar::class) {
    dependsOn(tasks.findByName("classes"))
    classifier = "sources"
    from(java.sourceSets["main"].allSource)
}

publishing {
    publications {
        create("mavenJava", MavenPublication::class.java).apply {
            groupId = project.group.toString()
            artifactId = "dynatest"
            version = project.version.toString()
            pom.withXml {
                val root = asNode()
                root.appendNode("description", "Simple Dynamic Testing Framework piggybacking on JUnit5")
                root.appendNode("name", "DynaTest")
                root.appendNode("url", "https://github.com/mvysny/dynatest")
            }
            from(components.findByName("java")!!)
            artifact(sourceJar) {
                classifier = "sources"
            }
        }
    }
}

tasks.findByName("build")!!.dependsOn(tasks.findByName("publishToMavenLocal")!!)

bintray {
    user = local.getProperty("bintray.user")
    key = local.getProperty("bintray.key")
    pkg(closureOf<BintrayExtension.PackageConfig> {
        repo = "github"
        name = "com.github.mvysny.dynatest"
        setLicenses("Apache-2.0")
        vcsUrl = "https://github.com/mvysny/dynatest"
        publish = true
        setPublications("mavenJava")
        version(closureOf<BintrayExtension.VersionConfig> {
            this.name = project.version.toString()
            released = Date().toString()
        })
    })
}
