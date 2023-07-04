#!/usr/bin/env kscript

/*
 * Copyright 2020 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Include("../src/main/kotlin/ArgsParser.kt")

import java.io.File
import java.io.IOException
import java.lang.ProcessBuilder.Redirect.INHERIT
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.system.exitProcess

class Main(vararg args: String) {
    private val parser = ArgsParser(args = args, "deploy", exit = true)
    private val help by parser.flag("-h", "--help", hidden = true)
    private val key by parser.opt("-k", "--key", help = "GPG Key for deployment to sonatype").optional()
    private val isCI by parser.flag(env = "GITHUB_ACTIONS", help = "Operate with CI behavior")
    private val branch by parser.opt(env = "GITHUB_BASE_REF", help = "Branch name override")
        .default { "git branch --show-current".execute().stdout }

    private val verbose by parser.flag("-v", "--verbose")
    private val username by parser.opt("--username", env = "CI_DEPLOY_USERNAME").optional()
    private val password by parser.opt("--password", env = "CI_DEPLOY_PASSWORD").optional()

    private val bazel: String by lazy { "bazel".which() }

    private val version = File("versions.bzl").extractPythonicStringVariable("LIBRARY_VERSION").also {
        if (it.isEmpty()) {
            System.err.println("Could not extract version from version file.")
            exitProcess(1)
        }
    }
    private val snapshotVersion = version.endsWith("-SNAPSHOT")

    private val pom_file: String by lazy { "$bazel build //:argslib_pom".execAndFilterSuffix(".pom") }
    private val artifact_file: String by lazy { "$bazel build //:argslib_deployment_jar".execAndFilterSuffix(".jar") }
    private val sources_file: String by lazy { "$bazel build //:argslib_sources_jar".execAndFilterSuffix("-sources.jar") }
    private val javadoc_file: String by lazy { "placeholder-javadoc.jar" }

    fun run() {
        if (help) help(0)
        if (!File("WORKSPACE").exists()) {
            exit(1) { "Must run deployment script from the workspace root." }
        }
        val repo = if (isCI) {
            if (branch == "main") Repo.SonatypeSnapshots
            else exit(1) {"Aborting deployment on a non-main branch." }
        } else if (branch.startsWith("release-")) {
            if (key.isNullOrBlank()) help(1) { "Must supply --key <gpgkey> for release deployments." }
            else if (snapshotVersion) help(1) { "Don't use a snapshot version ($version) on a release branch ($branch)" }
            else Repo.SonatypeStaging
        } else Repo.FakeLocalRepo

        val mvn_goal = key?.let { "gpg:sign-and-deploy-file" } ?: "deploy:deploy-file"
        val key_flag = key?.let { " -Dgpg.keyname=$it" } ?: ""
        val settings_file = if (repo != Repo.FakeLocalRepo) " -gs settings.xml" else ""
        val debug_flag = if (verbose) " --debug -X" else ""
        val javadoc_flag = if (!snapshotVersion) " -Djavadoc=$javadoc_file" else ""
        val mvn_cmd = "mvn $mvn_goal" +
                settings_file +
                debug_flag +
                " -Dmaven.resolver.transport=wagon" +
                " -Dfile=$artifact_file" +
                " -DpomFile=$pom_file" +
                " -Dsources=$sources_file" +
                " -DrepositoryId=${repo.id}" +
                " -Durl=${repo.url}" +
                javadoc_flag +
                key_flag

        println("Deploying version $version to $repo")
        if (verbose) println("Executing command: $mvn_cmd")
        mvn_cmd.cmd(outputRedirect = INHERIT, errorRedirect = INHERIT)
            .apply {
                with(environment()) {
                    if (repo != Repo.FakeLocalRepo) {
                        if (username.isNullOrBlank() || password.isNullOrBlank()) {
                            help(1) {
                                "Must supply either CI_DEPLOY_USERNAME/CI_DEPLOY_PASSWORD " +
                                        "environment vairables, or override --username/--password to deploy to a " +
                                        "non-fake repo."
                            }
                        } else {
                            putIfAbsent("CI_DEPLOY_USERNAME", username)
                            putIfAbsent("CI_DEPLOY_PASSWORD", password)
                        }
                    }
                }
                println("Executing: ${command().joinToString(" ")}")
            }
            .execute(timeout = 300)
    }

    private fun help(code: Int, message: (() -> String)? = null): Nothing {
        message?.let { println(it()) }
        println(parser.help())
        exitProcess(code)
    }
}

fun exit(code: Int, message: () -> String): Nothing {
    System.err.println(message())
    exitProcess(code)
}

sealed class Repo(val id: String, val url: String) {
    object SonatypeSnapshots : Repo(
        "sonatype-nexus-snapshots",
        "https://s01.oss.sonatype.org/content/repositories/snapshots"
    )
    object SonatypeStaging : Repo(
        "sonatype-nexus-staging",
        "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2"
    )
    object FakeLocalRepo : Repo(
        "local-fake",
        "file:///tmp/fakerepo"
    )

    override fun toString(): String = "Repo[id=$id, url=$url]"
}

fun String.execAndFilterSuffix(suffix: String) =
    cmd()
        .execute()
        .let { proc ->
            if (proc.isAlive) throw TimeoutException("Should not still be running.")
            if (proc.exitValue() != 0) {
                val error = """Error executing command.
                Stdout: ${proc.stdout}
                Stderr: ${proc.stderr}
                """.trimIndent()
                throw IllegalStateException(error)
            }
            proc.stdout + proc.stderr
        }
        .lines()
        .firstOrNull() { it.endsWith(suffix) }
        ?.trim() ?: exit(1) { "No $suffix file found in output of '$this'" }

/** A utility method to extact a known version */
fun File.extractPythonicStringVariable(variable: String) =
    readText()
        .lines()
        .first { it.startsWith(variable) }
        .substringBefore("#") // ditch comments
        .substringAfter("=")
        .trim('"', ' ')

