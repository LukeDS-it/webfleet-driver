package it.ldsoftware.webfleet.driver.testcontainers

import java.util.Collections

import com.dimafeng.testcontainers.KafkaContainer
import com.typesafe.scalalogging.LazyLogging
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

}
