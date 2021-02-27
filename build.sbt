import Dependencies._
import sbtrelease.Git
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.Utilities.stateW

import scala.sys.process.ProcessBuilder

val ReleaseBranch = "dev"
val ProductionBranch = "master"

inThisBuild {
  Seq(
    organization := "com.ruchij",
    scalaVersion := Dependencies.ScalaVersion,
    maintainer := "me@ruchij.com",
    scalacOptions ++= Seq("-Xlint", "-feature", "-Wconf:cat=lint-byname-implicit:s"),
    addCompilerPlugin(kindProjector),
    addCompilerPlugin(betterMonadicFor),
    addCompilerPlugin(scalaTypedHoles)
  )
}

lazy val migrationApp =
  (project in file("./migration-app"))
    .enablePlugins(JavaAppPackaging)
    .settings(
      name := "migration-app",
      topLevelDirectory := None,
      libraryDependencies ++= Seq(catsEffect, flywayCore, postgresql, h2, pureconfig, logbackClassic)
    )

lazy val root =
  (project in file("."))
    .enablePlugins(BuildInfoPlugin, JavaAppPackaging)
    .settings(
      name := "pingdom",
      topLevelDirectory := None,
      libraryDependencies ++= rootDependencies ++ rootTestDependencies.map(_ % Test),
      buildInfoKeys := Seq[BuildInfoKey](name, organization, version, scalaVersion, sbtVersion),
      buildInfoPackage := "com.eed3si9n.ruchij"
    )
    .dependsOn(migrationApp)

lazy val development =
  (project in file("./development"))
    .settings(name := "development", topLevelDirectory := None)
    .dependsOn(root % "compile->test")

lazy val rootDependencies =
  Seq(
    http4sDsl,
    http4sBlazeServer,
    http4sCirce,
    circeGeneric,
    circeParser,
    circeLiteral,
    jodaTime,
    pureconfig,
    enumeratum,
    doobieCore,
    jbcrypt,
    redis4cats,
    logbackClassic
  )

lazy val rootTestDependencies =
  Seq(scalaTest, embeddedRedis, pegdown)

addCommandAlias("testWithCoverage", "; coverage; test; coverageReport")

val verifyReleaseBranch = { state: State =>
  val git = Git.mkVcs(state.extract.get(baseDirectory))
  val branch = git.currentBranch

  if (branch != ReleaseBranch) {
    sys.error {
      s"The release branch is $ReleaseBranch, but the current branch is set to $branch"
    }
  } else state
}

val mergeReleaseToMaster = { state: State =>
  val git = Git.mkVcs(state.extract.get(baseDirectory))

  val (updatedState, releaseTag) = state.extract.runTask(releaseTagName, state)

  updatedState.log.info(s"Merging $releaseTag to $ProductionBranch...")

  val userInput: Option[ProcessBuilder] =
    SimpleReader
      .readLine("Push changes to the remote master branch? (Y/n) ")
      .map(_.toLowerCase) match {
      case Some("y") | Some("") =>
        updatedState.log.info(s"Pushing changes to remote master ($releaseTag)...")
        Some(git.cmd("push"))

      case _ =>
        updatedState.log.warn("Remember to push changes to remote master")
        None
    }

  val actions: List[ProcessBuilder] =
    List(git.cmd("checkout", ProductionBranch), git.cmd("merge", releaseTag)) ++
      userInput ++
      List(git.cmd("checkout", ReleaseBranch))

  actions.reduce(_ #&& _) !!

  updatedState.log.info(s"Successfully merged $releaseTag to $ProductionBranch")

  updatedState
}
releaseProcess := Seq(
  ReleaseStep(verifyReleaseBranch),
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(mergeReleaseToMaster),
  setNextVersion,
  commitNextVersion,
  pushChanges
)
