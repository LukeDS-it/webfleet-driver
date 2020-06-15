package it.ldsoftware.webfleet.driver.testcontainers

import java.util.Collections

import com.dimafeng.testcontainers.PostgreSQLContainer
import com.typesafe.scalalogging.LazyLogging
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer

class PgsqlContainer(network: Network, enableLog: Boolean = false)
    extends PostgreSQLContainer(
      dockerImageNameOverride = Some("postgres:9.6.17"),
      databaseName = Some("webfleet"),
      pgUsername = Some("webfleet"),
      pgPassword = Some("password")
    )
    with LazyLogging {

  configure { container =>
    container.setNetwork(network)
    container.withNetworkAliases("pgsql")
    if (enableLog) {
      container.withLogConsumer(new Slf4jLogConsumer(logger.underlying))
    }
    container.setExposedPorts(Collections.singletonList(5432))
  }

}
