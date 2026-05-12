## Cursor Cloud specific instructions

FlixelGDX is a Java 17 game framework library (not a standalone application). There is no runnable app in this repo; it is tested via unit tests and by consuming the published artifacts from a separate project.

### Key commands

All commands are run from the repository root using the Gradle wrapper (`./gradlew`).

| Task | Command |
|------|---------|
| Compile all modules | `./gradlew classes` |
| Run unit tests | `./gradlew :flixelgdx-test:test` |
| Publish to Maven Local | `./gradlew publishToMavenLocal` |
| Generate Javadocs | `./gradlew javadocAll` |

See `COMPILING.md` for full build/test documentation and `CONTRIBUTING.md` for coding standards and PR workflow.

### Environment notes

- The VM has JDK 21 installed. Gradle 9.x runs on JDK 17+, and the Gradle toolchain (foojay-resolver) auto-downloads JDK 17 for compilation. No manual JDK 17 install is needed.
- The Gradle daemon is disabled by default (`org.gradle.daemon=false` in `gradle.properties`) to save memory.
- Default logging level is `quiet`; use `--info` for verbose output during debugging.
- The Android module is excluded by default. Pass `-PincludeAndroid=true` or add `includeAndroid=true` to `local.properties` if you need it.
- Tests use the libGDX headless backend and do not require a display or GPU.
- PRs should target the `develop` branch per `CONTRIBUTING.md`.
