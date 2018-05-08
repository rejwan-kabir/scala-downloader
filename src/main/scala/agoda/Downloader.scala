package agoda

import java.io.{BufferedOutputStream, File, FileOutputStream}
import java.net.{HttpURLConnection, URL}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object Downloader extends App {
  type Destination = String
  implicit val destination: Destination = "/home/rejwan/TEMP/"

  val urls = List(
    "http://longwallpapers.com/Desktop-Wallpaper/rain-wallpapers-hd-For-Desktop-Wallpaper.jpg",
    "https://wallpaper.wiki/wp-content/uploads/2017/06/Light-water-close-up-nature-rain-wallpapers-HD.jpg",
    "https://i.pinimg.com/564x/2e/88/31/2e8831e90095c14437bbb866dd7cd3ec.jpg",
    "https://i.pinimg.com/564x/5c/46/e4/5c46e4d74edf8e4c396beda8a126397f.jpg",
    "https://i.pinimg.com/564x/30/f9/51/30f9518869ddedf7bddd5e5a5e65d5a2.jpg",
    "https://i.pinimg.com/564x/3c/64/db/3c64db15ff4a2351cf29634eb7c9240c.jpg",
    "https://i.pinimg.com/564x/70/6c/bd/706cbd9f15223e48168941f89aefff22.jpg")

  def fetch(urlString: String)(implicit destination: Destination): File = {
    val fileName = urlString.substring(urlString.lastIndexOf('/') + 1)
    val file = new File(destination + fileName)
    val connection = new URL(urlString).openConnection.asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("GET")
    connection.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)")
    val in = connection.getInputStream
    val out = new BufferedOutputStream(new FileOutputStream(file))
    try {
      val byteArray = Stream.continually(in.read).takeWhile(_ != -1).map(_.toByte).toArray
      out.write(byteArray)
      out.flush()
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
  /*
  val futureSeq = Future.sequence(futureList)
  futureSeq.onComplete {
    case Success(_) => println("All downloads completed")
    case Failure(e) => println("Exception happened in " + e.getMessage)
  }
  */
  Thread.sleep(120000)
}
