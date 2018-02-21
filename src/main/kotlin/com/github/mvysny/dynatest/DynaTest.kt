package com.github.mvysny.dynatest

import org.junit.platform.commons.annotation.Testable

/**
 * A definition of a test graph node, either a group or a concrete test. Since we can't run tests right when [DynaNodeGroup.test]
 * is called (because it's the job of JUnit5 to actually run tests), we need to remember the test so that we can tell JUnit5 to run it
 * later on.
 *
 * Every [DynaNodeGroup.test] and [DynaNodeGroup.group] call
 * creates this node which in turn can be converted to JUnit5 structures eligible for execution.
 */
sealed class DynaNode(internal val name: String, internal val src: StackTraceElement?) {
    protected var inDesignPhase: Boolean = true
    internal abstract fun onDesignPhaseEnd()
    protected fun checkInDesignPhase(funName: String) {
        check(inDesignPhase) { "It appears that you are attempting to call $funName from a test{} block. You should create tests only from the group{} blocks since they run at design time (and not at run time, like the test{} blocks)" }
    }

    /**
     * Creates a new test case with given [name] and registers it within current group. Does not run the test closure immediately -
     * the test is only registered for being run later on by JUnit5 runner (or by [runTests]).
     * @param body the implementation of the test; does not run immediately but only when the test case is run
     */
    abstract fun test(name: String, body: DynaNodeTest.()->Unit)

    /**
     * Registers a block which will be run exactly once before any of the tests are run. Only the tests nested in this group and its subgroups are
     * @param block the block to run. Any exceptions thrown by the block will make the test fail.
     */
    abstract fun beforeAll(block: ()->Unit)

    /**
     * Registers a block which will be run before every test registered to this group and to any nested groups.
     * `beforeEach` blocks registered by a parent/ancestor group runs before `beforeEach` blocks registered by this group.
     *
     * If any of the `beforeEach` blocks fails, no further `beforeEach` blocks are executed; furthermore the test itself is not executed as well.
     * However, all of the [afterEach] blocks for the corresponding group and all parent groups still *are* executed.
     * @param block the block to run. Any exceptions thrown by the block will make the test fail.
     */
    abstract fun beforeEach(block: ()->Unit)

    /**
     * Registers a block which will be run after every test registered to this group and to any nested groups.
     * `afterEach` blocks registered by a parent/ancestor group runs after `afterEach` blocks registered by this group.
     *
     * The `afterEach` blocks are called even if the test fails. If the `beforeEach` block fails, only the `afterEach` blocks in the corresponding
     * group and all ancestor groups are called.
     *
     * If the `afterEach` blocks throws an exception, those exceptions are added as [Throwable.getSuppressed] to the main exception (as thrown
     * by the `beforeEach` block or the test itself); or just rethrown if there is no main exception. Any exception thrown by the `afterEach`
     * block will cause the test to fail.
     * @param block the block to run. Any exceptions thrown by the block will make the test fail.
     */
    abstract fun afterEach(block: ()->Unit)

    /**
     * Registers a block which will be run only once after all of the tests are run. Only the tests nested in this group and its subgroups are
     * considered.
     * @param block the block to run. Any exceptions thrown by the block will make the test fail.
     */
    abstract fun afterAll(block: ()->Unit)
}

/**
 * Represents a single test with a [name], an execution [context] and the test's [body]. Created when you call [DynaNodeGroup.test].
 *
 * To start writing tests, just extend [DynaTest]. See [DynaTest] for more details.
 */
class DynaNodeTest internal constructor(name: String, internal val body: DynaNodeTest.()->Unit, src: StackTraceElement?) : DynaNode(name, src) {
    private fun fail(funName: String): Nothing =
        throw IllegalStateException("It appears that you are attempting to call $funName from a test{} block. You should create tests only from the group{} blocks since they run at design time (and not at run time, like the test{} blocks)")

    /**
     * You should create tests only from the group{} blocks.
     */
    @Deprecated("You should create tests only from the group{} blocks", level = DeprecationLevel.ERROR)
    override fun test(name: String, body: DynaNodeTest.() -> Unit): Nothing = fail("test")

    override fun onDesignPhaseEnd() {
        inDesignPhase = false
    }

