package learn

import scala.concurrent.Future
import scala.util.{Failure, Random, Success}
import scala.concurrent.ExecutionContext.Implicits.global

object Example1 extends App {
  println("Before computation")
  
  val f = Future[Int] {
    Thread.sleep(Random.nextInt(500))
    42
  }

  println("Before onComplete")

  f.onComplete {
    case Success(s) => println(s)
    case Failure(ex) => println("exception : " + ex.getMessage)
  }

  println("A...")
  Thread.sleep(100)
  println("B...")
  Thread.sleep(100)
  println("C...")
  Thread.sleep(100)
  println("D...")
  Thread.sleep(100)
  println("E...")
  Thread.sleep(100)
  println("F...")
  Thread.sleep(100)
  println("G...")
  Thread.sleep(100)

  Thread.sleep(2000)
}
