// Copyright (c) 2021 by Christian Gruber, All Rights Reserved.
// Freely licensed under MIT, BSD 2-clause, Apache 2.0, or WTFPL
package args

import ArgsParser
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class ArgsParserTest {

  class Main(val opts: ArgsParser) {
    val foo by opts.opt("--foo", "-f", help = "help text")
    val bar by opts.opt("--bar", "-b").default { "defaultBar" }
    val baz by opts.opt("--baz", env = "SOME_RANDOM_ENV_VAR") { it.toInt() }
    val bin by opts.opt("--bin", env = "TEST_BINARY")
    val flag by opts.flag("--flag")

    fun help() = opts.help()
  }

  @Test fun basic() {
    val main = Main(ArgsParser("--foo", "razzledazzle"))
    assertThat(main.foo).isEqualTo("razzledazzle")
    assertThat(main.bar).isEqualTo("defaultBar")
  }

  @Test fun secondary() {
    val main = Main(ArgsParser("-f", "razzledazzle"))
    assertThat(main.foo).isEqualTo("razzledazzle")
  }

  @Test fun ints() {
    assertThat(Main(ArgsParser("--baz", "4")).baz).isEqualTo(4)
  }

  @Test fun flag() {
    assertThat(Main(ArgsParser("--baz", "4")).flag).isFalse()
    assertThat(Main(ArgsParser("--baz", "4", "--flag")).flag).isTrue()
  }

  @Test fun env() {
    assertThat(Main(ArgsParser("--baz", "4")).bin).isEqualTo("src/test/kotlin/args/${ArgsParserTest::class.simpleName}")
    assertThat(Main(ArgsParser("--baz", "4", "--bin", "bin")).bin).isEqualTo("bin")
  }

  @Test fun missingValue() {
    val main = Main(ArgsParser("--baz"))
    val e = assertThrows(IllegalStateException::class.java) { val foo = main.baz }
    assertThat(e).hasMessageThat().isEqualTo("Option --baz has no argument")
  }

  @Test fun missingValueWithTrailingFlag() {
    val main = Main(ArgsParser("--baz", "--flag"))
    val e = assertThrows(IllegalStateException::class.java) { val foo = main.baz }
    assertThat(e).hasMessageThat().isEqualTo("Option --baz expects a value, but found opt '--flag'")
  }

  @Test fun help() {
    val main = Main(ArgsParser("--baz"))
    assertThat(main.help()).isEqualTo(
      """
      Usage 'binary <options and flags> ...'
        --foo, -f
          help text
        --bar, -b
        --baz
        --bin
        --flag
      """.trimIndent()
    )
  }
}
