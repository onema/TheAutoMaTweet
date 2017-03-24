name := "LambdaScala"

version := "1.0"

scalaVersion := "2.12.1"

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

lazy val root = (project in file(".")).
  settings(
    name := "lambda-demo",
    version := "1.0",
    scalaVersion := "2.12.1",
    retrieveManaged := true,
    libraryDependencies += "com.amazonaws" % "aws-lambda-java-core" % "1.1.0",
    libraryDependencies += "com.amazonaws" % "aws-lambda-java-events" % "1.3.0",
    libraryDependencies += "com.amazonaws" % "aws-lambda-java-log4j" % "1.0.0",
    libraryDependencies += "com.amazonaws" % "aws-java-sdk-rekognition" % "1.11.105",
    libraryDependencies += "org.twitter4j" % "twitter4j-core" % "4.0.6"
  )
