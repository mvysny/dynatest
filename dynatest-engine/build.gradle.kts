import com.jfrog.bintray.gradle.BintrayExtension
import groovy.lang.Closure
import org.gradle.api.tasks.GradleBuild
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

dependencies {
    compile(project(":dynatest-api"))
    compile("org.junit.platform:junit-platform-engine:1.3.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.0")
}

val org.gradle.api.Project.java2: org.gradle.api.plugins.JavaPluginConvention get() =
    convention.getPluginByName("java")

val sourceJar = task("sourceJar", Jar::class) {
    dependsOn(tasks.findByName("classes"))
    classifier = "sources"
    from(java2.sourceSets["main"].allSource)
}

publishing {
    publications {
        create("mavenJava", MavenPublication::class.java).apply {
            groupId = project.group.toString()
            artifactId = "dynatest-engine"
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

bintray {
    val local = Properties()
    val localProperties: File = project.rootProject.file("local.properties")
    if (localProperties.exists()) {
        localProperties.inputStream().use { local.load(it) }
    }

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
