dependencies {
    api(kotlin("stdlib-jdk8"))
    api(kotlin("test"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${properties["junit_jupiter_version"]}")
}

kotlin {
    explicitApi()
}

val publishing = ext["publishing"] as (artifactId: String) -> Unit
publishing("dynatest-api")
