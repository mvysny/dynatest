package com.github.mvysny.dynatest.engine

import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.mvysny.dynatest.DynaNodeTest
import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.Outcome
import org.junit.platform.commons.util.ReflectionUtils
import org.junit.platform.engine.*
import org.junit.platform.engine.discovery.*
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.hierarchical.EngineExecutionContext
import org.junit.platform.engine.support.hierarchical.Node
import java.lang.reflect.Modifier
import java.util.*
import java.util.function.Predicate

/**
 * Since JUnit5's dynamic tests lack the necessary features, I'll implement my own Test Engine. In particular, JUnit5's dynamic tests:
 * * do not allow to reference the pointer to the source code of the test accurately: https://github.com/junit-team/junit5/issues/1293
 * * do not support beforeGroup/afterGroup: https://github.com/junit-team/junit5/issues/1292
 */
public class DynaTestEngine : TestEngine {

    private val classFilter: Predicate<Class<*>> = Predicate { it.isPublic && !it.isAbstract && DynaTest::class.java.isAssignableFrom(it) }

    override fun discover(request: EngineDiscoveryRequest, uniqueId: UniqueId): TestDescriptor {
        // this functionDynamicNode must never fail, otherwise JUnit5 will silently ignore this TestEngine and the user will never know.
        // that's why we will wrap any exception thrown by this method into a specialized, always failing TestDescriptor.
        // see https://github.com/gradle/gradle/issues/4418 for more details.

        fun buildClassNamePredicate(request: EngineDiscoveryRequest): Predicate<String> {
            val filters = ArrayList<DiscoveryFilter<String>>()
            filters.addAll(request.getFiltersByType(ClassNameFilter::class.java))
            filters.addAll(request.getFiltersByType(PackageNameFilter::class.java))
            return Filter.composeFilters<String>(filters).toPredicate()
        }

        try {
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

            // filter out non-DynaTest classes as per https://github.com/gradle/gradle/issues/4418
            classes
                .filter { DynaTest::class.java.isAssignableFrom(it) }
                .forEach {
                    try {
                        // let's use newInstance() since the recommended way of getDeclaredConstructor().newInstance()
                        // wraps any checked exceptions in InvocationTargetException which we really don't want in Kotlin :)
                        @Suppress("DEPRECATION")
                        val test: DynaTest = it.newInstance() as DynaTest
                        val td = DynaNodeTestDescriptor(result.uniqueId, test.root)
                        result.addChild(td)
                        test.root.onDesignPhaseEnd()
                    } catch (t: Throwable) {
                        result.addChild(InitFailedTestDescriptor(result.uniqueId, it, t))
                    }
                }
            return result

        } catch (t: Throwable) {
            return InitFailedTestDescriptor(uniqueId, DynaTestEngine::class.java, t)
        }
    }

    override fun getId(): String = "DynaTest"

    override fun execute(request: ExecutionRequest) {

        fun TestDescriptor.engineExecutionListenerExecutionSuccess() {
            val skipped = (this as? DynaNodeTestDescriptor)?.isEnabled == false
            if (!skipped) {
                request.engineExecutionListener.executionFinished(
                    this,
                    TestExecutionResult.successful()
                )
            } else {
                request.engineExecutionListener.executionSkipped(
                    this,
                    "x"
                )
            }
        }

        /**
         * Runs all tests defined in this descriptor. This function does not throw exception if any of the
         * test/beforeEach/beforeAll/afterEach/afterAll fails.
         */
        fun TestDescriptor.runAllTests() {
            // mark test started
            request.engineExecutionListener.executionStarted(this)

            // if this test descriptor denotes a DynaNodeGroup, run all `beforeGroup` blocks.
            try {
                (this as? DynaNodeTestDescriptor)?.runBeforeGroup()
            } catch (t: Throwable) {
                // one of the `beforeGroup` failed; do not run anything in this group (but still run all afterGroup blocks in this group)
                // mark the group as failed.
                (this as? DynaNodeTestDescriptor)?.runAfterGroup(t)
                request.engineExecutionListener.executionFinished(this, TestExecutionResult.failed(t))
                // bail out, we're done.
                return
            }

            // beforeGroup ran successfully, continue with the normal test execution.
            children.forEach { childDescriptor -> childDescriptor.runAllTests() }

            try {
                if (this is DynaNodeTestDescriptor && this.isTest) {
                    runTest()
                } else if (this is InitFailedTestDescriptor) {
                    throw RuntimeException(failure)
                }
                (this as? DynaNodeTestDescriptor)?.runAfterGroup(null)
                engineExecutionListenerExecutionSuccess()
            } catch (t: Throwable) {
                request.engineExecutionListener.executionFinished(this, TestExecutionResult.failed(t))
            }
        }

        request.rootTestDescriptor.runAllTests()
    }
}

/**
 * A container which hosts all DynaTest test classes wrapped in [DynaNodeTestDescriptor]s - they then in turn host individual groups and tests.
 * Returned by [DynaTestEngine.discover].
 */
internal class ClassListTestDescriptor(uniqueId: UniqueId) : AbstractTestDescriptor(uniqueId, "DynaTest") {
    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.CONTAINER
}

/**
 * Computes [UniqueId] for given [node], from the ID of the parent.
 * @receiver the parent ID.
 */
private fun UniqueId.append(node: DynaNodeImpl): UniqueId {
    val segmentType = when(node) {
        is DynaNodeTestImpl -> "test"
        is DynaNodeGroupImpl -> "group"
    }
    return append(segmentType, node.name)
}

