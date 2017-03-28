# sbt-drone

[![Build Status](https://platform-ci.scala-lang.org/api/badges/jvican/sbt-drone/status.svg)](https://platform-ci.scala-lang.org/jvican/sbt-drone)
[ ![Bintray download](https://api.bintray.com/packages/jvican/sbt-plugins/sbt-drone/images/download.svg) ](https://bintray.com/jvican/sbt-plugins/sbt-drone/_latestVersion)

Simple sbt plugin that exposes the Drone CI environment variables in sbt.
  
It currently supports [Drone 0.5](http://readme.drone.io/0.5/).

## Installation

Add the following line to your `plugins.sbt` file:

```scala
addSbtPlugin("me.vican.jorge" % "sbt-drone" % "0.1.0")
```

## Use

This gives you access to two settings:

```scala
  val insideDrone: SettingKey[Boolean] = settingKey("Checks if CI is executing the build.")
  val droneEnvironment: SettingKey[Option[CIEnvironment]] = settingKey("Get the Drone environment.")
```

These settings are scoped for every project.
  
1. `insideDrone.value` will tell you whether you're running it in the CI.
2. `droneEnvironment.value` will allow you to get information on:
    * Drone build information (event, status, creation date, etc).
    * Author information (name, email, avatar).
    * Commit information (sha, branch, link, message, author, etc).
    * Repository information (full name, owner, etc).
    
To see the list of all the exposed environment variables, see the [Drone 0.5 documentation](http://readme.drone.io/0.5/usage/environment-reference/).
