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
                       prevBuildStatus: String,
                       prevBuildNumber: Int,
                       prevCommitSha: String)

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

  def getEnvVariable(key: String): Option[String] = sys.env.get(key)

  def getDroneEnvVariableOrDie[T](key: String, conversion: String => T): T = {
    getEnvVariable(key)
      .map(conversion)
      .getOrElse(sys.error(Feedback.undefinedEnvironmentVariable(key)))
  }

  lazy val droneSettings = Seq(
    insideDrone := getEnvVariable("DRONE").exists(_.toBoolean),
    droneEnvironment := {
      if (!insideDrone.value) None
      else {
        val repositoryInfo = for {
          ciRepo <- getEnvVariable("DRONE_REPO")
          ciRepoOwner <- getEnvVariable("DRONE_REPO_OWNER")
          ciRepoName <- getEnvVariable("DRONE_REPO_NAME")
          ciRepoScm <- getEnvVariable("DRONE_REPO_SCM")
          ciRepoLink <- getEnvVariable("DRONE_REPO_LINK")
          ciRepoAvatar <- getEnvVariable("DRONE_REPO_AVATAR")
          ciRepoBranch <- getEnvVariable("DRONE_REPO_BRANCH")
          ciRepoPrivate <- getEnvVariable("DRONE_REPO_PRIVATE").map(
            _.toBoolean)
          ciRepoTrusted <- getEnvVariable("DRONE_REPO_TRUSTED").map(
            _.toBoolean)
        } yield
          RepositoryInfo(ciRepo,
                         ciRepoOwner,
                         ciRepoName,
                         ciRepoScm,
                         ciRepoLink,
                         ciRepoAvatar,
                         ciRepoBranch,
                         ciRepoPrivate,
                         ciRepoTrusted)

        val buildInfo = for {
          ciBuildNumber <- getEnvVariable("DRONE_BUILD_NUMBER").map(_.toInt)
          ciBuildEvent <- getEnvVariable("DRONE_BUILD_EVENT")
          ciBuildStatus <- getEnvVariable("DRONE_BUILD_STATUS")
          ciBuildLink <- getEnvVariable("DRONE_BUILD_LINK")
          ciBuildCreated <- getEnvVariable("DRONE_BUILD_CREATED")
          ciBuildStarted <- getEnvVariable("DRONE_BUILD_STARTED")
          ciBuildFinished <- getEnvVariable("DRONE_BUILD_FINISHED")
          ciPrevBuildStatus <- getEnvVariable("DRONE_PREV_BUILD_STATUS")
          ciPrevBuildNumber <- getEnvVariable("DRONE_PREV_BUILD_NUMBER").map(
            _.toInt)
          ciPrevCommitSha <- getEnvVariable("DRONE_PREV_COMMIT_SHA")
        } yield
          BuildInfo(
            ciBuildNumber,
            ciBuildEvent,
            ciBuildStatus,
            ciBuildLink,
            ciBuildCreated,
            ciBuildStarted,
            ciBuildFinished,
            ciPrevBuildStatus,
            ciPrevBuildNumber,
            ciPrevCommitSha
          )

        val commitInfo = for {
          ciCommitSha <- getEnvVariable("DRONE_COMMIT_SHA")
          ciCommitRef <- getEnvVariable("DRONE_COMMIT_REF")
          ciCommitBranch <- getEnvVariable("DRONE_COMMIT_BRANCH")
          ciCommitLink <- getEnvVariable("DRONE_COMMIT_LINK")
          ciCommitMessage <- getEnvVariable("DRONE_COMMIT_MESSAGE")
          ciAuthor <- getEnvVariable("DRONE_COMMIT_AUTHOR")
          ciAuthorEmail <- getEnvVariable("DRONE_COMMIT_AUTHOR_EMAIL")
          ciAuthorAvatar <- getEnvVariable("DRONE_COMMIT_AUTHOR_AVATAR")
        } yield
          CommitInfo(ciCommitSha,
                     ciCommitRef,
                     ciCommitBranch,
                     ciCommitLink,
                     ciCommitMessage,
                     AuthorInfo(ciAuthor, ciAuthorEmail, ciAuthorAvatar))

        for {
          ciDroneArch <- getEnvVariable("DRONE_ARCH")
          ciRepositoryInfo <- repositoryInfo
          ciCommitInfo <- commitInfo
          ciBuildInfo <- buildInfo
          ciRemoteUrl <- getEnvVariable("DRONE_REMOTE_URL")
        } yield
          CIEnvironment(
            file(DefaultDroneWorkspace),
            ciDroneArch,
            ciRepositoryInfo,
            ciCommitInfo,
            ciBuildInfo,
            ciRemoteUrl,
            getEnvVariable("DRONE_PULL_REQUEST").map(_.toInt),
            getEnvVariable("DRONE_TAG")
          )
      }
    }
  )
}
