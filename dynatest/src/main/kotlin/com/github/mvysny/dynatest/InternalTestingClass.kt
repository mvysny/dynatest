package com.github.mvysny.dynatest

import com.github.mvysny.dynatest.engine.DynaNodeGroupImpl
import com.github.mvysny.dynatest.engine.toTestSource
import org.junit.platform.engine.support.descriptor.ClassSource

/**
 * This whole file is only for the purposes of testing of resolving class name to TestSource. Please ignore this file.
 */


internal fun internalTestingClassGetTestSourceOfThis(): StackTraceElement = DynaNodeGroupImpl.computeTestSource()!!

internal class InternalTestingClass {
    companion object {
        @JvmStatic
        internal fun getTestSourceOfThis(): ClassSource = DynaNodeGroupImpl.computeTestSource()!!.toTestSource() as ClassSource

        /**
         * A nasty test. This test will make Gradle freeze after last test.
         * A Test for https://github.com/gradle/gradle/issues/5737
         */
        @JvmStatic
        internal fun gradleFreezingTest(g: DynaNodeGroup) {
            g.test("because ") {}
        }
    }
}
