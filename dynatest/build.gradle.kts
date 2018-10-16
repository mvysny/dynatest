import com.jfrog.bintray.gradle.BintrayExtension
import groovy.lang.Closure
import org.gradle.api.tasks.GradleBuild
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

dependencies {
    compile(project(":dynatest-api"))
    compile("org.junit.jupiter:junit-jupiter-api:5.3.0")
    compile("org.junit.platform:junit-platform-engine:1.3.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.0")
}

val configureBintray = ext["configureBintray"] as (artifactId: String) -> Unit
configureBintray("dynatest")

this.getTasksByName("test", false).first().doLast {
    // verify that Gradle ran tests for all test classes and didn't ignore DynaTests
    val expectedTests = file("src/test/kotlin/com/github/mvysny/dynatest")
        .list()
        .filter { it.endsWith("Test.kt") }
        .map { "TEST-com.github.mvysny.dynatest.${it.removeSuffix(".kt")}.xml" }
        .sorted()
    val actualTests = file("build/test-results/test")
        .list()
        .filter { it.endsWith(".xml") && !it.contains("_UniqueIdCheckupClass") }
        .sorted()
    if (expectedTests != actualTests) {
        throw RuntimeException("Expected tests to run: $expectedTests got $actualTests")
    }
}
