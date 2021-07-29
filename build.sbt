import sbt.Tests.{Group, SubProcess}
import scoverage.ScoverageKeys
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.SbtAutoBuildPlugin

lazy val compileDeps = Seq(
  ws,
  "uk.gov.hmrc"        %% "bootstrap-backend-play-28" % "5.3.0",
  "com.kenshoo"        %% "metrics-play"              % "2.6.19_0.7.0",
  "com.github.blemale" %% "scaffeine"                 % "3.1.0",
  "org.typelevel"      %% "cats-core"                 % "2.2.0",
  ws
)

def testDeps(scope: String) =
  Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % "5.3.0"          % scope,
    "org.scalatest"          %% "scalatest"               % "3.2.9"          % scope,
    "org.scalatestplus"      %% "mockito-3-4"            % "3.2.9.0"         % scope,
    "com.vladsch.flexmark"    % "flexmark-all"            % "0.36.8"         % scope,
    "org.scalatestplus.play" %% "scalatestplus-play"      % "5.1.0"          % scope,
    "com.github.tomakehurst"  % "wiremock-jre8"           % "2.26.3"         % scope,
    "com.typesafe.akka"      %% "akka-testkit"            % "2.6.10"         % scope
  )

lazy val root = (project in file("."))
  .settings(
    name := "national-duty-repayment-center",
    organization := "uk.gov.hmrc",
    scalaVersion := "2.12.12",
    PlayKeys.playDefaultPort := 8451,
    resolvers += "third-party-maven-releases" at "https://artefacts.tax.service.gov.uk/artifactory/third-party-maven-releases/",
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*filters.*;.*handlers.*;.*components.*;.*repositories.*;" +
      ".*BuildInfo.*;.*javascript.*;.*Routes.*;.*GuiceInjector;" +
      ".*ControllerConfiguration",
    ScoverageKeys.coverageMinimum := 46.43,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    libraryDependencies ++= compileDeps ++ testDeps("test") ++ testDeps("it"),
    publishingSettings,
    unmanagedResourceDirectories in Compile += baseDirectory.value / "resources",
    routesImport ++= Seq("uk.gov.hmrc.nationaldutyrepaymentcenter.binders.UrlBinders._")
  )
  .configs(IntegrationTest)
  .settings(
    Keys.fork in IntegrationTest := false,
    Defaults.itSettings,
    unmanagedSourceDirectories in IntegrationTest += baseDirectory(_ / "it").value,
    parallelExecution in IntegrationTest := false,
    testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
    majorVersion := 0
  )
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
  tests.map { test =>
    new Group(test.name, Seq(test), SubProcess(ForkOptions().withRunJVMOptions(Vector(s"-Dtest.name=${test.name}"))))
  }
