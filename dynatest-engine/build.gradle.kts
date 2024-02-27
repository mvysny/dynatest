dependencies {
    api(project(":dynatest-api"))
    api(libs.junit.jupiter.api)
    api(libs.junit.platform.engine)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

kotlin {
    explicitApi()
}

val publishing = ext["publishing"] as (artifactId: String) -> Unit
publishing("dynatest-engine")

/**
 * Counts all occurrences of [substring] within the receiver string.
 */
fun String.countOccurrences(substring: String) =
    indices.count { substring(it).startsWith(substring) }

// verify that Gradle ran tests for all test classes and didn't ignore DynaTests
tasks.named<Task>("test") { doLast {
    val testClasses: Array<String> = file("src/test/kotlin/com/github/mvysny/dynatest").list()!!
    val expectedTests: List<String> = testClasses
            .filter { it.endsWith("Test.kt") }
            .map { "TEST-com.github.mvysny.dynatest.${it.removeSuffix(".kt")}.xml" }
            .sorted()
    val actualTests: List<String> = file("build/test-results/test")
            .list()!!
            .filter { it.endsWith(".xml") && !it.contains("_UniqueIdCheckupClass") }
            .sorted()
    if (expectedTests != actualTests) {
        throw RuntimeException("build.gradle.kts: Expected tests to run: $expectedTests got $actualTests")
    }

    // verify that Gradle runs all tests even if they are same-named (but different UniqueId)
    val testXmlPath = "build/test-results/test/TEST-com.github.mvysny.dynatest.DynaTestEngineTest.xml"
    val xml = file(testXmlPath).readText()
    val testcases = xml.countOccurrences("<testcase")
    val expectedTestCaseCount = 45
    if (testcases != expectedTestCaseCount) {
        throw RuntimeException("build.gradle.kts: Expected $expectedTestCaseCount testcases in $testXmlPath but got $testcases")
    }
} }
