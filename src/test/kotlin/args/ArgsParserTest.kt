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

  enum class ArgsEnum { FIRST, SECOND, THIRD }
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

  @Test fun enumeratedValues() {
    class EnumOptions(private val opts: ArgsParser) {
      val required by opts.enumerated(
          "--required1" to ArgsEnum.FIRST,
          "--required2" to ArgsEnum.SECOND,
          "--required3" to ArgsEnum.THIRD,
          help = "required enum values"
      )
      val optional by opts.enumerated(
          "--optional1" to ArgsEnum.FIRST,
          "--optional2" to ArgsEnum.SECOND,
          "--optional3" to ArgsEnum.THIRD,
          help = "optional enum values"
      ).optional()
      val defaulted by opts.enumerated(
          "--defaulted1" to ArgsEnum.FIRST,
          "--defaulted2" to ArgsEnum.SECOND,
          "--defaulted3" to ArgsEnum.THIRD,
          help = "enum values with a default enum values"
      ).default { ArgsEnum.SECOND }
    }

    // test required values
    assertThat(EnumOptions(ArgsParser("--required1")).required).isEqualTo(ArgsEnum.FIRST)
    assertThat(EnumOptions(ArgsParser("--required2")).required).isEqualTo(ArgsEnum.SECOND)
    assertThat(EnumOptions(ArgsParser("--required3")).required).isEqualTo(ArgsEnum.THIRD)

    // Test optional values (--required1 there to satisfy... the requirement)
    assertThat(EnumOptions(ArgsParser("--required1")).optional).isNull()
    assertThat(EnumOptions(ArgsParser("--required1", "--optional1")).optional).isEqualTo(ArgsEnum.FIRST)
    assertThat(EnumOptions(ArgsParser("--required1", "--optional2")).optional).isEqualTo(ArgsEnum.SECOND)
    assertThat(EnumOptions(ArgsParser("--required1", "--optional3")).optional).isEqualTo(ArgsEnum.THIRD)

    // test defaulted values
    assertThat(EnumOptions(ArgsParser("--required1")).defaulted).isEqualTo(ArgsEnum.SECOND)
    assertThat(EnumOptions(ArgsParser("--required1", "--defaulted1")).defaulted).isEqualTo(ArgsEnum.FIRST)
    assertThat(EnumOptions(ArgsParser("--required1", "--defaulted2")).defaulted).isEqualTo(ArgsEnum.SECOND)
    assertThat(EnumOptions(ArgsParser("--required1", "--defaulted3")).defaulted).isEqualTo(ArgsEnum.THIRD)

    // test overlapping mapped values
    val e = assertThrows(java.lang.IllegalStateException::class.java) {
      EnumOptions(ArgsParser("--required1", "--required2")).required
    }
    assertThat(e.message).isEqualTo("Only one of --required1, --required2, --required3 should appear in the args")

    // test missing
    val e2 = assertThrows(java.lang.IllegalStateException::class.java) { EnumOptions(ArgsParser()).required }
    assertThat(e2.message).isEqualTo("Could not find --required1, --required2, --required3 in args")

    // test help
    val parser = ArgsParser(name = "myapp").also { EnumOptions(it) /* force loading of opt definitions */ }
    assertThat(parser.help()).isEqualTo(
        """
      Usage 'myapp <options and flags> ...'
        --required1, --required2, --required3
          required enum values
        --optional1, --optional2, --optional3 (optional)
          optional enum values
        --defaulted1, --defaulted2, --defaulted3 (optional)
          enum values with a default enum values
      """.trimIndent()
    )
  }

  @Test fun hidden() {
    // Just to ensure the hidden flag is actually working.
    assertThat(TestOpts(ArgsParser("--hidden", "4")).hidden).isTrue()
  }

  @Test fun missinOption() {
    val testOpts = TestOpts(ArgsParser())
    val e = assertThrows(IllegalStateException::class.java) { val foo = testOpts.foo }
    assertThat(e).hasMessageThat().isEqualTo("Could not find --foo, -f in args")
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
    val parser = ArgsParser("--baz", name = "myapp").also { TestOpts(it) /* force loading of opt definitions */ }
    assertThat(parser.help()).isEqualTo(
      """
      Usage 'myapp <options and flags> ...'
        --foo, -f
          help text
        --bar, -b (optional)
        --baz [env-var: SOME_RANDOM_ENV_VAR]
        --bin [env-var: TEST_BINARY]
        --flag (optional)
        --optional (optional)
      """.trimIndent()
    )
  }
}
