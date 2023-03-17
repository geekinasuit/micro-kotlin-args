workspace(name = "micro-kotlin-args")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
load(
    "//:versions.bzl",
    "KOTLINC_RELEASE_SHA",
    "KOTLIN_VERSION",
    "KOTLIN_RULES_SHA",
    "KOTLIN_RULES_URL",
    "MAVEN_REPOSITORY_RULES_SHA",
    "MAVEN_REPOSITORY_RULES_VERSION",
    "maven_artifacts",
)

# Load the kotlin rules repository, and setup kotlin rules and toolchain.
http_archive(
    name = "io_bazel_rules_kotlin",
    sha256 = KOTLIN_RULES_SHA,
    urls = [KOTLIN_RULES_URL],
)

load("@io_bazel_rules_kotlin//kotlin:repositories.bzl", "kotlin_repositories", "kotlinc_version")

kotlin_repositories(compiler_release = kotlinc_version(
    release = KOTLIN_VERSION,
    sha256= KOTLINC_RELEASE_SHA,
))

register_toolchains("//:kotlin_toolchain")

http_archive(
    name = "maven_repository_rules",
    sha256 = MAVEN_REPOSITORY_RULES_SHA,
    strip_prefix = "bazel_maven_repository-%s" % MAVEN_REPOSITORY_RULES_VERSION,
    urls = ["https://github.com/square/bazel_maven_repository/archive/%s.zip" % MAVEN_REPOSITORY_RULES_VERSION],
)

load("@maven_repository_rules//maven:maven.bzl", "maven_repository_specification")

maven_repository_specification(
    name = "maven",
    artifacts = maven_artifacts(),
    repository_urls = {"central": "https://repo1.maven.org/maven2"},
)