    /**
     * You should create tests only from the group{} blocks.
     */
    @Deprecated("You should create tests only from the group{} blocks", level = DeprecationLevel.ERROR)
    override fun beforeAll(block: () -> Unit): Nothing = fail("beforeAll")

    /**
     * You should create tests only from the group{} blocks.
     */
    @Deprecated("You should create tests only from the group{} blocks", level = DeprecationLevel.ERROR)
    override fun afterEach(block: () -> Unit): Nothing = fail("afterEach")

    /**
     * You should create tests only from the group{} blocks.
     */
    @Deprecated("You should create tests only from the group{} blocks", level = DeprecationLevel.ERROR)
    override fun beforeEach(block: () -> Unit): Nothing = fail("beforeEach")

    /**
     * You should create tests only from the group{} blocks.
     */
    @Deprecated("You should create tests only from the group{} blocks", level = DeprecationLevel.ERROR)
    override fun afterAll(block: () -> Unit): Nothing = fail("afterAll")
}

/**
 * Represents a single test group with a [name]. Created when you call [group].
 *
 * To start writing tests, just extend [DynaTest]. See [DynaTest] for more details.
 */
class DynaNodeGroup internal constructor(name: String, src: StackTraceElement?) : DynaNode(name, src) {
    internal val children = mutableListOf<DynaNode>()
    /**
     * What to run before every test.
     */
    internal val beforeEach = mutableListOf<()->Unit>()
    /**
     * What to run after every test.
     */
    internal val afterEach = mutableListOf<()->Unit>()
    /**
     * What to run before any of the test is started in this group.
     */
    internal val beforeAll = mutableListOf<()->Unit>()
    /**
     * What to run after all tests are done in this group.
     */
    internal val afterAll = mutableListOf<()->Unit>()

    override fun onDesignPhaseEnd() {
        inDesignPhase = false
        children.forEach { it.onDesignPhaseEnd() }
    }

    override fun test(name: String, body: DynaNodeTest.()->Unit) {
        checkInDesignPhase("test")
        val source = computeTestSource()
        children.add(DynaNodeTest(name, body, source))
    }

    /**
     * Creates a nested group with given [name] and runs given [block]. In the block, you can create both sub-groups and tests, and you can
     * mix those freely as you like.
     * @param block the block, runs immediately.
     */
    fun group(name: String, block: DynaNodeGroup.()->Unit) {
        checkInDesignPhase("group")
        val source = computeTestSource()
        val group = DynaNodeGroup(name, source)
        group.block()
        children.add(group)
    }

    override fun beforeEach(block: ()->Unit) {
        checkInDesignPhase("beforeEach")
        beforeEach.add(block)
    }

    override fun afterEach(block: ()->Unit) {
        checkInDesignPhase("afterEach")
        afterEach.add(block)
    }

    override fun beforeAll(block: ()->Unit) {
        checkInDesignPhase("beforeAll")
        beforeAll.add(block)
    }

    override fun afterAll(block: ()->Unit) {
        checkInDesignPhase("afterAll")
        afterAll.add(block)
    }
}

/**
 * Inherit from this class to write the tests:
 * ```
 * class PhotoListTest : DynaTest({
 *   lateinit var photoList: PhotoList
 *   beforeAll { photoList = PhotoList() }
 *
 *   group("tests of the `list()` method") {
 *     test("initially the list must be empty") {
 *       expect(true) { photoList.list().isEmpty }
 *     }
 *   }
 *   ...
 * })
 * ```
 * @param block add groups and tests within this block, to register them to a test suite.
 */
@Testable
abstract class DynaTest(block: DynaNodeGroup.()->Unit) {
    /**
     * The "root" group which will nest all groups and tests produced by the initialization block.
     */
    internal val root = DynaNodeGroup(javaClass.simpleName, StackTraceElement(javaClass.name, "<init>", null, -1))
    init {
        root.block()
    }

    @Testable
    fun blank() {
        // must  be here, otherwise Intellij won't launch this class as a test (via rightclick).
    }
}

/**
 * Computes the pointer to the source of the test and returns it.
 * @return the pointer to the test source; returns null if the source can not be computed by any means.
 */
internal fun computeTestSource(): StackTraceElement? {
    val stackTrace = Thread.currentThread().stackTrace
    if (stackTrace.size < 4) return null
    val caller: StackTraceElement = stackTrace[3]
    return caller
}
