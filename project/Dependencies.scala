import sbt.*

object Dependencies {

  val circe: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser",
  ).map(_ % Versions.circe)

  val http: Seq[ModuleID] = Seq(
    Libraries.tapirCore,
    Libraries.tapirZIO,
    Libraries.tapirHttp4sZIO,
    Libraries.tapirCirce,
    Libraries.http4sBlazeServer,
    Libraries.sttpClient,
    Libraries.zioSttpClientBackend,
  )

  val domain: Seq[ModuleID] = Seq(
    Libraries.scalatest,
    Libraries.pureconfig,
    Libraries.zio,
    Libraries.zioStream,
    Libraries.zioSlf4j2,
    Libraries.logbackClassic,
    Libraries.logbackEncoder,
    Libraries.redisClients,
  ) ++ circe ++ http

  val zookeeper: Seq[ModuleID] = Seq(
    Libraries.zookeeperApi,
    Libraries.catsCore,
  )

  object Libraries {
    val scalatest    = "org.scalatest"         %% "scalatest"       % Versions.scalatest % Test
    val zookeeperApi = "org.apache.zookeeper"   % "zookeeper"       % Versions.zookeeper
    val zio          = "dev.zio"               %% "zio"             % Versions.zio
    val zioStream    = "dev.zio"               %% "zio-streams"     % Versions.zio
    val catsCore     = "org.typelevel"         %% "cats-core"       % Versions.cats
    val pureconfig   = "com.github.pureconfig" %% "pureconfig-core" % Versions.pureconfig

    val zioSlf4j2      = "dev.zio"             %% "zio-logging-slf4j2"       % Versions.zioSlf4j2
    val logbackClassic = "ch.qos.logback"       % "logback-classic"          % Versions.logbackClassic
    val logbackEncoder = "net.logstash.logback" % "logstash-logback-encoder" % Versions.logbackEncoder

    val tapirCore         = "com.softwaremill.sttp.tapir" %% "tapir-core"              % Versions.tapir
    val tapirZIO          = "com.softwaremill.sttp.tapir" %% "tapir-zio"               % Versions.tapir
    val tapirHttp4sZIO    = "com.softwaremill.sttp.tapir" %% "tapir-http4s-server-zio" % Versions.tapir
    val tapirCirce        = "com.softwaremill.sttp.tapir" %% "tapir-json-circe"        % Versions.tapir
    val http4sBlazeServer = "org.http4s"                  %% "http4s-blaze-server"     % Versions.http4s

    val sttpClient           = "com.softwaremill.sttp.tapir"   %% "tapir-sttp-client"             % Versions.tapir
    val zioSttpClientBackend = "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % Versions.zioSttpClientBackend
    val redisClients         = "redis.clients"                  % "jedis"                         % Versions.redis
  }

  object Versions {
    val scalatest            = "3.2.19"
    val zookeeper            = "3.9.2"
    val zio                  = "2.1.9"
    val cats                 = "2.12.0"
    val pureconfig           = "0.17.7"
    val circe                = "0.14.7"
    val tapir                = "1.10.8"
    val http4s               = "0.23.16"
    val zioSttpClientBackend = "3.10.1"
    val zioSlf4j2            = "2.3.0"
    val logbackClassic       = "1.5.6"
    val logbackEncoder       = "8.0"
    val redis                = "5.1.3"
  }
}
