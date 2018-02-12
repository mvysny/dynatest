package com.github.mvysny.dynatest

import org.junit.platform.engine.*
import org.junit.platform.engine.reporting.ReportEntry
import java.util.*

/**
 * A very simple test support, simply runs all registered tests immediately; bails out at first failed test. You generally should rely on
 * JUnit5 to run your tests instead - just extend [DynaTest] class. To create a reusable test battery just define an extension method on the
 * [DynaNodeGroup] class - see the `CalculatorTest.kt` file for more details.
 */
fun runTests(block: DynaNodeGroup.()->Unit) {
    val group = DynaNodeGroup("root", null)
    group.block()
    val testDescriptor = DynaNodeTestDescriptor(UniqueId.forEngine("dynatest"), group)
    DynaTestEngine().execute(ExecutionRequest(testDescriptor, ThrowingExecutionListener, EmptyConfigParameters))
}

object ThrowingExecutionListener : EngineExecutionListener {
    override fun executionFinished(testDescriptor: TestDescriptor, testExecutionResult: TestExecutionResult) {
        if (testExecutionResult.throwable.isPresent) throw testExecutionResult.throwable.get()
    }

    override fun reportingEntryPublished(testDescriptor: TestDescriptor?, entry: ReportEntry?) {}
    override fun executionSkipped(testDescriptor: TestDescriptor?, reason: String?) {
        throw RuntimeException("Unexpected")
    }
    override fun executionStarted(testDescriptor: TestDescriptor?) {}
    override fun dynamicTestRegistered(testDescriptor: TestDescriptor?) {
        throw RuntimeException("Unexpected")
    }
}

object EmptyConfigParameters : ConfigurationParameters {
    override fun getBoolean(key: String?): Optional<Boolean> = Optional.ofNullable(null)
    override fun size(): Int = 0
    override fun get(key: String?): Optional<String> = Optional.ofNullable(null)
}
