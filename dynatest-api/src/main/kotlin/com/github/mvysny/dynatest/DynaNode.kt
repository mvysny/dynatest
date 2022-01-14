package com.github.mvysny.dynatest

/**
 * Makes sure that you can't call [DynaNodeGroup] methods from the scope of the [DynaNodeTest].
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
@DslMarker
public annotation class DynaTestDsl

@Deprecated("Replace by DynaTestDsl", ReplaceWith("DynaTestDsl"))
public typealias DynaNodeDsl = DynaTestDsl

/**
 * A group of tests, may contain tests and other groups as well. Created when you call [group]. Allows you to control test
 * lifecycle:
 * * [beforeEach]/[afterEach] adds action that need to be executed before/after every test, in this group and all subgroups
 * * [beforeGroup]/[afterGroup] adds action that need to be executed before/after all tests in this group and all subgroups are ran
 *
 * To start writing tests, just extend the `DynaTest` class in dynatest-engine. See `DynaTest` class documentation for more details.
 *
 * *Warning*: the methods may only be called when the `DynaTest` class is constructed. None of these methods can be called when the
 * tests are being run. Doing so will throw [IllegalStateException].
 *
 * **Implementation note:** The weird `(@DynaNodeDsl Unit)` receiver is only used to prevent
 * calling e.g. `test {}` from `beforeGroup {}`. Whenever messing around with the DSL annotations,
 * please make sure to look at the `UncompilableTest.kt` file.
 */
@DynaTestDsl
public interface DynaNodeGroup {
    /**
     * Creates a new test case with given [name] and registers it within current group. Does not run the test closure immediately -
     * the test is only registered for being run later on by JUnit5 runner.
     * @param body the implementation of the test; does not run immediately but only when the test case is run
     * @throws IllegalStateException if this method is called when the tests are being run by JUnit.
     */
    @DynaTestDsl
    public fun test(name: String, body: DynaNodeTest.() -> Unit)

    /**
     * Creates a nested group with given [name] and runs given [block]. In the block, you can create both sub-groups and tests, and you can
     * mix those freely as you like.
     * @param block the block, runs immediately.
     * @throws IllegalStateException if this method is called when the tests are being run by JUnit.
     */
    @DynaTestDsl
    public fun group(name: String, block: DynaNodeGroup.() -> Unit)

    /**
     * Creates a disabled test case with given [name] and registers it within current group. Does not run the test closure -
     * the test is only registered but not run later on by JUnit5 runner.
     *
     * Useful for temporarily disabling a test.
     * @param body the implementation of the test; does not run immediately but only when the test case is run
     * @throws IllegalStateException if this method is called when the tests are being run by JUnit.
     */
    @DynaTestDsl
    public fun xtest(name: String, body: DynaNodeTest.() -> Unit)

    /**
     * Creates a nested disabled group with given [name] and runs given [block].
     * In the block, you can create both sub-groups and tests, and you can
     * mix those freely as you like.
     *
     * None of the child tests are run. Useful for temporarily disabling a group of tests.
     * @param block the block, runs immediately.
     * @throws IllegalStateException if this method is called when the tests are being run by JUnit.
     */
    @DynaTestDsl
    public fun xgroup(name: String, block: DynaNodeGroup.() -> Unit)

    /**
     * Registers a block which will be run exactly once before any of the tests in the current group are run.
     * Only the tests nested in this group and its subgroups are
     * considered.
     *
     * If this group is enabled, these blocks will be run regardless of whether there
     * are any child tests/groups in this group or not.
     * @param block the block to run. Any exceptions thrown by the block will make the test fail.
     * @throws IllegalStateException if this method is called when the tests are being run by JUnit.
     */
    @DynaTestDsl
    public fun beforeGroup(block: (@DynaTestDsl Unit).() -> Unit)

    /**
     * Registers a block which will be run before every test registered to this group and to any nested groups.
     * `beforeEach` blocks registered by a parent/ancestor group runs before `beforeEach` blocks registered by this group.
     *
     * If any of the `beforeEach` blocks fails, no further `beforeEach` blocks are executed; furthermore the test itself is not executed as well.
     * However, all of the [afterEach] blocks for the corresponding group and all parent groups still *are* executed.
     *
     * If this test or any parent group is disabled, these blocks will not run.
     * @param block the block to run. Any exceptions thrown by the block will make the test fail.
     * @throws IllegalStateException if this method is called when the tests are being run by JUnit.
     */
    @DynaTestDsl
    public fun beforeEach(block: (@DynaTestDsl Unit).() -> Unit)

    /**
     * Registers a [block] which will be run after every test registered to this group and to any nested groups.
     * All `afterEach` blocks registered by a parent/ancestor group runs after `afterEach` blocks registered by this group.
     *
     * The `afterEach` blocks are called even if the test fails. If the `beforeEach` block fails, only the `afterEach` blocks in the corresponding
     * group and all ancestor groups are called.
     *
     * If any of the `afterEach` blocks throws an exception, those exceptions are added as [Throwable.getSuppressed] to the main exception (as thrown
     * by the `beforeEach` block or the test itself); or just rethrown if there is no main exception. Any exception thrown by the `afterEach`
     * block will cause the test to fail.
     *
     * If this test or any parent group is disabled, these blocks will not run.
     * @param block the block to run. Any exceptions thrown by the block will make the test fail.
     * The block receives an [Outcome] of the test run.
     * @throws IllegalStateException if this method is called when the tests are being run by JUnit.
     */
    @DynaTestDsl
    public fun afterEach(block: (@DynaTestDsl Unit).(outcome: @DynaTestDsl Outcome) -> Unit)

    /**
     * Registers a block which will be run only once after all of the tests are run in the current group. Only the tests nested in this group and its subgroups are
     * considered.
     *
     * If this group is enabled, these blocks will be run regardless of whether there
     * are any child tests/groups in this group or not.
     * @param block the block to run. Any exceptions thrown by the block will make the test fail.
     * The block receives an [Outcome] of the test run.
     * @throws IllegalStateException if this method is called when the tests are being run by JUnit.
     */
    @DynaTestDsl
    public fun afterGroup(block: (@DynaTestDsl Unit).(outcome: @DynaTestDsl Outcome) -> Unit)
}

/**
 * Represents a single test with a name and a body block. Created when you call [DynaNodeGroup.test].
 *
 * To start writing tests, just extend the `DynaTest` class located in the `dynatest-engine` module. See `DynaTest` for more details.
 *
 * The main purpose of this interface is to serve as a DSL marker, to prevent
 * calling e.g. `test {}` from `beforeGroup {}`.
 */
@DynaTestDsl
public interface DynaNodeTest
