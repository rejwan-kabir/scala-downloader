package demo

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

object Downloader extends App {
  if (args.length != 2) {
    println("usage : <main-class> <url-list> <download-directory-path>")
    System.exit(1)
  }

  type FilePath = String
  implicit val destination: FilePath = if (args(1).endsWith("/")) args(1) else args(1) + "/"
  val urls = args(0).split(',').toList
  val timeout = 30 * 1000 // 30s

  def readChunk = 16 * 1024

  def fromInputStream(in: InputStream, bufferSize: Int): Stream[Array[Byte]] = {
    val buffer = new Array[Byte](bufferSize)
    in.read(buffer) match {
      case -1 => Stream.empty
      case len => buffer.slice(0, len) #:: fromInputStream(in, bufferSize)
    }
  }

  def fetch(in: InputStream)(urlString: String, cleanUp: => Unit = ())(destination: FilePath): File = {
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

  def fetchHTTP(urlString: String): FilePath => File = {
    val connection = new URL(urlString).openConnection.asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("GET")
    connection.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)")
    connection.setConnectTimeout(timeout)
    connection.setReadTimeout(timeout)
    fetch(connection.getInputStream)(urlString, connection.disconnect())
  }

  def fetchFTP(urlString: String): FilePath => File = {
    val connection = new URL(urlString).openConnection.asInstanceOf[FtpURLConnection]
    connection.setConnectTimeout(timeout)
    connection.setReadTimeout(timeout)
    fetch(connection.getInputStream)(urlString)
  }

  def fetchSFTP(urlString: String): FilePath => File = {
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
    session.setTimeout(timeout)
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

  def getProtocolConnection(urlString: String)(implicit destination: FilePath): File = {
    println("Processing : " + urlString)
    urlString.split("://")(0) match {
      case "http" | "https" => fetchHTTP(urlString)(destination)
      case "ftp" | "ftps" => fetchFTP(urlString)(destination)
      case "sftp" => fetchSFTP(urlString)(destination)
      case _ => throw new NoSuchElementException("Unsupported protocol")
    }
  }

  def futureToFutureTry[T](f: Future[T]): Future[Try[T]] =
    f.map(x => Success(x)).recover { case ex => Failure(ex) }

  Files.createDirectory(Paths.get(destination))
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