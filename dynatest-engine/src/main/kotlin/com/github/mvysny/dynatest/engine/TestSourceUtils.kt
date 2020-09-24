package com.github.mvysny.dynatest.engine

import com.github.mvysny.dynatest.InternalTestingClass
import org.junit.platform.commons.util.ReflectionUtils
import org.junit.platform.engine.TestSource
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.FilePosition
import org.junit.platform.engine.support.descriptor.FileSource
import org.junit.platform.engine.support.descriptor.MethodSource
import java.io.File
import java.lang.RuntimeException
import java.net.URL
import java.net.URLClassLoader

private val slash = File.separatorChar

/**
 * Computes the pointer to the source of the test and returns it. Tries to compute at least inaccurate pointer.
 * @return the pointer to the test source; returns null if the source can not be computed by any means.
 */
internal fun StackTraceElement.toTestSource(testName: String? = null): TestSource {
    val caller: StackTraceElement = this

    // Gradle is dumb to expect a hierarchy of ClassSources and MethodSources
    // Returning mixed FileSource will make Gradle freeze: https://github.com/gradle/gradle/issues/5737
    if (isRunningInsideGradle) {
        // return null  // WARNING THIS WILL MAKE GRADLE SKIP TESTS!!!!!
//        throw RuntimeException("Unsupported")   // THIS WILL MAKE GRADLE FREEZE!!! Retarded.
        // just returning ClassSource always will make gradle freeze. dpc

        // strip $ to avoid having Test$1.xml, Test$1$5.xml with tests scattered in them
        var bareClassName: String = caller.className
        val bareClassNameOmittingInnerclass: String = bareClassName.replaceAfter('$', "").trim('$')
        if (ReflectionUtils.tryToLoadClass(bareClassNameOmittingInnerclass).toOptional().isPresent) {
            bareClassName = bareClassNameOmittingInnerclass
            val bareClassNameOmittingKtSuffix: String = bareClassName.removeSuffix("Kt")
            if (ReflectionUtils.tryToLoadClass(bareClassNameOmittingKtSuffix).toOptional().isPresent) {
                bareClassName = bareClassNameOmittingKtSuffix
            }
        }
        if (testName != null) {
            return MethodSource.from(bareClassName, testName)
        }

        // BEWARE: if the bareClassName doesn't exist, then JUnit 5 will skip this group and
        // all nested tests silently!
        // it's better to fail properly than to risk tests not being run.
        ReflectionUtils.tryToLoadClass(bareClassNameOmittingInnerclass).getOrThrow { RuntimeException("Failed to load class", it) }
        return ClassSource.from(bareClassName, caller.filePosition)
    }

    // normally we would just return ClassSource, but there are the following issues with that:
    // 1. Intellij ignores FilePosition in ClassSource; reported as https://youtrack.jetbrains.com/issue/IDEA-186581
    // 2. If I try to remedy that by passing in the block class name (such as DynaTestTest$1$1$1$1), Intellij looks confused and won't perform any navigation
    // 3. FileSource seems to work very well.

    // Try to guess the absolute test file name from the file class. It should be located somewhere in src/test/kotlin or src/test/java
    if (!caller.fileName.isNullOrBlank() && caller.fileName.endsWith(".kt") && caller.lineNumber > 0) {
        // workaround for https://youtrack.jetbrains.com/issue/IDEA-188466
        // the thing is that when using $MODULE_DIR$, IDEA will set CWD to, say, karibu-testing/.idea/modules/karibu-testing-v8
        // we need to revert that back to karibu-testing/karibu-testing-v8
        var moduleDir: File = File("").absoluteFile
        if (moduleDir.absolutePath.contains("$slash.idea${slash}modules")) {
            moduleDir = File(moduleDir.absolutePath.replace("$slash.idea${slash}modules", ""))
        }

        // discover the file
        val folders: List<File> = listOf("java", "kotlin").map { File(moduleDir, "src/test/$it") }.filter { it.exists() }
        // don't use $slash here, since it's us who's producing those slashes
        val pkg: String = caller.className
                .replace('.', '/')
                .replaceAfterLast('/', "", "")
                .trim('/')
        var file: File? = folders.map { File(it, "$pkg/${caller.fileName}") }.firstOrNull { it.exists() }
        if (file == null) {
            // try another approach
            val clazz: Class<*>? = try {
                Class.forName(caller.className)
            } catch (e: ClassNotFoundException) {
                null
            }
            if (clazz != null && clazz != InternalTestingClass::class.java) {
                file = clazz.guessSourceFileName(caller.fileName)
            }
        }
        if (file != null) return FileSource.from(file, caller.filePosition)
    }

    // Intellij's ClassSource doesn't work on classes named DynaTestTest$1$1$1$1 (with $ in them); strip that.
    val bareClassName: String = caller.className.replaceAfter('$', "").trim('$')

    // We tried to resolve the test as FileSource, but we failed. Let's at least return the ClassSource.
    return ClassSource.from(bareClassName, caller.filePosition)
}