fun Process.wait(timeout: Long = 120, unit: TimeUnit = TimeUnit.SECONDS): Process =
    this.also { it.waitFor(timeout, unit) }

val Process.stderr: String get() = errorStream.bufferedReader().readText()

val Process.stdout: String get() = inputStream.bufferedReader().readText()

/** Short-cut which creates the command and executes it directly */
fun String.execute(
    timeout: Long = 120,
    unit: TimeUnit = TimeUnit.SECONDS,
    onTimeout: (Process) -> Unit = {},
    onError: (Process) -> Unit = {}
) = cmd().execute(timeout, unit, onTimeout, onError)

fun String.cmd(
    workingDir: File = File("."),
    outputRedirect: ProcessBuilder.Redirect = ProcessBuilder.Redirect.PIPE,
    errorRedirect: ProcessBuilder.Redirect = ProcessBuilder.Redirect.PIPE
): ProcessBuilder {
    val parts = this.split("\\s".toRegex())
    return ProcessBuilder(*parts.toTypedArray())
        .directory(workingDir)
        .redirectOutput(outputRedirect)
        .redirectError(errorRedirect)
}

fun ProcessBuilder.execute(
    timeout: Long = 120,
    unit: TimeUnit = TimeUnit.SECONDS,
    onTimeout: (Process) -> Unit = {},
    onError: (Process) -> Unit = {}
): Process = start().wait(timeout, unit).apply {
    if (isAlive) onTimeout(this)
    else when (exitValue()) {
        0 -> {}
        else -> onError(this)
    }
}

/**
 * Wraps the unix `which` comamnd, returning the first path entry for the given command, or null.
 */
fun String.which() = "which $this"
    .execute()
    .stdout
    .trim()
    .also {
        if (it.isEmpty()) throw IOException("Could not locate $this")
    }

Main(*args).run()
