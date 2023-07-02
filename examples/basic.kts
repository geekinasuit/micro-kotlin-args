#!/usr/bin/env kotlinc -script --

// A usage where the whole ArgsParser is copypasta'ed into the code. Suitable for situations
// where you can't include/concatenate scripts to include the parser from a separate file (or
// use maven-style deps)

import java.lang.RuntimeException
import kotlin.system.exitProcess

class CLI(val args: ArgsParser) {
  val foo by args.flag("--foo", help = "It's a foo, whaddya want?!")
  val bar by args.opt("-b", help = "expecting lots of that B.")
}

class ArgsParser(vararg args: String, val binaryName: String = "binary") {
  private val args = args.toList()
  var opts = mutableListOf<Opt<*>>()
  fun opt(vararg names: String, help: String? = null, env: String? = null): Opt<String> {
    return Opt(this, *names, help = help, env = env) { a -> a }.also { opts.add(it) }
  }
  fun flag(vararg names: String, help: String? = null, env: String? = null): Opt<Boolean> {
    return object : Opt<Boolean>(this, *names, help = help, env = env, xform = { false }) {
      init { this.allowMissingFlag = true }
      override fun findValue(index: Int): Boolean = index >= 0
    }.also { opts.add(it) }
  }
  fun <T : Any> opt(
    vararg names: String,
    help: String? = null,
    env: String? = null,
    xform: (String) -> T
  ): Opt<T> {
    return Opt(this, *names, help = help, env = env, xform = xform).also { opts.add(it) }
  }
  fun help() = "Usage '$binaryName <options and flags> ...'\n" + opts.joinToString("\n") {
    o ->"  ${o.names.joinToString()}${o.help?.let {"\n    ${o.help}"} ?: ""}"
  }
  open class Opt<T : Any>(
    val parser: ArgsParser,
    vararg val names: String,
    val help: String?,
    val env: String?,
    val xform: ((value: String) -> T)
  ) : kotlin.properties.ReadOnlyProperty<Any?, T?> {
    protected var allowMissingFlag: Boolean = false // so janky, but terse.
    private val value: T? by lazy {
      val index = parser.args.indexOfFirst { it in names }
      when {
        index >= 0 -> findValue(index) // found the flag
        env != null && System.getenv(env) != null -> xform.invoke(System.getenv(env))
        allowMissingFlag -> findValue(index)
        else -> throw kotlin.IllegalStateException("Could not find flag ${names.toList()} in args.")
      }
    }
    protected open fun findValue(index: Int) = if (index >= 0) {
      check(parser.args.size > index + 1) { "Option ${parser.args[index]} has no argument" }
      check(parser.args[index + 1] !in parser.opts.flatMap { it.names.toList() }) {
        "Option ${parser.args[index]} expects a value, but found opt '${parser.args[index + 1]}'"
      }
      xform.invoke(parser.args[index + 1])
    } else null
    override fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>) = value
    fun default(def: () -> T): kotlin.properties.ReadOnlyProperty<Any, T> =
      object : kotlin.properties.ReadOnlyProperty<Any, T> {
        init { this@Opt.allowMissingFlag = true }
        override fun getValue(r: Any, p: kotlin.reflect.KProperty<*>) =
          this@Opt.getValue(r, p) ?: def.invoke()
      }
  }
}

val cli = CLI(ArgsParser(*args, binaryName = "basic.kts"))
try {
  if ("--help" in args) {
    println(cli.args.help())
    exitProcess(0)
  }
  println("FOO: ${cli.foo}")
  println("BAR: ${cli.bar}")

} catch (e: RuntimeException) {
  println("${e.message}")
  println(cli.args.help())
  exitProcess(0)
}
