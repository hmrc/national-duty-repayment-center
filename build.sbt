import sbt.Tests.{Group, SubProcess}
import scoverage.ScoverageKeys
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.DefaultBuildSettings

val bootstrapVersion = "8.4.0"
val playVersion      = 30

lazy val compileDeps = Seq(
  ws,
  "uk.gov.hmrc" %% s"bootstrap-backend-play-$playVersion" % bootstrapVersion
)

def testDeps: Seq[ModuleID] =
  Seq(
    "uk.gov.hmrc"            %% s"bootstrap-test-play-$playVersion" % bootstrapVersion,
    "org.scalatest"          %% "scalatest"                         % "3.2.15",
    "org.scalatestplus"      %% "mockito-3-4"                       % "3.2.10.0",
    "com.vladsch.flexmark"    % "flexmark-all"                      % "0.64.6",
    "org.scalatestplus.play" %% "scalatestplus-play"                % "5.1.0"
  ).map(_ % Test)

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name                     := "national-duty-repayment-center",
    organization             := "uk.gov.hmrc",
    PlayKeys.playDefaultPort := 8451,
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*filters.*;.*handlers.*;.*components.*;.*repositories.*;" +
      ".*BuildInfo.*;.*javascript.*;.*Routes.*;.*GuiceInjector;" +
      ".*ControllerConfiguration",
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum    := true,
    ScoverageKeys.coverageHighlighting     := true,
    libraryDependencies ++= compileDeps ++ testDeps,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    scalacOptions += "-Wconf:src=routes/.*:s"
  )
  .enablePlugins(PlayScala,SbtDistributablesPlugin)
  .settings(scalafmtOnCompile := true)

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(
    root % "test->test"
  ) // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= testDeps)
