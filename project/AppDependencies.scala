import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-27"  % "2.25.0",
    "org.typelevel"           %% "cats-core"                  % "2.2.0"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-27"   % "2.25.0"                % Test,
    "com.typesafe.play"       %% "play-test"                % current                 % Test,
    "com.vladsch.flexmark"    %  "flexmark-all"             % "0.35.10"               % "test, it",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "3.1.3"                 % "test, it",
    "com.github.tomakehurst"  %  "wiremock-standalone"      % "2.25.0"                %"test, it",
    "org.pegdown"             %  "pegdown"                  % "1.6.0"                 % "test,it",
    "org.mockito"             %  "mockito-all"              % "1.10.19"               % Test,
    "uk.gov.hmrc"             %% "hmrctest"                 % "3.9.0-play-26"         %"test,it",
    "uk.gov.hmrc"             %% "reactivemongo-test"       % "4.21.0-play-26"        %"test,it"
  )

}
