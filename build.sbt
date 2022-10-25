import sbt.Tests.{Group, SubProcess}
import scoverage.ScoverageKeys
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.SbtAutoBuildPlugin

lazy val compileDeps = Seq(
  ws,
  "uk.gov.hmrc"        %% "bootstrap-backend-play-28" % "7.8.0",
  "com.kenshoo"        %% "metrics-play"              % "2.7.3_0.8.2",
  ws
)

def testDeps(scope: String) =
  Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % "7.8.0"     % scope,
    "org.scalatest"          %% "scalatest"               % "3.2.14"    % scope,
    "org.scalatestplus"      %% "mockito-3-4"             % "3.2.10.0"  % scope,
    "com.vladsch.flexmark"    % "flexmark-all"            % "0.62.2"    % scope,
    "org.scalatestplus.play" %% "scalatestplus-play"      % "5.1.0"     % scope,
    "com.github.tomakehurst"  % "wiremock-standalone"     % "2.26.3"    % scope
  )

lazy val root = (project in file("."))
  .settings(
    name := "national-duty-repayment-center",
    organization := "uk.gov.hmrc",
    scalaVersion := "2.13.10",
    PlayKeys.playDefaultPort := 8451,
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*filters.*;.*handlers.*;.*components.*;.*repositories.*;" +
      ".*BuildInfo.*;.*javascript.*;.*Routes.*;.*GuiceInjector;" +
      ".*ControllerConfiguration",
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    libraryDependencies ++= compileDeps ++ testDeps("test") ++ testDeps("it"),
    publishingSettings,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    scalacOptions += "-Wconf:src=routes/.*:s"
  )
  .configs(IntegrationTest)
  .settings(
    // To resolve a bug with version 2.x.x of the scoverage plugin - https://github.com/sbt/sbt/issues/6997
    libraryDependencySchemes ++= Seq("org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always)
  )
  .settings(
    IntegrationTest / Keys.fork := false,
    Defaults.itSettings,
    IntegrationTest / unmanagedSourceDirectories += baseDirectory(_ / "it").value,
    IntegrationTest / parallelExecution := false,
    IntegrationTest / testGrouping := oneForkedJvmPerTest((IntegrationTest / definedTests).value),
    majorVersion := 0
  )
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
  tests.map { test =>
    new Group(test.name, Seq(test), SubProcess(ForkOptions().withRunJVMOptions(Vector(s"-Dtest.name=${test.name}"))))
  }
