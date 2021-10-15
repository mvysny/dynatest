dependencies {
    api(kotlin("stdlib-jdk8"))
    api(kotlin("test"))
}

kotlin {
    explicitApi()
}

val configureBintray = ext["configureBintray"] as (artifactId: String) -> Unit
configureBintray("dynatest-api")
