dependencies {
    compile(project(":dynatest-api"))
    compile("org.junit.jupiter:junit-jupiter-api:5.4.0")
    compile("org.junit.platform:junit-platform-engine:1.4.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.0")
}

val configureBintray = ext["configureBintray"] as (artifactId: String) -> Unit
configureBintray("dynatest-engine")


// verify that Gradle ran tests for all test classes and didn't ignore DynaTests
fun String.countSubstrings(substring: String) =
    indices.count { substring(it).startsWith(substring) }

tasks.named<Task>("test") { doLast {
    val expectedTests: List<String> = file("src/test/kotlin/com/github/mvysny/dynatest")
        .list()!!
        .filter { it.endsWith("Test.kt") }
        .map { "TEST-com.github.mvysny.dynatest.${it.removeSuffix(".kt")}.xml" }
        .sorted()
    val actualTests = file("build/test-results/test")
        .list()!!
        .filter { it.endsWith(".xml") && !it.contains("_UniqueIdCheckupClass") }
        .sorted()
    if (expectedTests != actualTests) {
        throw RuntimeException("build.gradle.kts: Expected tests to run: $expectedTests got $actualTests")
    }

    // verify that Gradle runs all tests even if they are same-named (but different UniqueId)
    val testXmlPath = "build/test-results/test/TEST-com.github.mvysny.dynatest.DynaTestEngineTest.xml"
    val xml = file(testXmlPath).readText()
    val testcases = xml.countSubstrings("<testcase")
    if (testcases != 33) {
        throw RuntimeException("build.gradle.kts: Expected 33 testcases in $testXmlPath but got $testcases")
    }
} }
