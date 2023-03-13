#!/usr/bin/env kscript

@file:Include("../src/main/kotlin/ArgsParser.kt")

import java.lang.RuntimeException
import kotlin.system.exitProcess

class CLI(val args: ArgsParser) {
  val foo by args.flag("--foo", help = "It's a foo, whaddya want?!") { toInt() }

  val bar by args.opt("-b", help = "expecting lots of that B.")
}

val cli = CLI(ArgsParser(*args, binaryName = "basic.kts"))
try {
  if ("--help" in args) {
    println(cli.args.help())
    exitProcess(0)
  }
  println("FOO: ${cli.foo * 2}")
  println("BAR: ${cli.bar}")

} catch (e: RuntimeException) {
  println("${e.message}")
  println(cli.args.help())
  exitProcess(0)
}
