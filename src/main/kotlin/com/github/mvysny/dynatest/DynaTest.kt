package com.github.mvysny.dynatest

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.platform.commons.annotation.Testable
import org.junit.platform.engine.TestSource
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.FilePosition
import org.junit.platform.engine.support.descriptor.FileSource
import java.io.File

/**
 * The test execution context. In addition to running the test, it remembers what to run before every test, after every test, etc.
 */
internal class TestContext(val parent: TestContext? = null) {
    /**
     * What to run before every test.
     */
    val beforeEach = mutableListOf<()->Unit>()
    /**
     * What to run after every test.
     */
    val afterEach = mutableListOf<()->Unit>()
    private fun invokeBefore() {
        parent?.invokeBefore()
        beforeEach.forEach { it() }
    }
    private fun invokeAfter() {
        afterEach.forEach { it() }
        parent?.invokeAfter()
    }

    /**
     * Runs a [test] and makes sure that [beforeEach]/[afterEach] blocks were run properly.
     */
    fun runTest(test: ()->Unit) {
        invokeBefore()
        try {
            test()
        } finally {
            invokeAfter()
        }
    }
    companion object {
        val EMPTY = TestContext(null)
    }
}

/**
 * A definition of a test graph node, either a group or a concrete test. Since we can't run tests right when [DynaNodeGroup.test]
 * is called (because it's the job of JUnit5 to actually run tests), we need to remember the test so that we can tell JUnit5 to run it
 * later on.
 *
 * Every [DynaNodeGroup.test] and [DynaNodeGroup.group] call
 * creates this node which in turn can be converted to JUnit5's [DynamicNode] by the means of the [toDynamicNode] function.
 */
sealed class DynaNode(internal val name: String, protected val ctx: TestContext, internal val src: TestSource?) {
    /**
     * Converts this node to JUnit5's [DynamicNode]. We will pass the list of these nodes to JUnit5 so that it can run the tests contained
     * within those nodes.
     */
    internal abstract fun toDynamicNode(): DynamicNode

    /**
     * Only used from the [runTests] functions when the tests are running outside of JUnit5. Typically not used.
     */
    internal abstract fun runTests()
}

/**
 * Represents a single test with a [name], an execution [context] and the test's [body]. Created when you call [DynaNodeGroup.test].
 *
 * To start writing tests, just extend [DynaTest]. See [DynaTest] for more details.
 */
class DynaNodeTest internal constructor(name: String, context: TestContext, private val body: ()->Unit, src: TestSource?) : DynaNode(name, context, src) {
    override fun toDynamicNode(): DynamicNode = DynamicTest.dynamicTest(name, { runTests() })
    override fun runTests() {
        ctx.runTest(body)
    }
}

/**
 * Represents a single test group with a [name]. Created when you call [group].
 *
 * To start writing tests, just extend [DynaTest]. See [DynaTest] for more details.
 */
class DynaNodeGroup internal constructor(name: String, ctx: TestContext, src: TestSource?) : DynaNode(name, ctx, src) {
    internal val nodes = mutableListOf<DynaNode>()
    override fun toDynamicNode(): DynamicNode = DynamicContainer.dynamicContainer(name, nodes.map { it.toDynamicNode() })
    /**
     * Generates a test case with given [name] and registers it within this group. Does not run the test case immediately -
     * the test is only registered for being run later on by JUnit5 runner (or by [runTests]).
     * @param body run when the test case is run
     */
    fun test(name: String, body: ()->Unit) {
        val source = guessTestSource()
        nodes.add(DynaNodeTest(name, ctx, body, source))
    }
    fun group(name: String, block: DynaNodeGroup.()->Unit) {
        val source = guessTestSource()
        val group = DynaNodeGroup(name, TestContext(ctx), source)
        group.block()
        nodes.add(group)
    }

    /**
     * Registers a block which will be run before every test registered to this group and to any nested groups.
     * @param block the block to run. Any exceptions thrown by the block will make the test fail. @todo mavi run afterEach in case of failure?
     */
    fun beforeEach(block: ()->Unit) {
        ctx.beforeEach.add(block)
    }
    fun afterEach(block: ()->Unit) {
        ctx.afterEach.add(block)
    }
    override fun runTests() {
        nodes.forEach { it.runTests() }
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
    internal val root = DynaNodeGroup(javaClass.simpleName, TestContext.EMPTY, ClassSource.from(javaClass))
    init {
        root.block()
    }

    // don't remove this method, it is important for having a mixed set of both JUnit5 tests and DynaTests
    @TestFactory
    fun tests(): List<DynamicNode> = root.nodes.map { it.toDynamicNode() }
}

/**
 * A very simple test support, simply runs all registered tests immediately; bails out at first failed test. You generally should rely on
 * JUnit5 to run your tests instead - just extend [DynaTest] class. To create a reusable test battery just define an extension method on the
 * [DynaNodeGroup] class - see the `CalculatorTest.kt` file for more details.
 */
fun runTests(block: DynaNodeGroup.()->Unit) {
    val group = DynaNodeGroup("root", TestContext.EMPTY, null)
    group.block()
    group.runTests()
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
