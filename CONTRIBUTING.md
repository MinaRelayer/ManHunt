# Contributing

Use Java 25 and Paper API 26.1.2. Run the complete verification suite before submitting changes:

```bash
./gradlew clean test jar
```

On Windows:

```powershell
.\gradlew.bat clean test jar
```

Keep lifecycle transitions explicit, do not schedule tasks after plugin shutdown, keep world paths inside Paper's world container, and add regression tests for permission, player lifecycle, and cleanup changes.

## Publishing a release

Releases are created from version tags. Keep `gradle.properties`, `CHANGELOG.md`, and the tag version synchronized, then run:

```bash
git tag v<version>
git push origin v<version>
```

The combined GitHub Actions workflow will verify the version, run `clean test jar`, and publish the generated JAR as a GitHub Release.
