package com.github.mvysny.dynatest.engine

import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.mvysny.dynatest.DynaNodeTest
import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.Outcome
import org.junit.platform.engine.TestSource

/**
 * A definition of a test graph node, either a group or a concrete test. Since we can't run tests right when [DynaNodeGroup.test]
 * is called (because it's the job of JUnit5 to actually run tests), we need to remember the test so that we can tell JUnit5 to run it
 * later on.
 *
 * Every [DynaNodeGroup.test] and [DynaNodeGroup.group] call
 * creates this node which in turn can be converted to JUnit5 structures eligible for execution.
 */
internal sealed class DynaNodeImpl(
    internal val name: String,
    internal val src: StackTraceElement?,
    internal val enabled: Boolean
) {
    abstract fun toTestSource(): TestSource?
}

/**
 * Represents a single test with a [name] and the test's [body]. Created when you call [DynaNodeGroup.test].
 *
 * To start writing tests, just extend [DynaTest]. See [DynaTest] for more details.
 */
internal class DynaNodeTestImpl internal constructor(
    name: String,
    internal val body: DynaNodeTest.() -> Unit,
    src: StackTraceElement?,
    enabled: Boolean
) : DynaNodeImpl(name, src, enabled), DynaNodeTest {
    override fun toTestSource(): TestSource? = src?.toTestSource(name)
}

/**
 * Represents a single test group with a [name]. Created when you call [group].
 *
 * To start writing tests, just extend [DynaTest]. See [DynaTest] for more details.
 */
internal class DynaNodeGroupImpl internal constructor(
    name: String,
    src: StackTraceElement?,
    enabled: Boolean
) : DynaNodeImpl(name, src, enabled), DynaNodeGroup {

    override fun toTestSource(): TestSource? = src?.toTestSource(null)

    private var inDesignPhase: Boolean = true
    private fun checkInDesignPhase(funName: String) {
        check(inDesignPhase) { "It appears that you are attempting to call $funName from a test{} block. You should create tests only from the group{} blocks since they run at design time (and not at run time, like the test{} blocks)" }
    }

    internal val children = mutableListOf<DynaNodeImpl>()
    /**
     * What to run before every test.
     */
    internal val beforeEach = mutableListOf<()->Unit>()
    /**
     * What to run after every test.
     */
    internal val afterEach = mutableListOf<(Outcome)->Unit>()
    /**
     * What to run before any of the test is started in this group.
     */
    internal val beforeGroup = mutableListOf<()->Unit>()
    /**
     * What to run after all tests are done in this group.
     */
    internal val afterGroup = mutableListOf<(Outcome)->Unit>()

    internal fun onDesignPhaseEnd() {
        inDesignPhase = false
        children.forEach { (it as? DynaNodeGroupImpl)?.onDesignPhaseEnd() }
    }

    private fun checkNameNotYetUsed(name: String) {
        require(children.none { it.name == name }) { "test/group with name '$name' is already present: ${children.joinToString { it.name }}" }
    }

    override fun test(name: String, body: DynaNodeTest.()->Unit) {
        test(name, body, true)
    }

    private fun test(name: String, body: DynaNodeTest.()->Unit, enabled: Boolean) {
        checkInDesignPhase("test")
        checkNameNotYetUsed(name)
        val source = computeTestSource()
        children.add(DynaNodeTestImpl(name, body, source, this.enabled && enabled))
    }

    override fun group(name: String, block: DynaNodeGroup.()->Unit) {
        group(name, block, true)
    }

    private fun group(name: String, block: DynaNodeGroup.()->Unit, enabled: Boolean) {
        checkInDesignPhase("group")
        checkNameNotYetUsed(name)
        val source = computeTestSource()
        val group = DynaNodeGroupImpl(name, source, this.enabled && enabled)
        group.block()
        children.add(group)
    }

    override fun xtest(name: String, body: DynaNodeTest.() -> Unit) {
        test(name, body, false)
    }

    override fun xgroup(name: String, block: DynaNodeGroup.() -> Unit) {
        group(name, block, false)
    }

    override fun beforeEach(block: ()->Unit) {
        checkInDesignPhase("beforeEach")
        beforeEach.add(block)
    }

    override fun afterEach(block: (Outcome)->Unit) {
        checkInDesignPhase("afterEach")
        afterEach.add(block)
    }

    override fun beforeGroup(block: ()->Unit) {
        checkInDesignPhase("beforeGroup")
        beforeGroup.add(block)
    }

    override fun afterGroup(block: (Outcome)->Unit) {
        checkInDesignPhase("afterGroup")
        afterGroup.add(block)
    }

    companion object {
        private val pkg: String = DynaNodeGroupImpl::class.java.`package`.name
        /**
         * Computes the pointer to the source of the test and returns it.
         * @return the pointer to the test source; returns null if the source can not be computed by any means.
         */
        internal fun computeTestSource(): StackTraceElement? {
            val stackTrace: Array<StackTraceElement> = Thread.currentThread().stackTrace
            // find first stack trace which doesn't point to this package and is not Thread.getStackTrace()
            // That's going to be the caller of the test/group method.
            return stackTrace.asSequence()
                .filter { !it.className.startsWith(pkg) && it.className != Thread::class.java.name }
                .firstOrNull()
        }
    }
}
