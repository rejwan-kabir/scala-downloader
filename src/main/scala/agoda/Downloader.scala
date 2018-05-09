package agoda

import java.io._
import java.net.{HttpURLConnection, URL}
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption._
import java.nio.file.{Files, Paths}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

object Downloader extends App {
  type FilePath = String
  implicit val destination: FilePath = "/home/rejwan/TEMP/"
  val tempDir = destination // System.getProperty("java.io.tmpdir", "/tmp")
  val readChunk = 16 * 1024

  val urls = List(
    "http://longwallpapers.com/Desktop-Wallpaper/rain-wallpapers-hd-For-Desktop-Wallpaper.jpg",
    "https://wallpaper.wiki/wp-content/uploads/2017/06/Light-water-close-up-nature-rain-wallpapers-HD.jpg",
    "https://i.pinimg.com/564x/2e/88/31/2e8831e90095c14437bbb866dd7cd3ec.jpg",
    "https://i.pinimg.com/564x/5c/46/e4/5c46e4d74edf8e4c396beda8a126397f.jpg",
    "https://i.pinimg.com/564x/30/f9/51/30f9518869ddedf7bddd5e5a5e65d5a2.jpg",
    "https://i.pinimg.com/564x/3c/64/db/3c64db15ff4a2351cf29634eb7c9240c.jpg",
    "https://i.pinimg.com/564x/70/6c/bd/706cbd9f15223e48168941f89aefff22.jpg")

  def fromInputStream(in: InputStream, bufferSize: Int): Stream[Array[Byte]] = {
    val buffer = new Array[Byte](bufferSize)
    in.read(buffer) match {
      case -1 => Stream.empty
      case len => buffer.slice(0, len) #:: fromInputStream(in, bufferSize)
    }
  }

  def fetch(urlString: String)(implicit destination: FilePath): File = {
    println("Processing : " + urlString)
    val fileName = urlString.substring(urlString.lastIndexOf('/') + 1)
    val file = new File(destination + fileName)
    val connection = new URL(urlString).openConnection.asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("GET")
    connection.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)")
    val in = connection.getInputStream
    val out = FileChannel.open(Paths.get(destination + fileName), CREATE, WRITE)
    try {
      val stream = fromInputStream(in, readChunk)
      stream.zipWithIndex.map {
        case (data: Array[Byte], i: Int) =>
          val file = new File(tempDir + fileName + "_" + i)
          val intermediate = new BufferedOutputStream(new FileOutputStream(file))
          intermediate.write(data)
          intermediate.flush()
          intermediate.close()
          tempDir + fileName + "_" + i
      }.foreach(filePath => {
        val in = FileChannel.open(Paths.get(filePath), READ)
        in.transferTo(0, in.size, out)
        in.close()
        Files.delete(Paths.get(filePath))
      })
    } finally {
      out.close()
      connection.disconnect()
    }
    file
  }

  val futureList: List[Future[File]] = urls.map(u => Future(fetch(u)))
  futureList.foreach(_.onComplete {
    case Success(file) => println(file.getAbsolutePath + " completed")
    case Failure(ex) => println("exception on download in " + ex.getMessage)
  })

  val futureSeq = Future.sequence(futureList)
  futureSeq.onComplete {
    case Success(_) => println("All downloads completed")
    case Failure(e) => println("Exception happened in " + e.getMessage)
  }

  Await.ready(futureSeq, Duration.Inf)
}
