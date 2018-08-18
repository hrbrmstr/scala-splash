package splash

import org.scalatest._
import splish.Splash

class SplashSpec extends FlatSpec with Matchers {

  val s : Splash = Splash()
  val testURL = "https://rud.is/splash-test.html"

  "isActive()" should "be true" in {
    s.isActive() shouldEqual true
  }

  "version()" should "produce the correct version information" in {
    s.version()("splash").str shouldEqual "3.2"
  }

  "performanceStatistics()" should "produce the correct return structure" in {
    s.performanceStatistics()("cputime").num.getClass.toString shouldEqual "double"
  }

  "history()" should "produce the correct return structure" in {
    s.history().arr.getClass.toString shouldEqual "class scala.collection.mutable.ArrayBuffer"
  }

  "renderHTML()" should "return the test content" in {
    s.renderHTML(testURL) shouldEqual "<html><head>\n<title>Test</title>\n</head>\n<body>\ntest\n\n\n</body></html>"
  }

  "renderHAR()" should "produce a valid HAR document" in {
    s.renderHAR(testURL)("log")("pages")(0)("title").str shouldEqual "Test"
  }

  "renderJSON()" should "produce a valid JSON document" in {
    s.renderJSON(testURL)("title").str shouldEqual "Test"
  }

}
