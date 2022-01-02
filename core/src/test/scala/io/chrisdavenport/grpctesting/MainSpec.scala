package io.chrisdavenport.grpctesting

import munit.CatsEffectSuite
import cats.effect._

class MainSpec extends CatsEffectSuite {

  test("Main should exit succesfully") {
    assert(true == true)
  }

}
