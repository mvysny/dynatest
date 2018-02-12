package com.github.mvysny.dynatest

import org.junit.platform.commons.util.ReflectionUtils
import org.junit.platform.engine.*
import org.junit.platform.engine.discovery.*
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.ClassSource
import java.lang.reflect.Modifier
import java.util.ArrayList
import java.util.function.Predicate

/**
 * Since dynamic tests suck, I'll implement my own Test Engine.
 */
class DynaTestEngine : TestEngine {

    private val classFilter: Predicate<Class<*>> = Predicate { it.isPublic && !it.isAbstract && DynaTest::class.java.isAssignableFrom(it) }

    override fun discover(request: EngineDiscoveryRequest, uniqueId: UniqueId): TestDescriptor {

        fun buildClassNamePredicate(request: EngineDiscoveryRequest): Predicate<String> {
            val filters = ArrayList<DiscoveryFilter<String>>()
            filters.addAll(request.getFiltersByType(ClassNameFilter::class.java))
            filters.addAll(request.getFiltersByType(PackageNameFilter::class.java))
            return Filter.composeFilters<String>(filters).toPredicate()
        }

        val classNamePredicate = buildClassNamePredicate(request)
        val classes = mutableSetOf<Class<*>>()

        request.getSelectorsByType(ClasspathRootSelector::class.java).forEach { selector ->
            ReflectionUtils.findAllClassesInClasspathRoot(
                selector.classpathRoot, classFilter,
                classNamePredicate
            ).forEach { classes.add(it) }
        }
        request.getSelectorsByType(PackageSelector::class.java).forEach { selector ->
            ReflectionUtils.findAllClassesInPackage(selector.packageName, classFilter, classNamePredicate)
                .forEach { classes.add(it) }
        }
        request.getSelectorsByType(ClassSelector::class.java).forEach { selector -> classes.add(selector.javaClass) }

        val result = ClassListTestDescriptor(uniqueId)
        classes.map { it.newInstance() as DynaTest } .forEach { result.addChild(it.toTestDescriptor(result.uniqueId)) }
        return result
    }

    override fun getId() = "DynaTest"

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

internal class ClassListTestDescriptor(uniqueId: UniqueId) : AbstractTestDescriptor(uniqueId, "DynaTest") {
    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.CONTAINER
}

internal fun DynaNode.getId(parent: UniqueId): UniqueId {
    val segmentType = when(this) {
        is DynaNodeTest -> "test"
        is DynaNodeGroup -> "group"
    }
    return parent.append(segmentType, name)
}

internal class DynaNodeTestDescriptor(parentId: UniqueId, val node: DynaNode) : AbstractTestDescriptor(node.getId(parentId), node.name, node.src) {
    override fun getType(): TestDescriptor.Type = when (node) {
        is DynaNodeGroup -> TestDescriptor.Type.CONTAINER
        is DynaNodeTest -> TestDescriptor.Type.TEST
    }
}

internal fun DynaTest.toTestDescriptor(parentId: UniqueId): TestDescriptor =
    root.toTestDescriptor(parentId)

internal fun DynaNode.toTestDescriptor(parentId: UniqueId): TestDescriptor {
    val result = DynaNodeTestDescriptor(parentId, this)
    if (this is DynaNodeGroup) {
        nodes.forEach { result.addChild(it.toTestDescriptor(result.uniqueId)) }
    }
    return result
}

internal fun TestDescriptor.runTest(): Unit = when {
    this is DynaNodeTestDescriptor && node is DynaNodeTest -> node.runTests()
    else -> Unit // do nothing
}

val Class<*>.isPrivate: Boolean get() = Modifier.isPrivate(modifiers)
val Class<*>.isAbstract: Boolean get() = Modifier.isAbstract(modifiers)
val Class<*>.isPublic: Boolean get() = Modifier.isPublic(modifiers)
