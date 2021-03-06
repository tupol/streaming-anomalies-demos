name := "streaming-anomalies-demos"

organization := "org.tupol"

scalaVersion := "2.11.12"

val sparkUtilsVersion = "0.4.1-SNAPSHOT"

val onlineStatsVersion = "0.0.2-SNAPSHOT"

val sparkVersion = "2.3.2"

// ------------------------------
// DEPENDENCIES AND RESOLVERS

updateOptions := updateOptions.value.withCachedResolution(true)
resolvers += "Sonatype OSS Staging" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

lazy val providedDependencies = Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion force(),
  "org.apache.spark" %% "spark-sql" % sparkVersion force(),
  "org.apache.spark" %% "spark-mllib" % sparkVersion force(),
  "org.apache.spark" %% "spark-streaming" % sparkVersion force()
)

libraryDependencies ++= providedDependencies.map(_ % "provided")

lazy val excludeJars = ExclusionRule(organization = "net.jpountz.lz4", name = "lz4")

libraryDependencies ++= Seq(
  "org.tupol" %% "online-stats" % onlineStatsVersion,
  "org.apache.spark" %% "spark-sql-kafka-0-10" % sparkVersion,
  "org.apache.kafka" % "kafka_2.11" % "0.10.0.1",
  "org.apache.kafka" % "kafka-clients" % "0.10.0.1",
  "org.tupol" %% "spark-utils" % sparkUtilsVersion excludeAll(excludeJars),
  "org.tupol" %% "spark-utils" % sparkUtilsVersion % "test" classifier "tests" excludeAll(excludeJars),
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "org.scalacheck" %% "scalacheck" % "1.14.0" % "test",
  "net.manub" %% "scalatest-embedded-kafka" % "0.14.0" % "test" excludeAll(excludeJars),
  "org.apache.spark" %% "spark-sql-kafka-0-10" % sparkVersion % "test" excludeAll(excludeJars)
)

// ------------------------------
// TESTING
parallelExecution in Test := false

fork in Test := true

publishArtifact in Test := true

// ------------------------------
// TEST COVERAGE

scoverage.ScoverageKeys.coverageExcludedPackages := "org.apache.spark.ml.param.shared.*"
scoverage.ScoverageKeys.coverageExcludedFiles := ".*BuildInfo.*"

// ------------------------------
// PUBLISHING
isSnapshot := version.value.trim.endsWith("SNAPSHOT")

useGpg := true

// Nexus (see https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html)
publishTo := {
  val repo = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at repo + "content/repositories/snapshots")
  else
    Some("releases" at repo + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := true

publishMavenStyle := true

pomIncludeRepository := { _ => false }

licenses := Seq("MIT-style" -> url("https://opensource.org/licenses/MIT"))

homepage := Some(url("https://github.com/tupol/spark-tools"))

scmInfo := Some(
  ScmInfo(
    url("https://github.com/tupol/spark-tools.git"),
    "scm:git@github.com:tupol/spark-tools.git"
  )
)

developers := List(
  Developer(
    id    = "tupol",
    name  = "Oliver Tupran",
    email = "olivertupran@yahoo.com",
    url   = url("https://github.com/tupol")
  )
)

releasePublishArtifactsAction := PgpKeys.publishSigned.value
import ReleaseTransformations._
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,          // performs the initial git checks
  tagRelease,
  releaseStepCommand(s"""sonatypeOpen "${organization.value}" "${name.value} v${version.value}""""),
  releaseStepCommand("publishSigned"),
  releaseStepCommand("sonatypeRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges                     // also checks that an upstream branch is properly configured
)

// ------------------------------
// ASSEMBLY
assemblyJarName in assembly := s"${name.value}-assembly.jar"

// Add exclusions, provided...
assemblyMergeStrategy in assembly := {
  {
    case PathList("META-INF", xs@_*) => MergeStrategy.discard
    case "log4j.properties" => MergeStrategy.discard
    case x => MergeStrategy.first
  }
}

artifact in (Compile, assembly) := {
  val art = (artifact in (Compile, assembly)).value
  art.copy(`classifier` = Some("assembly"))
}

addArtifact(artifact in(Compile, assembly), assembly)

// Skip test in `assembly` and encompassing publish(Local) tasks.
test in assembly := {}

// ------------------------------
// BUILD-INFO
lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "org.tupol.spark.anomalies.info"
  )

buildInfoKeys ++= Seq[BuildInfoKey](
  resolvers,
  libraryDependencies in Test,
  BuildInfoKey.map(name) { case (k, v) => "project" + k.capitalize -> v.capitalize },
  BuildInfoKey.action("buildTime") {
    System.currentTimeMillis
  } // re-computed each time at compile
)

buildInfoOptions += BuildInfoOption.BuildTime
buildInfoOptions += BuildInfoOption.ToMap
buildInfoOptions += BuildInfoOption.ToJson
