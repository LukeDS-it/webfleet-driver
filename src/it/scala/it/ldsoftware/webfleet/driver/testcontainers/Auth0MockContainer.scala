package it.ldsoftware.webfleet.driver.testcontainers

import com.typesafe.scalalogging.LazyLogging
import org.testcontainers.containers.MockServerContainer
import org.testcontainers.containers.output.Slf4jLogConsumer

class Auth0MockContainer extends MockServerContainer with LazyLogging {
  getLogConsumers.add(new Slf4jLogConsumer(logger.underlying))
}
