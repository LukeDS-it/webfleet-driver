package it.ldsoftware.webfleet.driver.testcontainers

import com.dimafeng.testcontainers.PostgreSQLContainer
import com.typesafe.scalalogging.LazyLogging
import org.testcontainers.containers.{BindMode, Network}
import org.testcontainers.containers.output.Slf4jLogConsumer

class PgsqlContainer(network: Network)
    extends PostgreSQLContainer(
      dockerImageNameOverride = Some("postgres:9.6.17"),
      databaseName = Some("webfleet"),
      pgUsername = Some("webfleet"),
      pgPassword = Some("password")
    )
    with LazyLogging {

  configure { container =>
    container.setNetwork(network)
    container.withClasspathResourceMapping("init.sql", "/docker-entrypoint-initdb.d/database.sql", BindMode.READ_ONLY)
    container.withNetworkAliases("pgsql")
    container.withLogConsumer(new Slf4jLogConsumer(logger.underlying))
  }

}
