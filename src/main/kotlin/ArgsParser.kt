import java.lang.IllegalArgumentException

// Copyright (c) Christian Gruber, All Rights Reserved
// Licensed under BSD 2-term simplified, or Apache 2.0 licenses

/**
 * An arguments parser, which supplies options and flags. Options are of the form `-f` or `--foo` and are followed by
 * a value, or flags, which are of the same form, but do not contain any extra value - they merely flip a boolean to
 * true if present.  This is intended to be an extremely thin, minimalist, code-golf-small args parser which can be
 * conveniently copied into kotlin scripts, imported into kscripts, or pulled in as a maven dependency. It will never
 * be fully featured, so as to accomdiate this minimalist goal, but it has a fair number of features.
 *
 * One ArgsParser instance should be created for every options-class, as the parser is stateful, and responds to the
 * property delegation operaitons that happen on options-class construction. The "args" array can of course be reused,
 * but ArgsParser instances should not be.
 *
 * Usage:
 * ```
 *   data class MyOpts(var opts: ArgsParser) {
 *     var blah: String? by opts.opt("-b", "--blah")
 *     var foo: MyType? by opts.opt("-f", "--foo") { convertStringToMyType(it) }
 *     var qiz: String by opts.opt("-q", "--qiz").default { "whatevz" }
 *     var one: Boolean by opts.flag("-o, "--one")
 *   }
 *
 *   val opts = MyOpts(ArgsParser(args))
 * ```
 */
class ArgsParser(vararg args: String, val name: String = "binary", val exit: Boolean = false) {
  internal val args = args.toList()
  var opts = mutableMapOf<String, Opt<*>>()
  private fun putOpt(o: Opt<*>) = o.names.forEach {
    if (opts[it] == null) opts[it] = o else throw IllegalArgumentException("Multiple definitions of $it not supported.")
  }
  fun opt(vararg names: String, help: String? = null, env: String? = null): Opt<String> {
    return Opt(this, *names, help = help, env = env) { a -> a }.also(::putOpt)
  }
  fun flag(vararg names: String, help: String? = null, env: String? = null): kotlin.properties.ReadOnlyProperty<Any, Boolean> {
    return object : Opt<Boolean>(this, *names, help = help, env = env, xform = { false }) {
      override fun findValue(index: Int): Boolean = index >= 0
    }.also(::putOpt).default { false }
  }
  fun <T : Any> opt(
    vararg names: String,
    help: String? = null,
    env: String? = null,
    xform: (String) -> T
  ): Opt<T> {
    return Opt(this, *names, help = help, env = env, xform = xform).also(::putOpt)
  }
  fun help() = "Usage '$name <options and flags> ...'\n" + opts.values.toSet().joinToString("\n") { o ->
    val optional = if (o.allowMissingFlag) " (optional)" else ""
    "  ${o.names.joinToString()}${optional}${o.help?.let {"\n    $it"} ?: ""}"
  }
  open class Opt<T : Any>(
    private val parser: ArgsParser,
    vararg val names: String,
    val help: String?,
    val env: String?,
    val xform: ((value: String) -> T)
  ) : kotlin.properties.ReadOnlyProperty<Any?, T?> {
    var allowMissingFlag: Boolean = false // so janky, but terse.
    private val value: T? by lazy {
      val index = parser.args.indexOfFirst { it in names }
      when {
        index >= 0 -> findValue(index) // found the flag
        env != null && System.getenv(env) != null -> xform.invoke(System.getenv(env))
        allowMissingFlag -> findValue(index)
        parser.exit -> {
          println("Could not find flag ${names.toList()} in args.")
          println(parser.help())
          kotlin.system.exitProcess(1)
        }
        else -> throw kotlin.IllegalStateException("Could not find flag ${names.toList()} in args.")
      }
    }
    protected open fun findValue(index: Int) = if (index >= 0) {
      check(parser.args.size > index + 1) { "Option ${parser.args[index]} has no argument" }
      check(parser.args[index + 1] !in parser.opts.keys) {
        "Option ${parser.args[index]} expects a value, but found opt '${parser.args[index + 1]}'"
      }
      xform.invoke(parser.args[index + 1])
    } else null
    override fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>) = value
    fun default(def: () -> T): kotlin.properties.ReadOnlyProperty<Any, T> =
      object : kotlin.properties.ReadOnlyProperty<Any, T> {
        init { this@Opt.allowMissingFlag = true }
        override fun getValue(thisRef: Any, property: kotlin.reflect.KProperty<*>) =
          this@Opt.getValue(thisRef, property) ?: def.invoke()
      }
  }
}

/**
 * A function which validates that all non-optional flags/opts have been supplied, and that no unknown flags/opts have
 * been supplied. This will fail if the ArgsParser has not yet been used to configure an options class. The validation
 * logic will also only be accurate if the ArgsParser has been already used to configure all options classes that it
 * will ever be used for. If the ArgsParser is used for only a subset of args, then up-front validation is not possible,
 * and this API should not be called.
 *
 * It is built separately so as to make it easier to cut-and-paste the basic ArgsParser into scripts, if desired.
 */
fun ArgsParser.validate() {
  if (this.opts.isEmpty()) throw IllegalStateException("ArgsParser has not been used yet, but attempted to validate.")
  // Check for unknown flags
  val argsNames = args.filter { it.startsWith("-") }.toSet()
  val unknown = argsNames.filter { !opts.containsKey(it) }
  if (unknown.isNotEmpty()) throw IllegalArgumentException("Unknown arguments: ${unknown.joinToString()}")
  val missing = opts.values.toSet().filter{ !it.allowMissingFlag }.filter { it.names.intersect(argsNames).isEmpty() }
  if (missing.isNotEmpty()) throw IllegalArgumentException("Missing arguments: ${missing.joinToString { it.names[0] }}")
}
