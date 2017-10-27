import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import sbt.Resolver

import scala.util.Properties

name := """alexa-skills-poc"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.2"

credentials ++= Seq(
  Credentials(
    "Artifactory Realm",
    "artifactory.octanner.net",
    Properties.envOrElse("OCT_VAULT_SHARED_READ_ARTIFACTORY_USERNAME", "readonly"),
    Properties.envOrElse("OCT_VAULT_SHARED_READ_ARTIFACTORY_PASSWORD", "AP5voKzeXmojG2TFgyHu4J46iuA"))
)

resolvers ++= Seq(
  "OCTanner releases" at "https://artifactory.octanner.net/releases/",
  "OCTanner snapshots" at "https://artifactory.octanner.net/snapshots/",
  "OCTanner plugins releases" at "https://artifactory.octanner.net/plugins-releases/",
  "OCTanner plugins snapshots" at "https://artifactory.octanner.net/plugins-snapshots/",
  Resolver.mavenLocal
)

coverageMinimum := 80
coverageFailOnMinimum := true

libraryDependencies += jdbc
libraryDependencies += evolutions
libraryDependencies += ehcache
libraryDependencies += ws
libraryDependencies += guice
libraryDependencies += "com.typesafe.play" %% "anorm" % "2.5.3"
libraryDependencies += "org.postgresql" % "postgresql" % "9.4.1208"
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.0.+" % Test
libraryDependencies += "com.octanner" % "auth-oc_tanner" % "1.4.4"
libraryDependencies += "com.octanner.platform" %% "service-auth-play" % "1.2.+"
libraryDependencies += "com.octanner" %% "ws-tracer-client-play" % "0.0.1"
libraryDependencies += "com.octanner.platform" %% "service-auth-play-test" % "1.2.+" % Test
libraryDependencies += "io.swagger" %% "swagger-play2" % "1.6.+"
coverageExcludedPackages := "<empty>;Reverse.*;views.*;router.*;database.*"

val preferences =
  ScalariformKeys.preferences := ScalariformKeys.preferences.value
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(AlignParameters, true)
      .setPreference(DoubleIndentConstructorArguments, true)
      .setPreference(DanglingCloseParenthesis, Preserve)

SbtScalariform.scalariformSettings ++ Seq(preferences)

javaOptions in Test ++= Seq("-Dconfig.file=conf/application.test.conf")
