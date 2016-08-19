package org.splink.raven
import org.scalatest._
import org.splink.raven.FunctionMacros._

class FunctionMacrosTest extends FlatSpec with Matchers {

  object TestFunctions {
    case class Complex(s: String)

    def f1 = "f1()"
    def f2(s: String) = s"f2($s)"
    def f3(s: String, i: Int) = s"f2($s, $i)"
    def f4(c: Complex) = s"f4($c)"
    val f5: (String, Int) => String = (s: String, i: Int) => s"f5($s)"
  }

  "A function without parameters" should "not yield any types" in {
    val result = signature(TestFunctions.f1 _)
    result.types should be (empty)
  }

  "A function with one parameter" should "yield the name and type of the parameter" in {
    val result = signature(TestFunctions.f2 _)
    result.types.head should be ("s" -> "java.lang.String")
  }

  "A function with two parameters" should "yield the names and types of these parameters" in {
    val result = signature(TestFunctions.f3 _)
    result.types should be (List("s" -> "java.lang.String", "i" -> "scala.Int"))
  }

  "A function with a complex parameter" should "yield the name and type of the parameter" in {
    val result = signature(TestFunctions.f4 _)
    result.types.head should be ("c" -> "org.splink.raven.FunctionMacrosTest.TestFunctions.Complex")
  }

  "A function literal" should "not yield any types, because it is impossible to determine it's parameter name(s)" in {
    val result = signature(TestFunctions.f5 _)
    result.types should be (empty)
  }
}