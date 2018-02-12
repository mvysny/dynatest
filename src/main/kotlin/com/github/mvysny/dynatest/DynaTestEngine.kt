package com.github.mvysny.dynatest

import org.junit.platform.engine.*
import org.junit.platform.engine.discovery.ClassSelector
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.ClassSource

/**
 * Since dynamic tests suck, I'll implement my own Test Engine.
 *
 */
class DynaTestEngine : TestEngine {
    override fun discover(discoveryRequest: EngineDiscoveryRequest, uniqueId: UniqueId): TestDescriptor {
        // todo obey the filters+selectors
        println("${discoveryRequest.getFiltersByType(DiscoveryFilter::class.java)}")
        println("${discoveryRequest.getSelectorsByType(DiscoverySelector::class.java)}")
        var tests = discoveryRequest.getSelectorsByType(ClassSelector::class.java).map { it.className }
        // todo discover all *Test classes
        if (tests.isEmpty()) tests = listOf("com.github.mvysny.dynatest.DynaTestTest")

        val result = ClassListTestDescriptor(uniqueId)
        tests.map { Class.forName(it).newInstance() as DynaTest } .forEach { result.addChild(it.toTestDescriptor(result.uniqueId)) }
        println(result.children)
        return result
    }

    override fun getId() = "dynatest"

    override fun execute(request: ExecutionRequest) {

        fun runtest(td: TestDescriptor) {
            request.engineExecutionListener.executionStarted(td)
            td.children.forEach { runtest(it) }
            try {
                td.runTest()
                request.engineExecutionListener.executionFinished(td, TestExecutionResult.successful())
            } catch (t: Throwable) {
                request.engineExecutionListener.executionFinished(td, TestExecutionResult.failed(t))
            }
        }

        runtest(request.rootTestDescriptor)
    }
}

internal class ClassListTestDescriptor(uniqueId: UniqueId) : AbstractTestDescriptor(uniqueId, "root") {
    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.CONTAINER
}

internal fun DynaNode.getId(parent: UniqueId): UniqueId {
    val segmentType = when(this) {
        is DynaNodeTest -> "test"
        is DynaNodeGroup -> "group"
    }
    return parent.append(segmentType, name)
}

internal class DynaNodeTestDescriptor(parentId: UniqueId, val node: DynaNode, src: TestSource? = node.src) : AbstractTestDescriptor(node.getId(parentId), node.name, src) {
    override fun getType(): TestDescriptor.Type = when (node) {
        is DynaNodeGroup -> TestDescriptor.Type.CONTAINER
        is DynaNodeTest -> TestDescriptor.Type.TEST
    }
}

internal fun DynaTest.toTestDescriptor(parentId: UniqueId): TestDescriptor =
    root.toTestDescriptor(parentId, ClassSource.from(javaClass))

internal fun DynaNode.toTestDescriptor(parentId: UniqueId, src: TestSource? = this.src): TestDescriptor {
    val result = DynaNodeTestDescriptor(parentId, this, src)
    if (this is DynaNodeGroup) {
        nodes.forEach { result.addChild(it.toTestDescriptor(result.uniqueId)) }
    }
    return result
}

internal fun TestDescriptor.runTest(): Unit = when {
    this is DynaNodeTestDescriptor && node is DynaNodeTest -> node.runTests()
    else -> Unit // do nothing
}
