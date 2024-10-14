import sbt.*

object Dependencies {

  val scalatest      = "org.scalatest"         %% "scalatest"          % Versions.scalatest % Test
  val zookeeperApi   = "org.apache.zookeeper"   % "zookeeper"          % Versions.zookeeper
  val zio            = "dev.zio"               %% "zio"                % Versions.zio
  val zioStream      = "dev.zio"               %% "zio-streams"        % Versions.zio
  val catsCore       = "org.typelevel"         %% "cats-core"          % Versions.cats
  val pureconfig     = "com.github.pureconfig" %% "pureconfig-core"    % Versions.pureconfig
  val scalaZookeeper = "com.loopfor.zookeeper"  % "zookeeper-client_3" % Versions.scalaZookeeper

  val curator = "org.apache.curator" % "curator-x-async" % Versions.curator

  val domain: Seq[ModuleID] = Seq(
    scalatest,
    pureconfig,
    zio,
  )

  val zookeeper: Seq[ModuleID] = Seq(
    zookeeperApi,
    scalaZookeeper,
    catsCore,
    curator,
    zioStream,
  )

  object Versions {
    val scalatest      = "3.2.19"
    val zookeeper      = "3.9.2"
    val zio            = "2.1.9"
    val cats           = "2.12.0"
    val pureconfig     = "0.17.7"
    val scalaZookeeper = "1.7.1"
    val curator        = "5.7.0"
  }
}
