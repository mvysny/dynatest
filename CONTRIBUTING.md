# Developing

Please feel free to open bug reports to discuss new features; PRs are welcome as well :)

All utility methods should go into the [dynatest](dynatest) project. Only contribute
to [dynatest-api](dynatest-api) and [dynatest-engine](dynatest-engine) if there is
something wrong with the engine itself. The goal is to keep the `-api` and `-engine`
projects as small as possible, placing all utility methods into the `dynatest` project.

# Releasing

To release the library to Maven Central:

1. Edit `build.gradle.kts` and remove `-SNAPSHOT` in the `version=` stanza
2. Commit with the commit message of simply being the version being released, e.g. "0.20"
3. git tag the commit with the same tag name as the commit message above, e.g. `0.20`
4. `git push`, `git push --tags`
5. Run `./gradlew clean build publish`
6. Continue to the [OSSRH Nexus](https://oss.sonatype.org/#stagingRepositories) and follow the [release procedure](https://central.sonatype.org/pages/releasing-the-deployment.html).
7. Add the `-SNAPSHOT` back to the `version=` while increasing the version to something which will be released in the future,
   e.g. 0.21-SNAPSHOT, then commit with the commit message "0.21-SNAPSHOT" and push.
