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
     * Generates a test case with given [name] and registers it within this group. Does not run the test case immediately -
     * the test is only registered for being run later on by JUnit5 runner (or by [runTests]).
     * @param body run when the test case is run
     */
    fun test(name: String, body: ()->Unit) {
        val source = guessTestSource()
        children.add(DynaNodeTest(name, body, source))
    }

    fun group(name: String, block: DynaNodeGroup.()->Unit) {
        val source = guessTestSource()
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

    fun afterEach(block: ()->Unit) {
        afterEach.add(block)
    }

    /**
     * Registers a block which will be run once before any of the tests registered to this group and to any nested groups are run.
     * @param block the block to run. Any exceptions thrown by the block will make the test fail.
     */
    fun beforeAll(block: ()->Unit) {
        beforeAll.add(block)
    }
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

internal fun guessTestSource(): TestSource? {
    val stackTrace = Thread.currentThread().stackTrace
    if (stackTrace.size < 4) return null
    val caller: StackTraceElement = stackTrace[3]
    // normally we would just return ClassSource, but there are the following issues with that:
    // 1. Intellij ignores FilePosition in ClassSource
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
