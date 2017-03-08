name := "ex"

version := "0.1"

scalaVersion := "2.11.8"

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.1"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.17"

parallelExecution in Test := false

fork := true

resolvers += "Sonatype Public" at "https://oss.sonatype.org/content/groups/public/"

libraryDependencies += "org.scodec" %% "scodec-bits" % "1.1.2"

libraryDependencies += "org.scodec" %% "scodec-core" % "1.10.3"

libraryDependencies += "joda-time" % "joda-time" % "2.9.7"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.3.4"

libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "1.2.0"

libraryDependencies += "com.typesafe" % "config" % "1.2.1"