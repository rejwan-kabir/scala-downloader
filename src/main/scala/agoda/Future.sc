import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

val a = Future[Unit] {
  for (i <- 1 to 10) {
    print("A")
    Thread.sleep(1000)
  }
}

a.onComplete {
  case Success(_) => println("\nA done")
  case Failure(_) => println("\nA failed")
}

val b = Future[Unit] {
  for (i <- 1 to 10) {
    print("B")
    Thread.sleep(1000)
  }
}

b.onComplete {
  case Success(_) => println("\nB done")
  case Failure(_) => println("\nB failed")
}

for {
  ma <- a
  mb <- b
} yield (ma,mb)

