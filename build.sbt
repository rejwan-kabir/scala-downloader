name := "scala-downloader"

version := "0.1"

scalaVersion := "2.12.6"

// libraryDependencies += "org.apache.commons" % "commons-vfs2" % "2.2"
libraryDependencies ++= Seq(
  "com.jcraft" % "jsch" % "0.1.54",
  "org.scalatest" %% "scalatest" % "3.0.5" % Test)