package com.github.mvysny.dynatest

import org.junit.platform.commons.util.ReflectionUtils
import org.junit.platform.engine.*
import org.junit.platform.engine.discovery.*
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.FilePosition
import org.junit.platform.engine.support.descriptor.FileSource
import java.io.File
import java.lang.reflect.Modifier
import java.util.*
import java.util.function.Predicate

/**
 * Since JUnit5's dynamic tests lack the necessary features, I'll implement my own Test Engine. In particular, JUnit5's dynamic tests:
 * * do not allow to reference the pointer to the source code of the test accurately: https://github.com/junit-team/junit5/issues/1293
 * * do not support beforeGroup/afterGroup: https://github.com/junit-team/junit5/issues/1292
 */
class DynaTestEngine : TestEngine {

    private val classFilter: Predicate<Class<*>> = Predicate { it.isPublic && !it.isAbstract && DynaTest::class.java.isAssignableFrom(it) }

    override fun discover(request: EngineDiscoveryRequest, uniqueId: UniqueId): TestDescriptor {
        // this function must never fail, otherwise JUnit5 will silently ignore this TestEngine and the user will never know.
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

    override fun getId() = "DynaTest"

    override fun execute(request: ExecutionRequest) {

        fun runTest(td: DynaNodeTestDescriptor, node: DynaNodeTest) {
            td.runBlock { node.body(node) }
        }

        fun runAllTests(td: TestDescriptor) {
            // mark test started
            request.engineExecutionListener.executionStarted(td)

            // if this test descriptor denotes a DynaNodeGroup, run all `beforeGroup` blocks.
            try {
                (td as? DynaNodeTestDescriptor)?.runbeforeGroup()
            } catch (t: Throwable) {
                // one of the `beforeGroup` failed; do not run anything in this group (but still run all afterGroup blocks in this group)
                // mark the group as failed.
                (td as? DynaNodeTestDescriptor)?.runafterGroup(t)
                request.engineExecutionListener.executionFinished(td, TestExecutionResult.failed(t))
                // bail out, we're done.
                return
            }

            // beforeGroup ran successfully, continue with the normal test execution.
            td.children.forEach { runAllTests(it) }
            try {
                if (td is DynaNodeTestDescriptor && td.node is DynaNodeTest) {
                    runTest(td, td.node)
                } else if (td is InitFailedTestDescriptor) {
                    throw RuntimeException(td.failure)
                }
                (td as? DynaNodeTestDescriptor)?.runafterGroup(null)
                request.engineExecutionListener.executionFinished(td, TestExecutionResult.successful())
            } catch (t: Throwable) {
                request.engineExecutionListener.executionFinished(td, TestExecutionResult.failed(t))
            }
        }

        runAllTests(request.rootTestDescriptor)
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
private fun UniqueId.append(node: DynaNode): UniqueId {
    val segmentType = when(node) {
        is DynaNodeTest -> "test"
        is DynaNodeGroup -> "group"
    }
    return append(segmentType, node.name)
}

internal class DynaNodeTestDescriptor(parentId: UniqueId, val node: DynaNode) : AbstractTestDescriptor(parentId.append(node), node.name, node.src?.toTestSource()) {
    init {
        if (node is DynaNodeGroup) {
            node.children.forEach { addChild(DynaNodeTestDescriptor(uniqueId, it)) }
        }
    }

    override fun getType(): TestDescriptor.Type = when (node) {
        is DynaNodeGroup -> TestDescriptor.Type.CONTAINER
        is DynaNodeTest -> TestDescriptor.Type.TEST
    }

    fun runbeforeGroup() {
        if (node is DynaNodeGroup) {
            node.beforeGroup.forEach { it() }
        }
    }

    fun runafterGroup(t: Throwable?) {
        var tf = t
        if (node is DynaNodeGroup) {
            node.afterGroup.forEach {
                try {
                    it()
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
    fun runBlock(block: () -> Unit) {
        var lastNodeWithBeforeEachRan: DynaNodeTestDescriptor? = null
        try {
            getPathFromRoot().forEach { descriptor ->
                lastNodeWithBeforeEachRan = descriptor
                if (descriptor.node is DynaNodeGroup) {
                    descriptor.node.beforeEach.forEach { it() }
                }
            }
            block()
        } catch(t: Throwable) {
            lastNodeWithBeforeEachRan?.runAfterEach(t)
            throw t
        }
        lastNodeWithBeforeEachRan?.runAfterEach(null)
    }

    /**
     * Computes the path of dyna nodes from the root group towards this one.
     */
    private fun getPathFromRoot(): List<DynaNodeTestDescriptor> =
        generateSequence(this, { it -> it.parent.orElse(null) as? DynaNodeTestDescriptor }).toList().reversed()

    /**
     * Runs all `afterEach` blocks recursively, from this node all the way up to the root node. Properly propagates exceptions.
     */
    private fun runAfterEach(testFailure: Throwable?) {
        var tf = testFailure
        if (node is DynaNodeGroup) {
            node.afterEach.forEach {
                try {
                    it()
                } catch (t: Throwable) {
                    if (tf == null) tf = t else tf!!.addSuppressed(t)
                }
            }
        }
        (parent.orElse(null) as? DynaNodeTestDescriptor)?.runAfterEach(tf)
        if (testFailure == null && tf != null) throw tf!!
    }
}

val Class<*>.isPrivate: Boolean get() = Modifier.isPrivate(modifiers)
val Class<*>.isAbstract: Boolean get() = Modifier.isAbstract(modifiers)
val Class<*>.isPublic: Boolean get() = Modifier.isPublic(modifiers)

/**
 * Computes the pointer to the source of the test and returns it. Tries to compute at least inaccurate pointer.
 * @return the pointer to the test source; returns null if the source can not be computed by any means.
 */
private fun StackTraceElement.toTestSource(): TestSource {
    val caller: StackTraceElement = this
    // normally we would just return ClassSource, but there are the following issues with that:
    // 1. Intellij ignores FilePosition in ClassSource; reported as https://youtrack.jetbrains.com/issue/IDEA-186581
    // 2. If I try to remedy that by passing in the block class name (such as DynaTestTest$1$1$1$1), Intellij looks confused and won't perform any navigation
    // 3. FileSource seems to work very well.

    // Try to guess the absolute test file name from the file class. It should be located somewhere in src/test/kotlin or src/test/java
    if (!caller.fileName.isNullOrBlank() && caller.fileName.endsWith(".kt") && caller.lineNumber > 0) {
        // workaround for https://youtrack.jetbrains.com/issue/IDEA-188466
        // the thing is that when using $MODULE_DIR$, IDEA will set CWD to, say, karibu-testing/.idea/modules/karibu-testing-v8
        // we need to revert that back to karibu-testing/karibu-testing-v8
        var moduleDir = File("").absoluteFile
        if (moduleDir.absolutePath.contains("/.idea/modules")) {
            moduleDir = File(moduleDir.absolutePath.replace("/.idea/modules", ""))
        }

        // discover the file
        val folders = listOf("java", "kotlin").map { File(moduleDir, "src/test/$it") } .filter { it.exists() }
        val pkg = caller.className.replace('.', '/').replaceAfterLast('/', "", "").trim('/')
        val file: File? = folders.map { File(it, "$pkg/${caller.fileName}") } .firstOrNull { it.exists() }
        if (file != null) return FileSource.from(file, caller.filePosition)
    }
    // ClassSource doesn't work on classes named DynaTestTest$1$1$1$1 (with $ in them); strip that.
    // Intellij ignores the file position: https://youtrack.jetbrains.com/issue/IDEA-186581
    return ClassSource.from(caller.className.replaceAfter('$', "").trim('$'), caller.filePosition)
}

private val StackTraceElement.filePosition: FilePosition? get() = if (lineNumber > 0) FilePosition.from(lineNumber) else null

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
