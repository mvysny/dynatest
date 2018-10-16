package com.github.mvysny.dynatest.engine

import com.github.mvysny.dynatest.InternalTestingClass
import org.junit.platform.engine.TestSource
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.FilePosition
import org.junit.platform.engine.support.descriptor.FileSource
import java.io.File
import java.net.URLClassLoader

/**
 * Computes the pointer to the source of the test and returns it. Tries to compute at least inaccurate pointer.
 * @return the pointer to the test source; returns null if the source can not be computed by any means.
 */
internal fun StackTraceElement.toTestSource(): TestSource? {
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

        // try another approach
        val clazz = try { Class.forName(caller.className) } catch (e: ClassNotFoundException) { null }
        if (clazz != null && clazz != InternalTestingClass::class.java) {
            val file = clazz.guessSourceFileName(caller.fileName)
            if (file != null) return FileSource.from(file, caller.filePosition)
        }
    }

    // Intellij's ClassSource doesn't work on classes named DynaTestTest$1$1$1$1 (with $ in them); strip that.
    val bareClassName = caller.className.replaceAfter('$', "").trim('$')

    // Intellij ignores the file position: https://youtrack.jetbrains.com/issue/IDEA-186581 in ClassSource.
    // We tried to resolve the test as FileSource, but we failed. Let's at least return the ClassSource.

    // Returning mixed FileSource will make Gradle freeze: https://github.com/gradle/gradle/issues/5737
    if (isRunningInsideGradle) {
        // return null  // WARNING THIS WILL MAKE GRADLE SKIP TESTS!!!!!
        throw RuntimeException("Unsupported")
    }

    return ClassSource.from(bareClassName, caller.filePosition)
}

private val StackTraceElement.filePosition: FilePosition? get() = if (lineNumber > 0) FilePosition.from(lineNumber) else null

internal fun Class<*>.guessSourceFileName(fileNameFromStackTraceElement: String): File? {
    val resource = `package`.name.replace('.', '/') + "/" + simpleName + ".class"
    val url = Thread.currentThread().contextClassLoader.getResource(resource) ?: return null

    // We have a File that points to a .class file. We need to resolve that to the source .java file.
    // The most valuable part of the path is the absolute project path in which the file may be present.
    // Then, the class may be located in some folder named `build/something` or `out/production/classes`.
    // We need to remove that part and replace it with the path to the file.

    val classpath = (Thread.currentThread().contextClassLoader as URLClassLoader).urLs.toList()

    // classpath entry which contains given class
    val classpathEntry = classpath.firstOrNull { url.toString().startsWith(it.toString()) } ?: return null
    val classOutputDir = classpathEntry.toFile() ?: return null

    // step out of classOutputDir, but only 3 folders tops, so that we don't end up searching user's filesystem
    var potentialModuleDir = classOutputDir
    for (i in 0..3) {

        val fileName = `package`.name.replace('.', '/') + "/" + fileNameFromStackTraceElement
        val potentialFiles = listOf("src/main/java", "src/main/kotlin", "src/test/java", "src/test/kotlin").map {
            "${potentialModuleDir}/$it/$fileName"
        }

        val resolvedFileName = potentialFiles.firstOrNull { File(it).exists() }
        if (resolvedFileName != null) {
            return File(resolvedFileName)
        }

        potentialModuleDir = potentialModuleDir.parentFile
    }
    // don't fail - if the user has the sources elsewhere, just bail out and return null
//    throw RuntimeException("Looking for $this - it was found in $url which is $potentialModuleDir but I can't find it in any of the source roots!")

    return null
}
