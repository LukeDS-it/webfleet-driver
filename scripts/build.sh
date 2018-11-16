#!/bin/bash
if [ $TRAVIS_BRANCH = "master" ] && [ $TRAVIS_PULL_REQUEST = "false"]; then
    git checkout master
    sbt 'release with-defaults'
    git remote add origin-release https://${GITHUB_TOKEN}@github.com/LukeDS-it/webfleet-driver.git > /dev/null
    git push --quiet --set-upstream origin-release master
else
    sbt testAll
fi