package com.github.mvysny.dynatest

import org.junit.platform.engine.*
import org.junit.platform.engine.reporting.ReportEntry
import java.util.*
import kotlin.test.expect
import kotlin.test.fail

/**
 * A very simple test support, simply runs all registered tests immediately; bails out at first failed test. You generally should rely on
 * JUnit5 to run your tests instead - just extend [DynaTest] class. To create a reusable test battery just define an extension method on the
 * [DynaNodeGroup] class - see the `CalculatorTest.kt` file for more details.
 * @throws TestFailedException if any of the test failed. To expect this, nest call to this function into the [expectFailures] function.
 */
internal fun runTests(block: DynaNodeGroup.()->Unit): TestResults {
    val group = DynaNodeGroup("root", null)
    group.block()
    val testDescriptor = DynaNodeTestDescriptor(UniqueId.forEngine("dynatest"), group)
    val result = TestResults()
    DynaTestEngine().execute(ExecutionRequest(testDescriptor, TestResultBuilder(result), EmptyConfigParameters))
    if (!result.isSuccess) throw TestFailedException(result)
    return result
}

private class TestResultBuilder(val results: TestResults) : EngineExecutionListener {
    override fun executionFinished(testDescriptor: TestDescriptor, testExecutionResult: TestExecutionResult) {
        if (testDescriptor is DynaNodeTestDescriptor && testDescriptor.isContainer && testExecutionResult.status == TestExecutionResult.Status.SUCCESSFUL) {
            // don't register group as successful
        } else {
            results.testsRan[testDescriptor.uniqueId] = testExecutionResult
        }
    }

    override fun reportingEntryPublished(testDescriptor: TestDescriptor, entry: ReportEntry) {}

    override fun executionSkipped(testDescriptor: TestDescriptor, reason: String) {
        results.testsSkipped[testDescriptor.uniqueId] = reason
    }

    override fun executionStarted(testDescriptor: TestDescriptor) {}

    override fun dynamicTestRegistered(testDescriptor: TestDescriptor) {}
}

internal class TestFailedException(val results: TestResults) : Exception(results.toString())

/**
 * Wraps the [runTests] call and expect it to fail.
 */
internal fun expectFailures(block: ()->Unit, results: TestResults.()->Unit) {
    try {
        block()
        fail("Expected to fail")
    } catch (e: TestFailedException) {
        e.results.results()
    }
}

/**
 * The test results, captured by the [runTests] function.
 * @property testsRan all tests that were ran, either successfully or unsuccessfully. Only counts in groups when their `beforeAll` failed.
 * @property testsSkipped typically empty since there is no means to skip a test in DynaTest.
 */
internal data class TestResults(val testsRan: MutableMap<UniqueId, TestExecutionResult> = mutableMapOf(),
                                val testsSkipped: MutableMap<UniqueId, String> = mutableMapOf()) {

    val failures: Int get() = testsRan.values.count { it.status == TestExecutionResult.Status.FAILED }
    val successful: Int get() = testsRan.values.count { it.status == TestExecutionResult.Status.SUCCESSFUL }
    val aborted: Int get() = testsRan.values.count { it.status == TestExecutionResult.Status.ABORTED }
    val isSuccess: Boolean get() = failures == 0 && aborted == 0

    fun expectStats(successful: Int, failures: Int, aborted: Int) {
        expect(successful, dump()) { this.successful }
        expect(failures, dump()) { this.failures }
        expect(aborted, dump()) { this.aborted }
    }

    inline fun <reified T: Throwable> expectFailure(name: String) {
        expect<Class<out Throwable>>(T::class.java) { getFailure(name).javaClass }
    }

    fun getFailure(name: String): Throwable {
        val entry = testsRan.entries.firstOrNull { it.key.segments.last().value == name } ?: throw IllegalArgumentException("No test with name $name: ${testsRan.keys}")
        expect(TestExecutionResult.Status.FAILED) { entry.value.status }
        return entry.value.throwable.get()
    }

    override fun toString() = "TestResults(successful=$successful, failures=$failures, aborted=$aborted, skipped=${testsSkipped.size})"

    /**
     * Dumps all failures and their stacktraces; then dumps the test overview.
     */
    fun dump() = buildString {
        testsRan.entries.filter { it.value.status != TestExecutionResult.Status.SUCCESSFUL } .forEach {
            append("${it.key} --> ${it.value.status}\n")
            if (it.value.throwable.isPresent) {
                append(it.value.throwable.get().getStackTraceAsString())
            }
            append('\n')
        }
        testsSkipped.forEach {
            append("${it.key} --> SKIPPED: ${it.value}\n")
        }
        append(this@TestResults)
    }
}

internal object EmptyConfigParameters : ConfigurationParameters {
    override fun getBoolean(key: String?): Optional<Boolean> = Optional.ofNullable(null)
    override fun size(): Int = 0
    override fun get(key: String?): Optional<String> = Optional.ofNullable(null)
}
