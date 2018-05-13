import java.io._
import java.net.UnknownHostException

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

  "testFile.txt" should "be found and matched" in {
    def equal(src: File, dest: File): Boolean = {
      Source.fromFile(src).getLines().zipAll(Source.fromFile(dest).getLines(), "src", "dest").forall {
        case (srcLine: String, destLine: String) => srcLine == destLine
      }
    }

    val inputFile = new File("src/test/resource/source/testFile.txt")
    val outputFile = fetch(new BufferedInputStream(new FileInputStream(inputFile)))("src/test/resource/source/testFile.txt")("src/test/resource/destination/")
    assert(equal(inputFile, outputFile))
  }

  "garbage url" should "throw UnKnownHostException" in {
    val exp = intercept[UnknownHostException] {
      getProtocolConnection("http://jnhfkjhkjhrkjykjrhwejk.com/Desktop-Wallpaper/rain-wallpapers-hd-For-Desktop-Wallpaper.jpg")("src/test/resource/destination/")
    }
    assert(exp.getMessage == "jnhfkjhkjhrkjykjrhwejk.com")
  }
}
