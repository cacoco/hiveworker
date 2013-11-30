package io.angstrom.hiveworker.test

import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.mock.MockitoSugar
import org.scalatest.testng.TestNGSuite

trait UnitTest
  extends TestNGSuite
  with ShouldMatchers
  with MockitoSugar {

  val mocks: Seq[Any] = Seq.empty

  protected def resetAll() {
    for (mock <- mocks) {
      reset(mock)
    }
  }

}
