# micro-kotlin-args
The thinnest possible, yet usable, flags/options parser for Kotlin

> code-golf as a service, by Christian Gruber

***Currently running at:** under 90 lines of code (excluding license and comment lines)*

This library/snippet seeks to be the smallest possible args/flags/options parsing library 
for Kotlin, dependency free, and suitable for inclusion in kotlin apps or kotlin `.kts`
scripts. 

This will have known defects, becasue of it's enforced terseness, notably that it will have
only a limited (but surprisingly flexible) set of features, will not be as safe, or easy to
debug as other libraries.

Some basic features:
   - parsing `--foo value` style options
   - parsing `--myflag` style single flags
   - mapping multiple values (e.g. enums) to flags
   - transformation lambdas for altering flag values and types
   - configuring default values for flags. 
   - optional (nullable) flags
   - configuring opts/flags to be configured from an env var by default
   - super-basic help text support (and `Opt` objects exposed to allow more robust help text)
   - Multiple options classes using a shared parser (with caveats - they must all
     be instantiated prior to using `help()` or `validate()`)
   - `parser.validate()` to force early evaluation of the args (defaults to lazy evaluation)
     - Also reports unknown/undeclared flags

Some (current) limitations (compared to other flag parsers) include:
   - No support for commands
   - positional parameters
   - No negative flags (i.e. --no-foo to invert the value of foo)
   - No lists of paramters to options (`"-files a b c -someFlag"`)
   - No accumulative flags or options (`"-v -v -v"` turns into verbosity=3, for instance)

Other caveats
   - Evaluation is semi-lazy, so using the same parser in different options code has some
     subtleties. (Can instantiate all options classes and then call validate())

These may never be added or "fixed", as it may not be feasible to impelement them and keep
under or near the ideal goal of 50 lines of code. Contributions (within these constraints)
welcome. Especially contributions which shorten the code without sacrificing functionality.

**micro-kotlin-args** definitely primarily a tool for scripting. The constraint on size is
so that it can be cut-and-pasted into scripts, even those that only use `kotlinc -script`
and don't have extra fancy dependency management systems like kscript. That said, it's
similar enough to xenomachina or kotlin-cli that it should be familiar to use even in 
non-scripting contexts, and will be released as a maven artifact as well for this purpose.

# Usage

In short, you create an `OptionsParser` with the string args given by `main()`, and pass it in
to the class where args will be consumed. In that object, properties should use property delegation
and delegate to functions on the parser, which will fill the properties from the parsed arguments.

All declared options are required unless they have a `.default { "some value" }` clause. Flags
are not required and will be false if they are not supplied.

> Note: Negative flags are not supported.

Opt and flag values, when not explicitly set in the args, may be satisfied with a configured
default, or by an environment variable via the `env=` parameter (see example below)

For a simple example:
```kotlin
class Config(opts: ArgsParser) {
  val foo by opts.opt("--foo", "-f", help="A simple option with a paramter")
  val bar by opts.opt("--bar", "-b", help="An opt with a default value").default { "defaultBar" }
  val baz: Int by opts.opt("--baz", help="An opt's string converted to an Int") { it.toInt() }
  val bin by opts.opt("--bin", env = "TEST_BINARY")
  val flag by opts.flag("--flag")
}

fun main(vararg args: String) {
  val config = Config(ArgsParser(args))
  // use config.foo, config.bar, config.baz, etc.
}
```

# Including it

## Scripting

### Cut and paste

Just go to the source page, and cut and paste it in. 

### Scripting

Get the raw url of the source file, and use in kscript (or another similar system) and put this
in your kotlin script:
```kotlin
@file:Import("https://raw.githubusercontent.com/geekinasuit/micro-kotlin-args/6bbead09df583a61cf98785967093c97b38f40f2/src/main/kotlin/ArgsParser.kt")
```
Ideally you should not use the main branch at head, but lock in a particular commit.

## Maven Artifact

Using kscript or some other scripting engine that supports DependsOn annotations:
```kotlin
@file:DependsOn("com.geekinasuit.micro:micro-kotlin-args:<version>")
```
## License

Copyright (c) 2021, Christian Gruber, All Rights Reserved.

This code is multi-licensed, which license chosen at the user's discretion as:
    - Apache 2.0
    - BSD 2-clause simplified
    - MIT license
    - WTFPL if you aren't at liability risk using it

I would make it Public Domain but US and other legal regimes have real problems around
Public Domain and I don't want someone re-asserting property rights over this code, when
I wish it to be free for anyone to use, hence my reservation of rights in the copyright
line. I wish I didn't have to do that. I hereby also waive any notice clauses in the above
licenses, and so credit in consuming software is not required to be given to the author.
