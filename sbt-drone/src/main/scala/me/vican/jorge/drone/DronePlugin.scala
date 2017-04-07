package me.vican.jorge.drone

import sbt._
import java.io.File

object DronePlugin extends sbt.AutoPlugin {

  object autoImport extends DroneSettings with DroneModel

  override def trigger: PluginTrigger = allRequirements
  override def projectSettings = DronePluginImplementation.droneSettings
}

trait DroneModel {
  case class RepositoryInfo(fullName: String,
                            owner: String,
                            name: String,
                            scm: String,
                            link: String,
                            avatar: String,
                            branch: String,
                            isPrivate: Boolean,
                            isTrusted: Boolean)

  case class AuthorInfo(author: String, email: String, avatar: String)

  case class CommitInfo(sha: String,
                        ref: String,
                        branch: String,
                        link: String,
                        message: String,
                        author: AuthorInfo)

  case class BuildInfo(number: Int,
                       event: String,
                       status: String,
                       link: String,
                       created: String,
                       started: String,
                       finished: String,
                       prevBuildStatus: Option[String],
                       prevBuildNumber: Option[Int],
                       prevCommitSha: Option[String])

  case class CIEnvironment(rootDir: File,
                           arch: String,
                           repo: RepositoryInfo,
                           commit: CommitInfo,
                           build: BuildInfo,
                           remoteUrl: String,
                           pullRequest: Option[Int],
                           tag: Option[String])
}

trait DroneSettings { self: DroneModel =>
  val insideDrone: SettingKey[Boolean] = settingKey(
    "Checks if CI is executing the build.")
  val droneEnvironment: SettingKey[Option[CIEnvironment]] = settingKey(
    "Get the Drone environment.")
}

object DronePluginImplementation extends DroneModel with DroneSettings {
  val DefaultDroneWorkspace = "/drone"

  object Feedback {
    def undefinedEnvironmentVariable(name: String) =
      s"Undefined environment variable $name."
  }

  def getOptEnv(key: String): Option[String] = sys.env.get(key)

  def getEnvOrDie[T](key: String): String = {
    getOptEnv(key)
      .getOrElse(sys.error(Feedback.undefinedEnvironmentVariable(key)))
  }

  def getEnvOrDie[T](key: String, conversion: String => T): T = {
    getOptEnv(key)
      .map(conversion)
      .getOrElse(sys.error(Feedback.undefinedEnvironmentVariable(key)))
  }

  lazy val droneSettings = Seq(
    insideDrone := getOptEnv("DRONE").exists(_.toBoolean),
    droneEnvironment := {
      if (!insideDrone.value) None
      else {
        val repositoryInfo = RepositoryInfo(
          getEnvOrDie("DRONE_REPO"),
          getEnvOrDie("DRONE_REPO_OWNER"),
          getEnvOrDie("DRONE_REPO_NAME"),
          getEnvOrDie("DRONE_REPO_SCM"),
          getEnvOrDie("DRONE_REPO_LINK"),
          getEnvOrDie("DRONE_REPO_AVATAR"),
          getEnvOrDie("DRONE_REPO_BRANCH"),
          getEnvOrDie("DRONE_REPO_PRIVATE", _.toBoolean),
          getEnvOrDie("DRONE_REPO_TRUSTED", _.toBoolean)
        )

        val buildInfo = BuildInfo(
          getEnvOrDie("DRONE_BUILD_NUMBER", _.toInt),
          getEnvOrDie("DRONE_BUILD_EVENT"),
          getEnvOrDie("DRONE_BUILD_STATUS"),
          getEnvOrDie("DRONE_BUILD_LINK"),
          getEnvOrDie("DRONE_BUILD_CREATED"),
          getEnvOrDie("DRONE_BUILD_STARTED"),
          getEnvOrDie("DRONE_BUILD_FINISHED"),
          getOptEnv("DRONE_PREV_BUILD_STATUS"),
          getOptEnv("DRONE_PREV_BUILD_NUMBER").map(_.toInt),
          getOptEnv("DRONE_PREV_COMMIT_SHA")
        )

        val authorInfo = AuthorInfo(
          getEnvOrDie("DRONE_COMMIT_AUTHOR"),
          getEnvOrDie("DRONE_COMMIT_AUTHOR_EMAIL"),
          getEnvOrDie("DRONE_COMMIT_AUTHOR_AVATAR")
        )

        val commitInfo = CommitInfo(
          getEnvOrDie("DRONE_COMMIT_SHA"),
          getEnvOrDie("DRONE_COMMIT_REF"),
          getEnvOrDie("DRONE_COMMIT_BRANCH"),
          getEnvOrDie("DRONE_COMMIT_LINK"),
          getEnvOrDie("DRONE_COMMIT_MESSAGE"),
          authorInfo
        )

        Some(
          CIEnvironment(
            file(DefaultDroneWorkspace),
            getEnvOrDie("DRONE_ARCH"),
            repositoryInfo,
            commitInfo,
            buildInfo,
            getEnvOrDie("DRONE_REMOTE_URL"),
            getOptEnv("DRONE_PULL_REQUEST").map(_.toInt),
            getOptEnv("DRONE_TAG")
          ))
      }
    }
  )
}
