package me.jeffmay.neo4j.client.cypher

import me.jeffmay.neo4j.client.Show
import org.scalatest.WordSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class DefaultCypherShowsSpec
  extends WordSpec
  with DefaultCypherShows {

  "DefaultCypherShows" should {

    "show a String like \"string\"" in {
      forAll() { (str: String) =>
        assertResult("\"" + str + "\"") {
          Show[CypherString].show(CypherString(str))
        }
      }
    }

    "show a Char like 'c'" in {
      forAll() { (c: Char) =>
        assertResult(s"'$c'") {
          Show[CypherChar].show(CypherChar(c))
        }
      }
    }

    "show an Int like 123" in {
      assertResult("123") {
        Show[CypherInt].show(CypherInt(123))
      }
    }

    "show an Boolean like true" in {
      assertResult("true") {
        Show[CypherBoolean].show(CypherBoolean(true))
      }
    }

    "show an Long like 123L" in {
      assertResult("123L") {
        Show[CypherLong].show(CypherLong(123L))
      }
    }

    "show an Double like 1.23D" in {
      assertResult("1.23D") {
        Show[CypherDouble].show(CypherDouble(1.23D))
      }
    }

    "show an Float like 1.23f" in {
      assertResult("1.23f") {
        Show[CypherFloat].show(CypherFloat(1.23f))
      }
    }

    "show a Short like 123s" in {
      assertResult("123s") {
        Show[CypherShort].show(CypherShort(123))
      }
    }

    "show a Byte as hexadecimal like 0x24" in {
      forAll() { (b: Byte) =>
        assertResult(f"0x$b%02x") {
          Show[CypherByte].show(CypherByte(b))
        }
      }
    }
  }

}
