name := """web"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

credentials ++= {
  (sys.env.get("OCT_VAULT_SHARED_READ_ARTIFACTORY_USERNAME"), sys.env.get("OCT_VAULT_SHARED_READ_ARTIFACTORY_PASSWORD")) match {
    case (Some(user), Some(token)) => Seq(Credentials("Artifactory Realm", "artifactory.octanner.net",user,token))
    case _ => Seq[Credentials]()
  }
}

resolvers ++= Seq(
  "OCTanner releases" at "https://artifactory.octanner.net/releases/",
  "OCTanner snapshots" at "https://artifactory.octanner.net/snapshots/",
  "OCTanner plugins releases" at "https://artifactory.octanner.net/plugins-releases/",
  "OCTanner plugins snapshots" at "https://artifactory.octanner.net/plugins-snapshots/"
)

libraryDependencies += jdbc
libraryDependencies += cache
libraryDependencies += ws
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
libraryDependencies += "com.octanner.logging" % "logging-scala" % "0.4.5"
libraryDependencies += "com.octanner.platform" %% "service-auth-play" % "1.1.2"

coverageExcludedPackages := "<empty>;Reverse.*;views.*;router.*;database.*"
