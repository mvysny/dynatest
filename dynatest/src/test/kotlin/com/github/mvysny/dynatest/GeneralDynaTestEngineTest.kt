package com.github.mvysny.dynatest

import com.github.mvysny.dynatest.engine.DynaTestEngine
import com.github.mvysny.dynatest.engine.InitFailedTestDescriptor
import org.junit.jupiter.api.Test
import org.junit.platform.engine.*
import org.junit.platform.engine.discovery.ClassSelector
import org.junit.platform.engine.reporting.ReportEntry
import kotlin.test.expect

/**
 * Tests the very general properties and generic error-handling capabilities of the DynaTestEngine itself. More specialized tests are located
 * at [DynaTestEngineTest].
 */
class GeneralDynaTestEngineTest {
    /**
     * The [TestEngine.discover] function must not fail even if the test discovery itself fails.
     */
    @Test
    fun failingTestSuiteMustNotFailInDiscover() {
        val engine = DynaTestEngine()
        withFail {
            engine.discover2(TestSuiteFailingInInit::class.java)
        }
    }

    private fun DynaTestEngine.discover2(vararg testClasses: Class<*>): TestDescriptor {
        require (testClasses.isNotEmpty())
        return discover(object : EngineDiscoveryRequest {
            override fun getConfigurationParameters(): ConfigurationParameters = EmptyConfigParameters
            override fun <T : DiscoveryFilter<*>?> getFiltersByType(filterType: Class<T>?): MutableList<T> = mutableListOf()
            override fun <T : DiscoverySelector> getSelectorsByType(selectorType: Class<T>): MutableList<T> =
                testClasses.map { it.toSelector() } .filterIsInstance(selectorType) .toMutableList()
        }, UniqueId.forEngine(id))
    }

    /**
     * The [TestEngine.discover] block must not fail even if the test discovery itself fails; instead it must produce an always-failing
     * test descriptor.
     */
    @Test
    fun failingTestSuiteMustFailInExecute() {
        val engine = DynaTestEngine()
        val tests: TestDescriptor = withFail { engine.discover2(TestSuiteFailingInInit::class.java) }
        expect<Class<*>>(InitFailedTestDescriptor::class.java) { tests.children.first().javaClass }
        expectThrows(RuntimeException::class, "Simulated") {
            engine.execute(ExecutionRequest(tests, ThrowingExecutionListener, EmptyConfigParameters))
        }
    }

    @Test
    fun checkUniqueIDsGeneratedByTheEngine() {
        operator fun TestDescriptor.get(index: Int) = children.toList()[index]

        val engine = DynaTestEngine()
        var td = engine.discover2(_UniqueIdCheckupClass::class.java)
        expect("[engine:DynaTest]") { td.uniqueId.toString() }
        expect("[engine:DynaTest]/[group:_UniqueIdCheckupClass]") { td[0].uniqueId.toString() }
        td = td[0]
        expect("[engine:DynaTest]/[group:_UniqueIdCheckupClass]/[test:root test]") { td[0].uniqueId.toString() }
        expect("[engine:DynaTest]/[group:_UniqueIdCheckupClass]/[group:root group]") { td[1].uniqueId.toString() }
        expect("[engine:DynaTest]/[group:_UniqueIdCheckupClass]/[group:root group]/[test:nested]") { td[1][0].uniqueId.toString() }
        expect("[engine:DynaTest]/[group:_UniqueIdCheckupClass]/[group:root group]/[group:nested group]") { td[1][1].uniqueId.toString() }
        expect("[engine:DynaTest]/[group:_UniqueIdCheckupClass]/[group:root group]/[group:nested group]/[test:nested nested]") { td[1][1][0].uniqueId.toString() }
        expect("[engine:DynaTest]/[group:_UniqueIdCheckupClass]/[group:root group]/[test:nested2]") { td[1][2].uniqueId.toString() }
        expect("[engine:DynaTest]/[group:_UniqueIdCheckupClass]/[test:root test 2]") { td[2].uniqueId.toString() }
    }
}

class _UniqueIdCheckupClass : DynaTest({
    test("root test") {}
    group("root group") {
        test("nested") {}
        group("nested group") {
            test("nested nested") {}
        }
        test("nested2") {}
    }
    test("root test 2") {}
})

/**
 * An execution listener which immediately throws when an exception occurs. Used together with [runTests] to fail eagerly.
 */
private object ThrowingExecutionListener : EngineExecutionListener {
    override fun executionFinished(testDescriptor: TestDescriptor, testExecutionResult: TestExecutionResult) {
        if (testExecutionResult.throwable.isPresent) throw testExecutionResult.throwable.get()
    }

    override fun reportingEntryPublished(testDescriptor: TestDescriptor, entry: ReportEntry) {}
    override fun executionSkipped(testDescriptor: TestDescriptor, reason: String) {
        throw RuntimeException("Unexpected")
    }
    override fun executionStarted(testDescriptor: TestDescriptor) {}
    override fun dynamicTestRegistered(testDescriptor: TestDescriptor) {
        throw RuntimeException("Unexpected")
    }
}

private fun Class<*>.toSelector(): ClassSelector {
    val c = ClassSelector::class.java.declaredConstructors.first { it.parameterTypes[0] == Class::class.java }
    c.isAccessible = true
    return c.newInstance(this) as ClassSelector
}

private var fail = false
private fun <T> withFail(block: ()->T): T {
    fail = true
    try {
        return block()
    } finally {
        fail = false
    }
}

/**
 * A dyna test which throws an exception when initialized. Used by [GeneralDynaTestEngineTest] to check that this failure won't make the
 * [TestEngine.discover] function fail.
 */
class TestSuiteFailingInInit : DynaTest({
    if (fail) throw RuntimeException("Simulated")
})
