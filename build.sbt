name := "CogaiPrototype"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= {
  val sparkV = "1.6.0"
  val akkaV = "2.4.4"
  val kafkaV = "0.9.0.1"
  val slickV = "3.1.1"
  val scalatestV = "2.2.6"
  val awsJavaSdkV = "1.10.76"
  val sparkCSV = "1.4.0"
  val scalaMockV = "3.2.2"
  val mysqlConnectorV = "5.1.39"
  val slickJodaMapperVersion = "2.2.0"

  Seq(
    "org.apache.spark" %% "spark-core" % sparkV,
    "org.apache.spark" %% "spark-mllib" % sparkV,
    "org.apache.spark" %% "spark-streaming-kafka" % sparkV,
    "com.databricks" %% "spark-csv" % "1.4.0",
    "org.apache.kafka" %% "kafka" % kafkaV,
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-http-experimental" % akkaV,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaV,
    "com.typesafe.akka" %% "akka-testkit" % akkaV,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaV,
    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaV,
    "org.scalatest"     %% "scalatest"  % scalatestV % "test",
    "com.amazonaws" % "aws-java-sdk" % "1.10.76",
    "commons-io" % "commons-io" % "2.4",
    "org.scalamock" %% "scalamock-scalatest-support" % scalaMockV % "test",
    "com.typesafe.slick" %% "slick" % slickV,
    "com.typesafe.slick" %% "slick-hikaricp" % slickV,
    "mysql" % "mysql-connector-java" % mysqlConnectorV,
    "com.github.tototoshi" %% "slick-joda-mapper" % slickJodaMapperVersion,
    "joda-time" % "joda-time" % "2.7",
    "org.joda" % "joda-convert" % "1.7"
  )
}

dependencyOverrides ++= Set(
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.4"
)

