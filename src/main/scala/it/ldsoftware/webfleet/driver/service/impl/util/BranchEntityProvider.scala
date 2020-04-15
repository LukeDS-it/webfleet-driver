package it.ldsoftware.webfleet.driver.service.impl.util

import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import it.ldsoftware.webfleet.driver.actors.BranchContent.{BranchCommand, BranchKey}

class BranchEntityProvider(clusterSharding: ClusterSharding) extends EntityProvider[BranchCommand] {

  override def get(name: String): EntityRef[BranchCommand] =
    clusterSharding.entityRefFor(BranchKey, name)

}
