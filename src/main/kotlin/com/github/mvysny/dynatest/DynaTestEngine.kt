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
 * * do not support beforeAll/afterAll: https://github.com/junit-team/junit5/issues/1292
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
                        val test = it.newInstance() as DynaTest
                        val td = test.toTestDescriptor(result.uniqueId)
                        result.addChild(td)
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
                } else if (td is InitFailedTestDescriptor) {
                    throw RuntimeException(td.failure)
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

/**
 * A container which hosts all DynaTest test classes.
 */
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

internal class DynaNodeTestDescriptor(parentId: UniqueId, val node: DynaNode) : AbstractTestDescriptor(node.getId(parentId), node.name, node.src?.toTestSource()) {
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

internal class InitFailedTestDescriptor(parentId: UniqueId, val clazz: Class<*>, val failure: Throwable) :
    AbstractTestDescriptor(parentId.append("class", clazz.simpleName), clazz.simpleName, ClassSource.from(clazz)) {

    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.TEST
}
