dependencies {
    api(kotlin("stdlib-jdk8"))
    api(kotlin("test"))
    testRuntimeOnly(libs.junit.jupiter.engine)
}

kotlin {
    explicitApi()
}

val publishing = ext["publishing"] as (artifactId: String) -> Unit
publishing("dynatest-api")
