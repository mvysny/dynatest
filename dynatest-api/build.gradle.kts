dependencies {
    api(kotlin("stdlib-jdk8"))
    api(kotlin("test"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${properties["junit_jupiter_version"]}")
}

kotlin {
    explicitApi()
}

val configureBintray = ext["configureBintray"] as (artifactId: String) -> Unit
configureBintray("dynatest-api")
