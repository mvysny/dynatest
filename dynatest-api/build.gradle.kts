dependencies {
    compile(kotlin("stdlib"))  // don't depend on stdlib-jdk8 to stay compatible with Android
    compile(kotlin("test"))
}

val configureBintray = ext["configureBintray"] as (artifactId: String) -> Unit
configureBintray("dynatest-api")
