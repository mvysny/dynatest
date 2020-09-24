package com.github.mvysny.dynatest

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.test.expect
import kotlin.test.fail


/**
 * Expects that this file or directory exists on the file system.
 */
public fun File.expectExists() {
    expect(true, "file $absoluteFile does not exist") { exists() }
}

/**
 * Expects that this file or directory [expectExists] and is a directory.
 */
public fun File.expectDirectory() {
    expectExists()
    expect(true, "file $absoluteFile is not a directory") { isDirectory }
}

/**
 * Expects that this file or directory [expectExists] and is a file.
 */
public fun File.expectFile() {
    expectExists()
    expect(true, "file $absoluteFile is not a file") { isFile }
}

/**
 * Expects that this file or directory is a file [expectFile] and is readable ([File.canRead]).
 */
public fun File.expectReadableFile() {
    expectFile()
    expect(true, "file $absoluteFile is not readable") { canRead() }
}

/**
 * Expects that this file or directory is a file [expectFile] and is readable ([File.canRead]).
 */
public fun File.expectWritableFile() {
    expectFile()
    expect(true, "file $absoluteFile is not readable") { canWrite() }
}

/**
 * Internal.
 */
public class TempFolderProvider(
        private val node: DynaNodeGroup,
        private val prefix: String?,
        private val suffix: String?,
        private val keepOnFailure: Boolean
) {
    public operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadWriteProperty<Any?, File> {
        val property = LateinitProperty<File>(prop.name)
        var dir: File by property
        node.beforeEach {
            dir = createTempDir("tmp-${prefix ?: prop.name}", suffix)
        }
        node.afterEach { outcome ->
            if (!keepOnFailure || outcome.isSuccess) {
                dir.deleteRecursively()
            } else {
                println("Test ${outcome.testName} failed, keeping temporary dir $property")
            }
        }
        return property
    }
}

/**
 * Configures current [DynaNodeGroup] to create a temporary folder before every test is run, then delete it afterwards.
 *
 * Usage:
 * ```
 * group("source generator tests") {
 *   val sources: File by withTempDir()
 *   test("simple") {
 *     generateSourcesTo(sources)
 *     val generatedFiles: List<File> = sources.expectFiles("*.java", 10..10)
 *     // ...
 *   }
 *   test("more complex test") {
 *     // 'sources' will point to a new temporary directory now.
 *     generateSourcesTo(sources)
 *     // ...
 *   }
 * }
 * ```
 * @param prefix an optional temporary directory prefix as passed into [createTempDir].
 * Defaults to null which will use the variable name.
 */
public fun DynaNodeGroup.withTempDir(prefix: String? = null, suffix: String? = null, keepOnFailure: Boolean = true): TempFolderProvider =
        TempFolderProvider(this, prefix, suffix, keepOnFailure)

/**
 * Finds all files matching given [glob] pattern in this directory.
 * Always pass in forward slashes as path separators, even on Windows.
 * @param glob the files to find, e.g. `libs/ *.war` (only in particular folder) or `** / *.java` or `**.java` (anywhere).
 * @param expectedCount expected number of files, defaults to 1.
 */
public fun File.expectFiles(glob: String, expectedCount: IntRange = 1..1): List<File> {
    var glob = glob
    if (glob.startsWith("**/")) {
        // common mistake: wouldn't match files in root folder.
        glob = "**" + glob.substring(3)
    }
    expectDirectory()
    val pattern: String = if (OsUtils.isWindows) {
        // replace \ with \\ to avoid collapsing; replace forward slashes in glob with \\
        "glob:$absolutePath".replace("""\""", """\\""") + """\\""" + glob.replace("/", """\\""")
    } else {
        "glob:$absolutePath/$glob"
    }
    val matcher: PathMatcher = FileSystems.getDefault().getPathMatcher(pattern)
    val found: List<File> = absoluteFile.walk()
            .filter { matcher.matches(it.toPath()) }
            .toList()
    if (found.size !in expectedCount) {
        fail("Expected $expectedCount $glob but found ${found.size}: $found . Folder dump: ${absoluteFile.walk().joinToString("\n")}")
    }
    return found
}

/**
 * Operating system-related utilities.
 */
internal object OsUtils {
    val osName: String = System.getProperty("os.name")
    /**
     * True if we're running on Windows, false on Linux, Mac and others.
     */
    val isWindows: Boolean get() = osName.startsWith("Windows")
}
