package tests

import com.mle.cert.{CertMaker, CertRequestInfo}
import org.scalatest.FunSuite
import org.scalatestplus.play.OneAppPerSuite

import scala.concurrent.duration.DurationLong

/**
 * @author Michael
 */
class CommandTests extends FunSuite with OneAppPerSuite {
  val testConf = CertRequestInfo("FI", "Test State", "Test Locality", "Test Org", "Me", "a", "aaaaaa")
  test("can create ca, server and client certs") {
    val response = CertMaker.run(testConf)
    val result = response.await(30.seconds)
    assert(result.isSuccess)
  }
}
