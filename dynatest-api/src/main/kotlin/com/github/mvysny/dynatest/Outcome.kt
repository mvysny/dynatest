package com.github.mvysny.dynatest

/**
 * The outcome of a test run. Vended in [DynaNodeGroup.afterEach] and [DynaNodeGroup.afterGroup].
 * @property testName the test name; `null` in the `afterGroup` block.
 * @property failureCause if not null then either the test, or one of [DynaNodeGroup.afterEach] and [DynaNodeGroup.afterGroup]
 * have failed with an exception.
 */
public data class Outcome(val testName: String?, val failureCause: Throwable?) {
    /**
     * If true then the test and all previously called [DynaNodeGroup.afterEach] and [DynaNodeGroup.afterGroup] have succeeded.
     */
    val isSuccess: Boolean get() = failureCause == null
    /**
     * If true then [isSuccess] is false.
     */
    val isFailure: Boolean get() = !isSuccess
}
