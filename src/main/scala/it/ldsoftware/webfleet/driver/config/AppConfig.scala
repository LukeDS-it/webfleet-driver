package it.ldsoftware.webfleet.driver.config

import java.time.Duration
import java.util.Properties

import akka.actor.typed.ActorSystem
import akka.kafka.ConsumerSettings
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import it.ldsoftware.webfleet.driver.config.AppConfig._
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer

import scala.concurrent.duration._

// $COVERAGE-OFF$
case class AppConfig(private val config: Config) extends LazyLogging {

  lazy val serverPort: Int = config.appInt("server.port")

  lazy val timeout: Duration = config.appDuration("timeout")

  lazy val kafkaServers: String = config.appString("kafka.broker-list")

  lazy val jwtConfig: JwtConfig = JwtConfig(
    config.appString("auth0.audience"),
    config.appString("auth0.domain"),
    config.appString("auth0.issuer")
  )

  lazy val kafkaUser: String = config.appString("kafka.user")

  lazy val kafkaPass: String = config.appString("kafka.pass")

  lazy val jaasCfg: String =
    s"""org.apache.kafka.common.security.scram.ScramLoginModule required username="$kafkaUser" password="$kafkaPass";"""

  lazy val producerProperties: Properties = {
    val props = new Properties

    props.put("bootstrap.servers", kafkaServers)
    props.put("client.id", "webfleet-driver")
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    if (config.appBoolean("kafka.sasl")) {
      props.put("security.protocol", "SASL_SSL")
      props.put("sasl.mechanism", "SCRAM-SHA-256")
      props.put("sasl.jaas.config", jaasCfg)
    }

    props
  }

  lazy val contentTopic: String = config.appString("kafka.topics.contents")

  lazy val domainsTopic: String = config.appString("kafka.topics.domains")

  lazy val wfDomainsUrl: String = config.appString("domains-url")

  def getConfig: Config = config

  def consumerSettings(system: ActorSystem[_]): ConsumerSettings[String, String] = {
    val base = ConsumerSettings[String, String](
      system.classicSystem,
      new StringDeserializer,
      new StringDeserializer
    ).withBootstrapServers(kafkaServers)
      .withGroupId("webfleet-driver")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
      .withStopTimeout(0.seconds)

    if (!config.appBoolean("kafka.sasl")) base else {
      base
        .withProperty("security.protocol", "SASL_SSL")
        .withProperty("sasl.mechanism", "SCRAM-SHA-256")
        .withProperty("sasl.jaas.config", jaasCfg)
    }
  }

}

case object AppConfig {
  val PropsPath = "it.ldsoftware.webfleet.driver"

  implicit class ConfigOps(config: Config) {
    def appInt(path: String): Int = config.getInt(s"$PropsPath.$path")
    def appString(path: String): String = config.getString(s"$PropsPath.$path")
    def appBoolean(path: String): Boolean = config.getBoolean(s"$PropsPath.$path")
    def appDuration(path: String): Duration = config.getDuration(s"$PropsPath.$path")
  }

  case class JwtConfig(audience: String, domain: String, issuer: String)
}

// $COVERAGE-ON$
