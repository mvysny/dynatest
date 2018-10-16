import com.jfrog.bintray.gradle.BintrayExtension
import groovy.lang.Closure
import org.gradle.api.tasks.GradleBuild
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

dependencies {
    compile(project(":dynatest-api"))
    compile("org.junit.jupiter:junit-jupiter-api:5.3.0")
    compile("org.junit.platform:junit-platform-engine:1.3.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.0")
}

val configureBintray = ext["configureBintray"] as (artifactId: String) -> Unit
configureBintray("dynatest")
