LIBRARY_ARTIFACT_ID = "micro-kotlin-args"
LIBRARY_VERSION = "HEAD-SNAPSHOT"  # Don't refactor this without altering tools/deploy.kts

# What language compliance levels are we configuring
JAVA_LANGUAGE_LEVEL = "1.8"
KOTLIN_LANGUAGE_LEVEL = "1.3"

# What version of kotlin are we using
KOTLIN_VERSION = "1.4.32"
KOTLINC_RELEASE_SHA = "dfef23bb86bd5f36166d4ec1267c8de53b3827c446d54e82322c6b6daad3594c"
KOTLINC_RELEASE_URL = "https://github.com/JetBrains/kotlin/releases/download/v{v}/kotlin-compiler-{v}.zip".format(v = KOTLIN_VERSION)

# what version of the kotlin rules are we using
KOTLIN_RULES_VERSION = "1.5.0-beta-3"
KOTLIN_RULES_FORK = "bazelbuild"
KOTLIN_RULES_SHA = "58edd86f0f3c5b959c54e656b8e7eb0b0becabd412465c37a2078693c2571f7f"
KOTLIN_RULES_URL = "https://github.com/{fork}/rules_kotlin/releases/download/v{version}/rules_kotlin_release.tgz".format(
    fork = KOTLIN_RULES_FORK,
    version = KOTLIN_RULES_VERSION,
)

MAVEN_REPOSITORY_RULES_VERSION = "2.0.0-alpha-3"
MAVEN_REPOSITORY_RULES_SHA = "853976a2e4908f010568aad8f47b1a1e87e258f33b114e6e92599dc2779938c4"

MAVEN_LIBRARY_VERSION = "3.6.3"

DIRECT_ARTIFACTS = {
    "com.github.ajalt:clikt:2.6.0": {"insecure": True},
    "com.google.truth:truth:1.0": {
        "insecure": True,
        "testonly": True,
        "exclude": ["com.google.auto.value:auto-value-annotations"],
    },
    "com.google.guava:guava:27.1-jre": {
        "insecure": True,
        "testonly": True,
        "exclude": ["com.google.guava:failureaccess", "com.google.guava:listenablefuture"],
    },
    "junit:junit:4.13": {"insecure": True, "testonly": True},
}

TRANSITIVE_ARTIFACTS = [
    "com.googlecode.java-diff-utils:diffutils:1.3.0",
    "org.jetbrains.kotlin:kotlin-stdlib:1.3.70",
    "com.google.j2objc:j2objc-annotations:1.1",
    "com.google.code.findbugs:jsr305:3.0.2",
    "org.checkerframework:checker-qual:2.5.2",
    "org.checkerframework:checker-compat-qual:2.5.5",
    "org.hamcrest:hamcrest-core:1.3",
    "com.google.errorprone:error_prone_annotations:2.3.1",
    "org.jetbrains.kotlin:kotlin-stdlib-common:1.3.70",
    "org.codehaus.mojo:animal-sniffer-annotations:1.17",
    "org.jetbrains:annotations:13.0",
]
def maven_artifacts():
    artifacts = {}
    artifacts.update(DIRECT_ARTIFACTS)
    for artifact in TRANSITIVE_ARTIFACTS:
        artifacts.update({artifact: {"insecure": True}})
    return artifacts
