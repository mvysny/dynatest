package com.github.mvysny.dynatest

import org.junit.platform.commons.util.ReflectionUtils
import org.junit.platform.engine.*
import org.junit.platform.engine.discovery.*
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import java.lang.reflect.Modifier
import java.util.*
import java.util.function.Predicate

/**
 * Since JUnit5's dynamic tests lack the necessary features, I'll implement my own Test Engine. In particular, JUnit5's dynamic tests:
 * * do not allow to reference the pointer to the source code of the test accurately: https://github.com/junit-team/junit5/issues/1293
 * * do not support beforeAll/afterAll: https://github.com/junit-team/junit5/issues/1292
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

        // todo filter based on UniqueIdSelector when https://youtrack.jetbrains.com/issue/IDEA-169198 is fixed

        val result = ClassListTestDescriptor(uniqueId)
        classes.map { it.newInstance() as DynaTest } .forEach { result.addChild(it.toTestDescriptor(result.uniqueId)) }
        return result
    }

    override fun getId() = "DynaTest"

    override fun execute(request: ExecutionRequest) {

        fun runTest(td: DynaNodeTestDescriptor, node: DynaNodeTest) {
            td.runBeforeEach()
            try {
                node.body()
            } finally {
                td.runAfterEach()
            }
        }

        fun runAllTests(td: TestDescriptor) {
            request.engineExecutionListener.executionStarted(td)
            (td as? DynaNodeTestDescriptor)?.runBeforeAll()
            td.children.forEach { runAllTests(it) }
            try {
                if (td is DynaNodeTestDescriptor && td.node is DynaNodeTest) {
                    runTest(td, td.node)
                }
                (td as? DynaNodeTestDescriptor)?.runAfterAll()
                request.engineExecutionListener.executionFinished(td, TestExecutionResult.successful())
            } catch (t: Throwable) {
                request.engineExecutionListener.executionFinished(td, TestExecutionResult.failed(t))
            }
        }

        runAllTests(request.rootTestDescriptor)
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
    init {
        if (node is DynaNodeGroup) {
            node.children.forEach { addChild(DynaNodeTestDescriptor(uniqueId, it)) }
        }
    }

    override fun getType(): TestDescriptor.Type = when (node) {
        is DynaNodeGroup -> TestDescriptor.Type.CONTAINER
        is DynaNodeTest -> TestDescriptor.Type.TEST
    }

    fun runBeforeAll() {
        if (node is DynaNodeGroup) {
            node.beforeAll.forEach { it() }
        }
    }

    fun runAfterAll() {
        if (node is DynaNodeGroup) {
            node.afterAll.forEach { it() }
        }
    }

    fun runBeforeEach() {
        (parent.orElse(null) as? DynaNodeTestDescriptor)?.runBeforeEach()
        if (node is DynaNodeGroup) {
            node.beforeEach.forEach { it() }
        }
    }

    fun runAfterEach() {
        if (node is DynaNodeGroup) {
            node.afterEach.forEach { it() }
        }
        (parent.orElse(null) as? DynaNodeTestDescriptor)?.runAfterEach()
    }
}

internal fun DynaTest.toTestDescriptor(parentId: UniqueId): TestDescriptor =
    DynaNodeTestDescriptor(parentId, root)

val Class<*>.isPrivate: Boolean get() = Modifier.isPrivate(modifiers)
val Class<*>.isAbstract: Boolean get() = Modifier.isAbstract(modifiers)
val Class<*>.isPublic: Boolean get() = Modifier.isPublic(modifiers)
