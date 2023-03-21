// Copyright (c) 2021 by Christian Gruber, All Rights Reserved.
// Freely licensed under MIT, BSD 2-clause, Apache 2.0, or WTFPL
package args

import ArgsParser
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import validate
import java.lang.IllegalArgumentException

class ArgsParserTest {

  class TestOpts(private val opts: ArgsParser) {
    val foo by opts.opt("--foo", "-f", help = "help text")
    val bar by opts.opt("--bar", "-b").default { "defaultBar" }
    val baz by opts.opt("--baz", env = "SOME_RANDOM_ENV_VAR") { it.toInt() }
    val bin by opts.opt("--bin", env = "TEST_BINARY")
    val flag by opts.flag("--flag")
    val hidden by opts.flag("--hidden", hidden = true, env="SOME_HIDDEN_FIELD")
    val optional by opts.opt("--optional").optional()

    fun help() = opts.help()
  }

  @Test fun basic() {
    val testOpts = TestOpts(ArgsParser("--foo", "razzledazzle"))
    assertThat(testOpts.foo).isEqualTo("razzledazzle")
    assertThat(testOpts.bar).isEqualTo("defaultBar")
  }

  @Test fun secondary() {
    val testOpts = TestOpts(ArgsParser("-f", "razzledazzle"))
    assertThat(testOpts.foo).isEqualTo("razzledazzle")
  }

  @Test fun `type conversion`() {
    assertThat(TestOpts(ArgsParser("--baz", "4")).baz).isEqualTo(4)
  }

  @Test fun flag() {
    assertThat(TestOpts(ArgsParser("--baz", "4")).flag).isFalse()
    assertThat(TestOpts(ArgsParser("--baz", "4", "--flag")).flag).isTrue()
  }

  @Test fun env() {
    assertThat(TestOpts(ArgsParser("--baz", "4")).bin)
      .isEqualTo("src/test/kotlin/args/${ArgsParserTest::class.simpleName}")
    assertThat(TestOpts(ArgsParser("--baz", "4", "--bin", "bin")).bin).isEqualTo("bin")
  }

  @Test fun hidden() {
    // Just to ensure the hidden flag is actually working.
    assertThat(TestOpts(ArgsParser("--hidden", "4")).hidden).isTrue()
  }

  @Test fun missingValue() {
    val testOpts = TestOpts(ArgsParser("--baz"))
    val e = assertThrows(IllegalStateException::class.java) { val foo = testOpts.baz }
    assertThat(e).hasMessageThat().isEqualTo("Option --baz has no argument")
  }

  @Test fun `optional avoids missing value error`() {
    val testOpts = TestOpts(ArgsParser("--optional", "something"))
    assertThat(testOpts.optional).isNotNull()
    val testOpts2 = TestOpts(ArgsParser())
    assertThat(testOpts2.optional).isNull()
  }

  @Test fun missingValueWithTrailingFlag() {
    val testOpts = TestOpts(ArgsParser("--baz", "--flag"))
    val e = assertThrows(IllegalStateException::class.java) { val foo = testOpts.baz }
    assertThat(e).hasMessageThat().isEqualTo("Option --baz expects a value, but found opt '--flag'")
  }

  @Test fun `multiple options classes`() {
    val parser = ArgsParser("--foo", "razzledazzle", "--whatever")
    val testOpts = TestOpts(parser)

    class OtherOpts(val opts: ArgsParser) {
      val whatever: Boolean by opts.flag("--whatever")
    }
    val otherOpts = OtherOpts(parser)
    assertThat(testOpts.foo).isEqualTo("razzledazzle")
    assertThat(testOpts.bar).isEqualTo("defaultBar")
    assertThat(otherOpts.whatever).isTrue()
  }

  @Test fun `all options registered - no class`() {
    val argsParser = ArgsParser("--baz", "--flag")
    assertThat(argsParser.opts).hasSize(0)
  }

  @Test fun `all options registered - one class`() {
    val argsParser = ArgsParser("--baz", "--flag")
    assertThat(argsParser.opts).hasSize(0)
    TestOpts(argsParser)
    assertThat(argsParser.opts.keys).hasSize(9)
    assertThat(argsParser.opts.values.toSet()).hasSize(7)
  }

  @Test fun `all options registered - multiple classes`() {
    val argsParser = ArgsParser("--baz", "baz", "--flag")
    assertThat(argsParser.opts).hasSize(0)
    TestOpts(argsParser)
    assertThat(argsParser.opts.keys).hasSize(9)
    assertThat(argsParser.opts.values.toSet()).hasSize(7)
    class OtherOpts(opts: ArgsParser) {
      val whoop by opts.opt("--whoop")
    }
    OtherOpts(argsParser) // initialize second class
    assertThat(argsParser.opts.keys).hasSize(10)
    assertThat(argsParser.opts.values.toSet()).hasSize(8)
  }

  @Test fun `overlapping opts classes fail`() {
    class OtherOpts(opts: ArgsParser) {
      val baz by opts.opt("--baz")
    }
    val parser = ArgsParser() // don't need real args for this test.
    TestOpts(parser) // initialize first class
    val e = assertThrows(IllegalArgumentException::class.java) { OtherOpts(parser) }
    assertThat(e.message).isEqualTo("Multiple definitions of --baz not supported.")
  }

  @Test fun `validate options - uninitialized`() {
    val parser = ArgsParser("--baz", "baz", "--flag")
    val e = assertThrows(IllegalStateException::class.java) { parser.validate() }
    assertThat(e.message).isEqualTo("ArgsParser has not been used yet, but attempted to validate.")
  }
  @Test fun `validate options - missing required`() {
    val parser = ArgsParser("--baz", "baz", "--flag")
    assertThat(parser.opts).hasSize(0)
    TestOpts(parser)
    val e = assertThrows(IllegalArgumentException::class.java) { parser.validate() }
    assertThat(e.message).isEqualTo("Missing arguments: --foo, --bin")
  }

  @Test fun `validate options - unknown`() {
    val parser = ArgsParser("--baz", "baz", "--whatisthisflag", "-q")
    assertThat(parser.opts).hasSize(0)
    TestOpts(parser)
    val e = assertThrows(IllegalArgumentException::class.java) { parser.validate() }
    assertThat(e.message).isEqualTo("Unknown arguments: --whatisthisflag, -q")
  }

  @Test fun help() {
    val parser = ArgsParser("--baz").also { TestOpts(it) /* force loading of opt definitions */ }
    assertThat(parser.help()).isEqualTo(
      """
      Usage 'binary <options and flags> ...'
        --foo, -f
          help text
        --bar, -b (optional)
        --baz
        --bin
        --flag (optional)
        --optional (optional)
      """.trimIndent()
    )
  }
}
