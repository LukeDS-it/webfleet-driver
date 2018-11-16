# webfleet-driver [![Build Status](https://travis-ci.com/LukeDS-it/webfleet-driver.svg?branch=master)](https://travis-ci.com/LukeDS-it/jekyll-driver) [ ![Download](https://api.bintray.com/packages/lukeds-it/maven/webfleet-driver-api/images/download.svg) ](https://bintray.com/lukeds-it/maven/webfleet-driver-api/_latestVersion)

This microservice is part of Webfleet, a new distributed cms.

This microservice accepts and validates commands from users and transforms them into events which will be then sent
to a Kafka queue in order to be processed by the other parts of the webfleet ecosystem.

This project also publishes the API library with the contract that the server implements, so that any client
can implement calls with the correct signatures.
Api implementation is left to the client, so that no particular REST library is selected, to give freedom to choose
whatever library best fits the client application.
