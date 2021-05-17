package com.github.mvysny.dynatest

import com.github.mvysny.dynatest.engine.DynaNodeGroupImpl
import org.junit.platform.commons.annotation.Testable

/**
 * Inherit from this class to write the tests:
 * ```
 * class PhotoListTest : DynaTest({
 *   lateinit var photoList: PhotoList
 *   beforeGroup { photoList = PhotoList() }
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
public abstract class DynaTest(block: DynaNodeGroup.()->Unit) {
    /**
     * The "root" group which will nest all groups and tests produced by the initialization block.
     */
    internal val root = DynaNodeGroupImpl(
        javaClass.simpleName,
        StackTraceElement(javaClass.name, "<init>", null, -1),
        true
    )
    init {
        root.block()
    }

    @Testable
    public fun blank() {
        // must  be here, otherwise Intellij won't launch this class as a test (via rightclick).
    }
}
