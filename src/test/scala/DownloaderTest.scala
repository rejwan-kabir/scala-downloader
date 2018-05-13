import java.io._

import demo.Downloader._
import org.scalatest.{FlatSpec, Matchers}

import scala.io.Source

class DownloaderTest extends FlatSpec with Matchers {
  "unknown protocol" should "throw NoSuchElementException" in {
    val exp = intercept[NoSuchElementException] {
      getProtocolConnection("unknown://abc.xyz.com/file.ext")
    }
    assert(exp.getMessage == "Unsupported protocol")
  }

  "testFile123" should "be found and matched" in {
    def equal(src: File, dest: File): Boolean = {
      Source.fromFile(src).getLines().zipAll(Source.fromFile(dest).getLines(), "src", "dest").forall {
        case (srcLine: String, destLine: String) => srcLine == destLine
      }
    }

    val inputFile = new File("src/test/resource/source/testFile.txt")
    val outputFile = fetch(new BufferedInputStream(new FileInputStream(inputFile)))("src/test/resource/source/testFile.txt")("src/test/resource/destination/")
    assert(equal(inputFile, outputFile))
  }
}
