package com.github.mvysny.dynatest

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

internal data class TestContext(val beforeEach: List<()->Unit>, val afterEach: List<()->Unit>) {
    fun invoke(block: ()->Unit) {
        beforeEach.forEach { it() }
        try {
            block()
        } finally {
            afterEach.forEach { it() }
        }
    }
    companion object {
        val EMPTY = TestContext(listOf(), listOf())
    }
}
internal operator fun TestContext.plus(other: TestContext) = TestContext(beforeEach + other.beforeEach, other.afterEach + afterEach)

sealed class DynaNode(protected val name: String, protected val ctx: TestContext) {
    internal abstract fun toDynamicNode(): DynamicNode
}
class DynaNodeTest internal constructor(name: String, ctx: TestContext, private val body: ()->Unit) : DynaNode(name, ctx) {
    override fun toDynamicNode(): DynamicNode = DynamicTest.dynamicTest(name, { ctx.invoke(body) })
}
class DynaNodeGroup internal constructor(name: String, ctx: TestContext) : DynaNode(name, ctx) {
    internal val nodes = mutableListOf<DynaNode>()
    private val beforeEach = mutableListOf<()->Unit>()
    private val afterEach = mutableListOf<()->Unit>()
    override fun toDynamicNode(): DynamicNode = DynamicContainer.dynamicContainer(name, nodes.map { it.toDynamicNode() })
    fun test(name: String, body: ()->Unit) {
        nodes.add(DynaNodeTest(name, ctx + TestContext(beforeEach, afterEach), body))
    }
    fun group(name: String, block: DynaNodeGroup.()->Unit) {
        val group = DynaNodeGroup(name, ctx + TestContext(beforeEach, afterEach))
        group.block()
        nodes.add(group)
    }
    fun beforeEach(block: ()->Unit) {
        beforeEach.add(block)
    }
    fun afterEach(block: ()->Unit) {
        afterEach.add(block)
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
