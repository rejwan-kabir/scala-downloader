package agoda

import java.io._
import java.net.{HttpURLConnection, URI, URL}
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption._
import java.nio.file.{Files, Paths}
import java.util.Properties

import com.jcraft.jsch.JSch
import sun.net.www.protocol.ftp.FtpURLConnection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

// run with command line argument: https://i.pinimg.com/564x/5c/46/e4/shuvo.jpg,ftp://speedtest.tele2.net/512KB.zip,sftp://rejwan:amishuvo@localhost:22/home/rejwan/Dropbox/Photos/IMG_20150825_145004.jpg,http://longwallpapers.com/Desktop-Wallpaper/rain-wallpapers-hd-For-Desktop-Wallpaper.jpg,ftp://speedtest.tele2.net/2MB.zip,https://wallpaper.wiki/wp-content/uploads/2017/06/Light-water-close-up-nature-rain-wallpapers-HD.jpg,sftp://rejwan:amishuvo@localhost:22/home/rejwan/Dropbox/Photos/IMG_20150825_144432.jpg,https://i.pinimg.com/564x/2e/88/31/2e8831e90095c14437bbb866dd7cd3ec.jpg,ftp://speedtest.tele2.net/3MB.zip,https://i.pinimg.com/564x/5c/46/e4/5c46e4d74edf8e4c396beda8a126397f.jpg,https://i.pinimg.com/564x/5c/46/e4/imon.jpg,ftp://speedtest.tele2.net/5MB.zip,https://i.pinimg.com/564x/30/f9/51/30f9518869ddedf7bddd5e5a5e65d5a2.jpg,https://i.pinimg.com/564x/3c/64/db/3c64db15ff4a2351cf29634eb7c9240c.jpg,sftp://rejwan:amishuvo@localhost:22/home/rejwan/Dropbox/Photos/IMG_20150825_145034.jpg,https://i.pinimg.com/564x/70/6c/bd/706cbd9f15223e48168941f89aefff22.jpg,https://i.pinimg.com/564x/5c/46/e4/arshi.jpg
object Downloader extends App {
  type FilePath = String
  implicit val destination: FilePath = if (args(1).endsWith("/")) args(1) else args + "/"
  val readChunk = 16 * 1024
  val urls = args(0).split(',').toList

  /*
  val urls = List(
    "https://i.pinimg.com/564x/5c/46/e4/shuvo.jpg",
    "ftp://speedtest.tele2.net/512KB.zip",
    "sftp://rejwan:amishuvo@localhost:22/home/rejwan/Dropbox/Photos/IMG_20150825_145004.jpg",
    "http://longwallpapers.com/Desktop-Wallpaper/rain-wallpapers-hd-For-Desktop-Wallpaper.jpg",
    "ftp://speedtest.tele2.net/2MB.zip",
    "https://wallpaper.wiki/wp-content/uploads/2017/06/Light-water-close-up-nature-rain-wallpapers-HD.jpg",
    "sftp://rejwan:amishuvo@localhost:22/home/rejwan/Dropbox/Photos/IMG_20150825_144432.jpg",
    "https://i.pinimg.com/564x/2e/88/31/2e8831e90095c14437bbb866dd7cd3ec.jpg",
    "ftp://speedtest.tele2.net/3MB.zip",
    "https://i.pinimg.com/564x/5c/46/e4/5c46e4d74edf8e4c396beda8a126397f.jpg",
    "https://i.pinimg.com/564x/5c/46/e4/imon.jpg",
    "ftp://speedtest.tele2.net/5MB.zip",
    "https://i.pinimg.com/564x/30/f9/51/30f9518869ddedf7bddd5e5a5e65d5a2.jpg",
    "https://i.pinimg.com/564x/3c/64/db/3c64db15ff4a2351cf29634eb7c9240c.jpg",
    "sftp://rejwan:amishuvo@localhost:22/home/rejwan/Dropbox/Photos/IMG_20150825_145034.jpg",
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

  private[this] def fetch(in: InputStream)(urlString: String, cleanUp: => Unit = ())(implicit destination: FilePath): File = {
    val fileName = urlString.substring(urlString.lastIndexOf('/') + 1)
    val file = new File(destination + fileName)
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
      splitFileStream.toList.foreach(filePath => {
        val downloadedFile = FileChannel.open(Paths.get(filePath), READ)
        downloadedFile.transferTo(0, downloadedFile.size, out)
        downloadedFile.close()
      })
    } catch {
      case ex: Exception =>
        out.close()
        Files.delete(Paths.get(destination + fileName))
        throw ex
    }
    finally {
      in.close()
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
    connection.setConnectTimeout(30000)
    connection.setReadTimeout(30000)
    fetch(connection.getInputStream)(urlString, connection.disconnect())
  }

  private[this] def fetchFTP(urlString: String) = {
    val connection = new URL(urlString).openConnection.asInstanceOf[FtpURLConnection]
    connection.setConnectTimeout(30000)
    connection.setReadTimeout(30000)
    fetch(connection.getInputStream)(urlString)
  }

  private[this] def fetchSFTP(urlString: String) = {
    val uri = new URI(urlString)
    val jsch = new JSch
    val Array(user, password) = uri.getUserInfo.split(":")
    val session = jsch.getSession(user, uri.getHost, uri.getPort)
    import com.jcraft.jsch.ChannelSftp
    val config = new Properties
    config.put("StrictHostKeyChecking", "no")
    session.setConfig(config)
    session.setPassword(password)
    session.connect()
    session.setTimeout(30000)
    val channel = session.openChannel("sftp")
    channel.connect()
    val channelSftp = channel.asInstanceOf[ChannelSftp]
    val (directory, fileName) = uri.getPath.splitAt(uri.getPath.lastIndexOf('/') + 1)
    channelSftp.cd(directory)
    fetch(channelSftp.get(fileName))(urlString, {
      session.disconnect()
      channel.disconnect()
    })
  }

  def getProtocolConnection(urlString: String) = {
    println("Processing : " + urlString)
    urlString.split("://")(0) match {
      case "http" | "https" => fetchHTTP(urlString)
      case "ftp" | "ftps" => fetchFTP(urlString)
      case "sftp" => fetchSFTP(urlString)
      case _ => throw new NoSuchElementException("Unsupported protocol")
    }
  }

  def futureToFutureTry[T](f: Future[T]): Future[Try[T]] =
    f.map(x => Success(x)).recover { case ex => Failure(ex) }

  val listOfFutureTry: List[Future[Try[File]]] = urls.map(u => futureToFutureTry(Future(getProtocolConnection(u))))
  val futureSeq = Future.sequence(listOfFutureTry)
  listOfFutureTry.map(_.collect {
    case Success(x) => s"Successful Download : ${x.getAbsolutePath}"
    case Failure(e) => s"Download failed for - ${e.getMessage}"
  }).foreach(_.onComplete {
    case Success(x) => println(x)
    case Failure(e) => println(e.getMessage)
  })

  Await.ready(futureSeq, Duration.Inf)
}