package com.github.mvysny.dynatest

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

internal class TestContext(val parent: TestContext? = null) {
    val beforeEach = mutableListOf<()->Unit>()
    val afterEach = mutableListOf<()->Unit>()
    private fun invokeBefore() {
        parent?.invokeBefore()
        beforeEach.forEach { it() }
    }
    private fun invokeAfter() {
        afterEach.forEach { it() }
        parent?.invokeAfter()
    }
    fun invoke(block: ()->Unit) {
        invokeBefore()
        try {
            block()
        } finally {
            invokeAfter()
        }
    }
    companion object {
        val EMPTY = TestContext(null)
    }
}

sealed class DynaNode(protected val name: String, protected val ctx: TestContext) {
    internal abstract fun toDynamicNode(): DynamicNode
    internal abstract fun runTests()
}
class DynaNodeTest internal constructor(name: String, ctx: TestContext, private val body: ()->Unit) : DynaNode(name, ctx) {
    override fun toDynamicNode(): DynamicNode = DynamicTest.dynamicTest(name, { runTests() })
    override fun runTests() {
        ctx.invoke(body)
    }
}
class DynaNodeGroup internal constructor(name: String, ctx: TestContext) : DynaNode(name, ctx) {
    internal val nodes = mutableListOf<DynaNode>()
    override fun toDynamicNode(): DynamicNode = DynamicContainer.dynamicContainer(name, nodes.map { it.toDynamicNode() })
    /**
     * Generates a test case with given [name] and registers it within this group. Does not run the test case immediately -
     * the test is only registered for being run later on by JUnit5 runner (or by [runTests]).
     * @param body run when the test case is run
     */
    fun test(name: String, body: ()->Unit) {
        nodes.add(DynaNodeTest(name, ctx, body))
    }
    fun group(name: String, block: DynaNodeGroup.()->Unit) {
        val group = DynaNodeGroup(name, TestContext(ctx))
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

abstract class DynaTest(block: DynaNodeGroup.()->Unit) {
    private val root = DynaNodeGroup(javaClass.simpleName, TestContext.EMPTY)
    init {
        root.block()
    }

    @TestFactory
    fun tests(): List<DynamicNode> = root.nodes.map { it.toDynamicNode() }
}

/**
 * A very simple test support, simply runs all registered tests immediately; bails out at first failed test.
 */
fun runTests(block: DynaNodeGroup.()->Unit) {
    val group = DynaNodeGroup("root", TestContext.EMPTY)
    group.block()
    group.runTests()
}
