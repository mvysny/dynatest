package com.github.mvysny.dynatest

import org.junit.platform.commons.annotation.Testable
import org.junit.platform.engine.TestSource
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.FilePosition
import org.junit.platform.engine.support.descriptor.FileSource
import java.io.File

/**
 * A definition of a test graph node, either a group or a concrete test. Since we can't run tests right when [DynaNodeGroup.test]
 * is called (because it's the job of JUnit5 to actually run tests), we need to remember the test so that we can tell JUnit5 to run it
 * later on.
 *
 * Every [DynaNodeGroup.test] and [DynaNodeGroup.group] call
 * creates this node which in turn can be converted to JUnit5 structures eligible for execution.
 */
sealed class DynaNode(internal val name: String, internal val src: TestSource?)

/**
 * Represents a single test with a [name], an execution [context] and the test's [body]. Created when you call [DynaNodeGroup.test].
 *
 * To start writing tests, just extend [DynaTest]. See [DynaTest] for more details.
 */
class DynaNodeTest internal constructor(name: String, internal val body: ()->Unit, src: TestSource?) : DynaNode(name, src)

/**
 * Represents a single test group with a [name]. Created when you call [group].
 *
 * To start writing tests, just extend [DynaTest]. See [DynaTest] for more details.
 */
class DynaNodeGroup internal constructor(name: String, src: TestSource?) : DynaNode(name, src) {
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

    /**
     * Creates a new test case with given [name] and registers it within current group. Does not run the test closure immediately -
     * the test is only registered for being run later on by JUnit5 runner (or by [runTests]).
     * @param body the implementation of the test; does not run immediately but only when the test case is run
     */
    fun test(name: String, body: ()->Unit) {
        val source = computeTestSource()
        children.add(DynaNodeTest(name, body, source))
    }

    /**
     * Creates a nested group with given [name] and runs given [block]. In the block, you can create both sub-groups and tests, and you can
     * mix those freely as you like.
     * @param block the block, runs immediately.
     */
    fun group(name: String, block: DynaNodeGroup.()->Unit) {
        val source = computeTestSource()
        val group = DynaNodeGroup(name, source)
        group.block()
        children.add(group)
    }

    /**
     * Registers a block which will be run before every test registered to this group and to any nested groups.
     * @param block the block to run. Any exceptions thrown by the block will make the test fail.
     */
    fun beforeEach(block: ()->Unit) {
        beforeEach.add(block)
    }

    /**
     * Registers a block which will be run after every test registered to this group and to any nested groups.
     * @param block the block to run. Any exceptions thrown by the block will make the test fail.
     */
    fun afterEach(block: ()->Unit) {
        afterEach.add(block)
    }

    /**
     * Registers a block which will be run exactly once before any of the tests are run. Only the tests nested in this group and its subgroups are
     * @param block the block to run. Any exceptions thrown by the block will make the test fail.
     */
    fun beforeAll(block: ()->Unit) {
        beforeAll.add(block)
    }

    /**
     * Registers a block which will be run only once after all of the tests are run. Only the tests nested in this group and its subgroups are
     * considered.
     * @param block the block to run. Any exceptions thrown by the block will make the test fail.
     */
    fun afterAll(block: ()->Unit) {
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
 */
@Testable
abstract class DynaTest(block: DynaNodeGroup.()->Unit) {
    internal val root = DynaNodeGroup(javaClass.simpleName, ClassSource.from(javaClass))
    init {
        root.block()
    }

    @Testable
    fun blank() {
        // must  be here, otherwise Intellij won't launch this class as a test (via rightclick).
    }
}

/**
 * Computes the pointer to the source of the test and returns it. Tries to compute at least inaccurate pointer.
 * @return the pointer to the test source; returns null if the source can not be computed by any means.
 */
internal fun computeTestSource(): TestSource? {
    val stackTrace = Thread.currentThread().stackTrace
    if (stackTrace.size < 4) return null
    val caller: StackTraceElement = stackTrace[3]
    // normally we would just return ClassSource, but there are the following issues with that:
    // 1. Intellij ignores FilePosition in ClassSource; reported as https://youtrack.jetbrains.com/issue/IDEA-186581
    // 2. If I try to remedy that by passing in the block class name (such as DynaTestTest$1$1$1$1), Intellij looks confused and won't perform any navigation
    // 3. FileSource seems to work very well.

    // Try to guess the absolute test file name from the file class. It should be located somewhere in src/main/kotlin or src/main/java
    if (!caller.fileName.isNullOrBlank() && caller.fileName.endsWith(".kt")) {
        val folders = listOf("java", "kotlin").map { File("src/test/$it").absoluteFile } .filter { it.exists() }
        val pkg = caller.className.replace('.', '/').replaceAfterLast('/', "", "").trim('/')
        val file: File? = folders.map { File(it, "$pkg/${caller.fileName}") } .firstOrNull { it.exists() }
        if (file != null) return FileSource.from(file, FilePosition.from(caller.lineNumber))
    }
    // ClassSource doesn't work on classes named DynaTestTest$1$1$1$1 (with $ in them); strip that.
    return ClassSource.from(caller.className.replaceAfter('$', "").trim('$'))
}
