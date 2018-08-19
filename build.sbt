import Dependencies._

name := "scala-splash"
organization := "is.rud"
version := "0.1.0-SNAPSHOT"
licenses := Seq("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))

developers := List(
  Developer("hrbrmstr", "boB Rudis", "@hrbrmstr", url("https://github.com/hrbrmstr"))
)

organizationName := "Bob Rudis"
organizationHomepage := Option(url("https://rud.is/"))
homepage := scmInfo.value map(_.browseUrl)

scmInfo := Option(
  ScmInfo(url("https://github.com/hrbrmstr/scala-splash"), "git@github.com:hrbrmstr/scala-splash.git")
)

publishMavenStyle := true

scalaVersion := "2.12.6"

libraryDependencies += scalaTest % Test
libraryDependencies += "com.lihaoyi" %% "requests" % "0.1.3"
libraryDependencies += "com.lihaoyi" %% "ujson" % "0.6.6"
libraryDependencies += "com.github.scopt" %% "scopt" % "3.7.0"

publishArtifact in Test := false

retrieveManaged := true

enablePlugins(PackPlugin)

packJarNameConvention := "default"

autoAPIMappings := true

target in Compile in doc := baseDirectory.value / "docs"
 scalacOptions in Compile ++= Seq("-doc-root-content", "rootdoc.txt")

