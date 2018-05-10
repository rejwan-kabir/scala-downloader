package agoda

import java.io._
import java.net.{HttpURLConnection, URL, URLConnection}
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption._
import java.nio.file.{Files, Paths}

import sun.net.www.protocol.ftp.FtpURLConnection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

// run with command line argument: https://i.pinimg.com/564x/5c/46/e4/shuvo.jpg,ftp://speedtest.tele2.net/512KB.zip,http://longwallpapers.com/Desktop-Wallpaper/rain-wallpapers-hd-For-Desktop-Wallpaper.jpg,ftp://speedtest.tele2.net/2MB.zip,https://wallpaper.wiki/wp-content/uploads/2017/06/Light-water-close-up-nature-rain-wallpapers-HD.jpg,https://i.pinimg.com/564x/2e/88/31/2e8831e90095c14437bbb866dd7cd3ec.jpg,ftp://speedtest.tele2.net/3MB.zip,https://i.pinimg.com/564x/5c/46/e4/5c46e4d74edf8e4c396beda8a126397f.jpg,https://i.pinimg.com/564x/5c/46/e4/imon.jpg,ftp://speedtest.tele2.net/5MB.zip,https://i.pinimg.com/564x/30/f9/51/30f9518869ddedf7bddd5e5a5e65d5a2.jpg,https://i.pinimg.com/564x/3c/64/db/3c64db15ff4a2351cf29634eb7c9240c.jpg,https://i.pinimg.com/564x/70/6c/bd/706cbd9f15223e48168941f89aefff22.jpg,https://i.pinimg.com/564x/5c/46/e4/arshi.jpg /home/rejwan/TEMP/
object Downloader extends App {
  type FilePath = String
  implicit val destination: FilePath = if (args(1).endsWith("/")) args(1) else args + "/"
  val readChunk = 16 * 1024
  val urls = args(0).split(',').toList

  /*
  val urls = List(
    "https://i.pinimg.com/564x/5c/46/e4/shuvo.jpg",
    "ftp://speedtest.tele2.net/512KB.zip",
    "http://longwallpapers.com/Desktop-Wallpaper/rain-wallpapers-hd-For-Desktop-Wallpaper.jpg",
    "ftp://speedtest.tele2.net/2MB.zip",
    "https://wallpaper.wiki/wp-content/uploads/2017/06/Light-water-close-up-nature-rain-wallpapers-HD.jpg",
    "https://i.pinimg.com/564x/2e/88/31/2e8831e90095c14437bbb866dd7cd3ec.jpg",
    "ftp://speedtest.tele2.net/3MB.zip",
    "https://i.pinimg.com/564x/5c/46/e4/5c46e4d74edf8e4c396beda8a126397f.jpg",
    "https://i.pinimg.com/564x/5c/46/e4/imon.jpg",
    "ftp://speedtest.tele2.net/5MB.zip",
    "https://i.pinimg.com/564x/30/f9/51/30f9518869ddedf7bddd5e5a5e65d5a2.jpg",
    "https://i.pinimg.com/564x/3c/64/db/3c64db15ff4a2351cf29634eb7c9240c.jpg",
    "https://i.pinimg.com/564x/70/6c/bd/706cbd9f15223e48168941f89aefff22.jpg",
    "https://i.pinimg.com/564x/5c/46/e4/arshi.jpg"
  )
  */
  def fromInputStream(in: InputStream, bufferSize: Int): Stream[Array[Byte]] = {
    val buffer = new Array[Byte](bufferSize)
    in.read(buffer) match {
      case -1 => Stream.empty
      case len => buffer.slice(0, len) #:: fromInputStream(in, bufferSize)
    }
  }

  private[this] def fetch(connection: URLConnection)(urlString: String, cleanUp: => Unit = ())(implicit destination: FilePath): File = {
    println("Processing : " + urlString)
    val fileName = urlString.substring(urlString.lastIndexOf('/') + 1)
    val file = new File(destination + fileName)
    val in = connection.getInputStream
    val out = FileChannel.open(Paths.get(destination + fileName), CREATE, WRITE)
    try {
      val stream = fromInputStream(in, readChunk)
      val splitFileStream = stream.zipWithIndex.map {
        case (data: Array[Byte], i: Int) =>
          val file = File.createTempFile(fileName, s"_$i")
          val intermediate = new BufferedOutputStream(new FileOutputStream(file))
          intermediate.write(data)
          intermediate.flush()
          intermediate.close()
          file.deleteOnExit()
          file.getAbsolutePath
      }
      splitFileStream.foreach(filePath => {
        val in = FileChannel.open(Paths.get(filePath), READ)
        in.transferTo(0, in.size, out)
        in.close()
      })
    } catch {
      case ex: Exception =>
        out.close()
        Files.delete(Paths.get(destination + fileName))
        throw ex
    }
    finally {
      if (out.isOpen)
        out.close()
      cleanUp
    }
    file
  }

  private[this] def fetchHTTP(urlString: String) = {
    val connection = new URL(urlString).openConnection.asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("GET")
    connection.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)")
    fetch(connection)(urlString, connection.disconnect())
  }

  private[this] def fetchFTP(urlString: String) = {
    val connection = new URL(urlString).openConnection.asInstanceOf[FtpURLConnection]
    fetch(connection)(urlString)
  }

  def getProtocolConnection(urlString: String) = {
    urlString.split("://")(0) match {
      case "http" | "https" => fetchHTTP(urlString)
      case "ftp" | "sftp" => fetchFTP(urlString)
      case _ => throw new NoSuchElementException("Unsupported protocol")
    }
  }

  def futureToFutureTry[T](f: Future[T]): Future[Try[T]] =
    f.map(x => Success(x)).recover { case ex => Failure(ex) }

  val listOfFutureTry: List[Future[Try[File]]] = urls.map(u => futureToFutureTry(Future(getProtocolConnection(u))))
  val futureSeq = Future.sequence(listOfFutureTry)
  listOfFutureTry.map(_.collect {
    case Success(x) => s"Successful Download : ${x.getAbsolutePath}"
    case Failure(ex) => s"Download failed for - ${ex.getMessage}"
  }).foreach(_.onComplete {
    case Success(x) => println(x)
    case Failure(ex) => println(ex.getMessage)
  })

  Await.ready(futureSeq, Duration.Inf)
}