internal class DynaNodeTestDescriptor(parentId: UniqueId, val node: DynaNodeImpl) :
    AbstractTestDescriptor(parentId.append(node), node.name, node.toTestSource()), Node<EngineExecutionContext> {
    init {
        if (node is DynaNodeGroup) {
            // register children even if this group is disabled
            // this will cause Intellij to display those tests
            (node as DynaNodeGroupImpl).children.forEach { addChild(DynaNodeTestDescriptor(uniqueId, it)) }
        }
    }

    val isEnabled: Boolean get() = node.enabled

    override fun getType(): TestDescriptor.Type = when (node) {
        is DynaNodeGroupImpl -> TestDescriptor.Type.CONTAINER
        is DynaNodeTestImpl -> TestDescriptor.Type.TEST
    }

    fun runBeforeGroup() {
        if (node is DynaNodeGroup && isEnabled) {
            (node as DynaNodeGroupImpl).beforeGroup.forEach { it(Unit) }
        }
    }

    /**
     * Runs all [DynaNodeGroupImpl.afterGroup] on [node], even if some if the blocks fails with an exception.
     * @param t if not null, the test has failed with this exception.
     * @throws Throwable if any of the block fails or [t] was not null.
     */
    fun runAfterGroup(t: Throwable?) {
        var tf = t
        if (node is DynaNodeGroup && isEnabled) {
            (node as DynaNodeGroupImpl).afterGroup.forEach {
                try {
                    it(Unit, Outcome(null, tf))
                } catch (ex: Throwable) {
                    if (tf == null) tf = ex else tf!!.addSuppressed(ex)
                }
            }
        }
        if (tf != null && t == null) throw tf!!
    }

    /**
     * Runs given [block], properly prefixed with calls to `beforeEach` blocks and postfixed with calls to `afterEach` blocks.
     * If any of those fails, does a proper cleanup and then throws the exception.
     */
    private fun runBlock(testName: String, block: () -> Unit) {
        var lastNodeWithBeforeEachRan: DynaNodeTestDescriptor? = null
        try {
            getPathFromRoot().forEach { descriptor ->
                lastNodeWithBeforeEachRan = descriptor
                if (descriptor.node is DynaNodeGroup) {
                    (descriptor.node as DynaNodeGroupImpl).beforeEach.forEach { it(Unit) }
                }
            }
            block()
        } catch(t: Throwable) {
            lastNodeWithBeforeEachRan?.runAfterEach(testName, t)
            throw t
        }
        lastNodeWithBeforeEachRan?.runAfterEach(testName, null)
    }

    fun runTest() {
        if (isEnabled) {
            runBlock(node.name) { (node as DynaNodeTestImpl).body(node) }
        }
    }

    /**
     * Computes the path of dyna nodes from the root group towards this one.
     */
    private fun getPathFromRoot(): List<DynaNodeTestDescriptor> =
        generateSequence(this, { it: DynaNodeTestDescriptor -> it.parent.orElse(null) as? DynaNodeTestDescriptor })
                .toList()
                .reversed()

    /**
     * Runs all `afterEach` blocks recursively, from this node all the way up to the root node. Properly propagates exceptions.
     * @param testFailure if not null, the test has failed with this exception.
     */
    private fun runAfterEach(testName: String, testFailure: Throwable?) {
        var tf: Throwable? = testFailure
        if (isEnabled) {
            if (node is DynaNodeGroup) {
                (node as DynaNodeGroupImpl).afterEach.forEach { afterEachBlock: Unit.(Outcome) -> Unit ->
                    try {
                        afterEachBlock(Unit, Outcome(testName, tf))
                    } catch (t: Throwable) {
                        if (tf == null) tf = t else tf!!.addSuppressed(t)
                    }
                }
            }
            (parent.orElse(null) as? DynaNodeTestDescriptor)?.runAfterEach(
                testName,
                tf
            )
        }
        if (testFailure == null && tf != null) throw tf!!
    }

    override fun shouldBeSkipped(context: EngineExecutionContext): Node.SkipResult = when {
        node.enabled -> Node.SkipResult.doNotSkip()
        else -> Node.SkipResult.skip("x")
    }
}

/**
 * Returns true if this class is private.
 */
public val Class<*>.isPrivate: Boolean get() = Modifier.isPrivate(modifiers)
/**
 * Returns true if this class is abstract.
 */
public val Class<*>.isAbstract: Boolean get() = Modifier.isAbstract(modifiers)
/**
 * Returns true if this class is public.
 */
public val Class<*>.isPublic: Boolean get() = Modifier.isPublic(modifiers)

/**
 * When the [DynaTest]'s block fails to run properly and produce tests, [DynaTestEngine.discover] will return this test descriptor to mark
 * the whole DynaTest as failed. Even more, the whole [DynaTestEngine.discover] method is wrapped in try-catch which will produce this test
 * descriptor on failure. This way, the [DynaTestEngine.discover] method never fails (which is very important: see https://github.com/gradle/gradle/issues/4418
 * for more details).
 */
internal class InitFailedTestDescriptor(parentId: UniqueId, clazz: Class<*>, val failure: Throwable) :
    AbstractTestDescriptor(parentId.append("class", clazz.simpleName), clazz.simpleName, ClassSource.from(clazz)) {

    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.TEST
}
