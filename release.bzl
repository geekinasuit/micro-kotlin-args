POM_TEMPLATE = """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/maven-v4_0_0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.geekinsuit.micro</groupId>
  <artifactId>micro-kotlin-args</artifactId>
  <version>{version}</version>
  <name>Micro Kotlin Args</name>
  <description>Razor thin library/script for processing args/flags/options in Kotlin</description>
  <url>http://github.com/geekinasuit/micro-kotlin-args</url>
  <developers>
    <developer>
      <id>cgruber</id>
      <name>Christian Gruber</name>
      <email>christian@geekinasuit.com</email>
      <url>http://github.com/cgruber</url>
    </developer>
  </developers>
  <licenses>
    <license>
      <name>Apache 2.0</name>
      <url>https://spdx.org/licenses/Apache-2.0.html</url>
      <distribution>repo</distribution>
    </license>
    <license>
      <name>BSD 2-clause</name>
      <url>https://spdx.org/licenses/BSD-2-Clause.html</url>
      <distribution>repo</distribution>
    </license>
    <license>
      <name>MIT</name>
      <url>https://spdx.org/licenses/MIT.html</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <connection>scm:git:git://github.com:geekinasuit/{artifact_id}.git</connection>
    <developerConnection>scm:git:git@github.com:geekinasuit/{artifact_id}.git</developerConnection>
    <url>https://github.com/geekinasuit/{artifact_id}</url>
  </scm>
</project>
"""

def _generate_pom_rule_impl(ctx):
    filename = "%s-%s.pom" % (ctx.attr.artifact_id, ctx.attr.version)
    out = ctx.actions.declare_file(filename)
    ctx.actions.write(
        output = out,
        content = POM_TEMPLATE.format(
            artifact_id = ctx.attr.artifact_id,
            version = ctx.attr.version
        ),
    )
    return [DefaultInfo(files = depset([out]))]

def _generate_sources_jar_impl(ctx):
    filename = "%s-%s-sources.jar" % (ctx.attr.artifact_id, ctx.attr.version)
    out = ctx.actions.declare_file(filename)
    target = ctx.attr.target
    source_jar = target[JavaInfo].source_jars[0]
    ctx.actions.run_shell(
        inputs = [source_jar],
        outputs = [out],
        progress_message = "Preparing %s" % out.path,
        command = "cp %s %s" % (source_jar.path, out.path),
    )
    return [DefaultInfo(files = depset([out]))]

def _generate_deployment_jar_impl(ctx):
    filename = "%s-%s.jar" % (ctx.attr.artifact_id, ctx.attr.version)
    out = ctx.actions.declare_file(filename)
    target = ctx.attr.target
    deployment_jar = target[JavaInfo].compile_jars.to_list()[0]
    ctx.actions.run_shell(
        inputs = [deployment_jar],
        outputs = [out],
        progress_message = "Preparing %s" % out.path,
        command = "cp %s %s" % (deployment_jar.path, out.path),
    )
    return [DefaultInfo(files = depset([out]))]

maven_pom = rule(
    implementation = _generate_pom_rule_impl,
    attrs = {
        "artifact_id": attr.string(doc = "The maven artifactId", mandatory = True),
        "version": attr.string(doc = "The maven version", mandatory = True),
        "target": attr.label(
            doc = "The target library from which to generate metadata",
        ),
    },
)

sources_jar = rule(
    implementation = _generate_sources_jar_impl,
    attrs = {
        "artifact_id": attr.string(doc = "The maven artifactId", mandatory = True),
        "version": attr.string(doc = "The maven version", mandatory = True),
        "target": attr.label(
            doc = "The target library from which to generate metadata",
        ),
    },
)

deployment_jar = rule(
    implementation = _generate_deployment_jar_impl,
    attrs = {
        "artifact_id": attr.string(doc = "The maven artifactId", mandatory = True),
        "version": attr.string(doc = "The maven version", mandatory = True),
        "target": attr.label(
            doc = "The target library from which to generate metadata",
        ),
    },
)
