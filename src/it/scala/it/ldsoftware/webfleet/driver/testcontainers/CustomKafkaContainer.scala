package it.ldsoftware.webfleet.driver.testcontainers

import java.util.{Collections, Properties}

import com.dimafeng.testcontainers.KafkaContainer
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer

class CustomKafkaContainer(network: Network, enableLog: Boolean = false)
    extends KafkaContainer
    with LazyLogging {

  configure { container =>
    container.setNetwork(network)
    container.withNetworkAliases("kafka")
    if (enableLog) {
      container.withLogConsumer(new Slf4jLogConsumer(logger.underlying))
    }
    container.setExposedPorts(Collections.singletonList(9093))
  }

  private lazy val props = {
    val tmp = new Properties

    tmp.put("bootstrap.servers", s"http://localhost:${mappedPort(9093)}")
    tmp.put("client.id", "webfleet-driver")
    tmp.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    tmp.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    tmp
  }

  lazy val kafkaProducer = new KafkaProducer[String, String](props)

  def send(record: ProducerRecord[String, String]): Unit = kafkaProducer.send(record).get

  def consumerFor(topic: String): KafkaConsumer[String, String] = {
    val props: Properties = new Properties()
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.put("bootstrap.servers", s"http://localhost:${mappedPort(9093)}")
    props.put("group.id", "webfleet-test")
    props.put("enable.auto.commit", "true")

    val kafkaConsumer = new KafkaConsumer[String, String](props)
    kafkaConsumer.subscribe(Collections.singletonList(topic))

    kafkaConsumer
  }

}
