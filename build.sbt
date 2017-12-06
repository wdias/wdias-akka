organization := "org.wdias"
name := "wdias"
version := "0.1"
scalaVersion := "2.12.4"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

lazy val akkaVersion = "2.5.7"
lazy val akkaHttpVersion = "10.0.10"
lazy val scalaTestVersion = "3.0.4"
lazy val influxDBClientVersion = "0.5.2"
lazy val logBackVersion = "1.2.3"
lazy val slickVersion = "3.2.1"
lazy val mysqlConnectorVersion = "5.1.36"
lazy val netcdfVersion = "4.6.11"

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "com.paulgoldbaum" %% "scala-influxdb-client" % influxDBClientVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion,
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % logBackVersion,
    "com.typesafe.slick" %% "slick" % slickVersion,
    "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
    "mysql" % "mysql-connector-java" % mysqlConnectorVersion,
    "edu.ucar" % "netcdfAll" % netcdfVersion
)

resolvers += "Unidata Releases" at "https://artifacts.unidata.ucar.edu/repository/unidata-releases"

licenses := Seq(("CC0", url("http://creativecommons.org/publicdomain/zero/1.0")))
