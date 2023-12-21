dependencies {
    api(project(":dynatest-engine"))
}

kotlin {
    explicitApi()
}

val publishing = ext["publishing"] as (artifactId: String) -> Unit
publishing("dynatest")
