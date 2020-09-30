dependencies {
    api(kotlin("stdlib"))  // don't depend on stdlib-jdk8 to stay compatible with Android
    api(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-engine:${properties["junit_jupiter_version"]}")
}

kotlin {
    explicitApi()
}

val configureBintray = ext["configureBintray"] as (artifactId: String) -> Unit
configureBintray("dynatest-api")
