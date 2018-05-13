import java.io._
import java.net.UnknownHostException

import demo.Downloader._
import org.apache.commons.io.FileUtils
import org.scalatest.{FlatSpec, Matchers}

class DownloaderTest extends FlatSpec with Matchers {
  "unknown protocol" should "throw NoSuchElementException" in {
    val exp = intercept[NoSuchElementException] {
      getProtocolConnection("unknown://abc.xyz.com/file.ext")
    }
    assert(exp.getMessage == "Unsupported protocol")
  }

  "text files" should "be found and matched" in {
    val inputFile = new File("src/test/resource/source/testFile.txt")
    val outputFile = fetch(new BufferedInputStream(new FileInputStream(inputFile)))("src/test/resource/source/testFile.txt")("src/test/resource/destination/")
    assert(FileUtils.contentEquals(inputFile, outputFile))
  }

  "image files" should "be found and matched" in {
    val inputFile = new File("src/test/resource/source/Rain.jpg")
    val outputFile = fetch(new BufferedInputStream(new FileInputStream(inputFile)))("src/test/resource/source/Rain.jpg")("src/test/resource/destination/")
    assert(FileUtils.contentEquals(inputFile, outputFile))
  }

  "zipped files" should "be found and matched" in {
    val inputFile = new File("src/test/resource/source/zipped.tar.gz")
    val outputFile = fetch(new BufferedInputStream(new FileInputStream(inputFile)))("src/test/resource/source/zipped.tar.gz")("src/test/resource/destination/")
    assert(FileUtils.contentEquals(inputFile, outputFile))
  }

  "garbage url" should "throw UnKnownHostException" in {
    val exp = intercept[UnknownHostException] {
      getProtocolConnection("http://jnhfkjhkjhrkjykjrhwejk.com/Desktop-Wallpaper/rain-wallpapers-hd-For-Desktop-Wallpaper.jpg")("src/test/resource/destination/")
    }
    assert(exp.getMessage == "jnhfkjhkjhrkjykjrhwejk.com")
  }
}