private val StackTraceElement.filePosition: FilePosition? get() = if (lineNumber > 0) FilePosition.from(lineNumber) else null

/**
 * Guesses source file for given class. For Intellij it is able to discover sources also in another module,
 * for Gradle it only discovers sources in this module.
 * @param fileNameFromStackTraceElement the class is known to be present in this file. May be different than `simpleClassName.java`
 * in case of Kotlin where classes may reside in random files.
 */
internal fun Class<*>.guessSourceFileName(fileNameFromStackTraceElement: String): File? {
    val resource = `package`.name.replace('.', '/') + "/" + simpleName + ".class"
    val classLoader: ClassLoader = Thread.currentThread().contextClassLoader

    // in case of Intellij, the url is something like
    // file:/home/mavi/work/my/dynatest/dynatest-engine/out/production/classes/com/github/mvysny/dynatest/InternalTestingClassKt.class
    val url: URL = classLoader.getResource(resource) ?: return null

    // We have a File that points to a .class file. We need to resolve that to the source .java/.kt file.
    // The most valuable part of the path is the absolute project path in which the file may be present.
    // Then, the class may be located in some folder named `build/something` or `out/production/classes`.
    // We need to remove that part and replace it with the path to the file, e.g. `src/main/kotlin`

    // try to replace "out/production/classes" or "build/classes/java/test" with "src/main/kotlin" (or such)
    val fullPathToClassName: String = url.toFile()?.absolutePath?.replace('\\', '/') ?: return null
    val buildFolderRegex: Regex = "(build/classes/(java|kotlin)/[^/]+/)|out/production/classes/".toRegex()
    if (fullPathToClassName.contains(buildFolderRegex)) {
        for (srcPath in listOf("src/main/java/", "src/main/kotlin/", "src/test/java/", "src/test/kotlin/")) {
            val replacement: String = fullPathToClassName.replace(buildFolderRegex, srcPath)
            assert(replacement != fullPathToClassName)
            val potentialSourceFile = File(File(replacement).absoluteFile.parentFile, fileNameFromStackTraceElement)
            if (potentialSourceFile.exists()) {
                return potentialSourceFile
            }
        }
    }
    if (classLoader !is URLClassLoader) {
        // JDK9+  - can't do the classpath scanning, just bail out
        return null
    }

    // JDK8: scan the classpath and find the path that matches the file.
    val classpath: List<URL> = classLoader.urLs.toList()

    // classpath entry which contains given class
    val classpathEntry: URL = classpath.firstOrNull { url.toString().startsWith(it.toString()) } ?: return null
    val classOutputDir: File = classpathEntry.toFile() ?: return null

    // step out of classOutputDir, but only 4 folders tops, so that we don't end up searching user's filesystem
    // Intellij outputs to out/production/classes
    // Gradle outputs to build/classes/java/test

    // Doesn't work with Gradle! It will include dependent modules as jars, e.g.
    // file:/home/mavi/work/my/dynatest/dynatest-api/build/libs/dynatest-api-0.11-SNAPSHOT.jar
    // With Gradle, this will only resolve sources in current project.
    var potentialModuleDir: File = classOutputDir
    repeat(5) {

        val fileName: String = `package`.name.replace('.', '/') + "/" + fileNameFromStackTraceElement
        val potentialFiles = listOf("src/main/java", "src/main/kotlin", "src/test/java", "src/test/kotlin").map {
            "$potentialModuleDir/$it/$fileName"
        }

        val resolvedFileName: String? = potentialFiles.firstOrNull { File(it).exists() }

        if (resolvedFileName != null) {
            return File(resolvedFileName)
        }

        potentialModuleDir = potentialModuleDir.parentFile
    }
    // don't fail - if the user has the sources elsewhere, just bail out and return null
//    throw RuntimeException("Looking for $this - it was found in $url which is $potentialModuleDir but I can't find it in any of the source roots!")

    return null
}
