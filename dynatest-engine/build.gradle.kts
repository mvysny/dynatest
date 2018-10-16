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
configureBintray("dynatest-engine")

fun String.countSubstrings(substring: String) =
    indices.count { substring(it).startsWith(substring) }

tasks.named<Task>("test") { doLast {
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

    // verify that Gradle runs all tests even if they are same-named (but different UniqueId)
    val xml = file("build/test-results/test/TEST-com.github.mvysny.dynatest.DynaTestEngineTest.xml").readText()
    if (xml.countSubstrings("<testcase") != 29) {
        throw RuntimeException("Expected 29 tests in DynaTestEngineTest but got $actualTests")
    }
} }
