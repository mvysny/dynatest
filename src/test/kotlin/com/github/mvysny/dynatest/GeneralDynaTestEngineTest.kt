package com.github.mvysny.dynatest

import org.junit.jupiter.api.Test
import org.junit.platform.engine.*
import org.junit.platform.engine.discovery.ClassSelector
import kotlin.test.expect

/**
 * Tests the very general properties and generic error-handling capabilities of the DynaTestEngine itself. More specialized tests are located
 * at [DynaTestEngineTest].
 */
class GeneralDynaTestEngineTest {
    /**
     * The [TestEngine.discover] block must not fail even if the test discovery itself fails.
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
        expectThrows(RuntimeException::class) {
            engine.execute(ExecutionRequest(tests, ThrowingExecutionListener, EmptyConfigParameters))
        }
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

class TestSuiteFailingInInit : DynaTest({
    if (fail) throw RuntimeException("Simulated")
})
