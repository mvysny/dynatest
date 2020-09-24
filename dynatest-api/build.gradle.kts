dependencies {
    api(kotlin("stdlib"))  // don't depend on stdlib-jdk8 to stay compatible with Android
    api(kotlin("test"))
}

kotlin {
    explicitApi()
}

val configureBintray = ext["configureBintray"] as (artifactId: String) -> Unit
configureBintray("dynatest-api")
