package com.ruchij.core.types

import cats.effect.Blocker

import scala.language.implicitConversions

sealed trait CustomBlocker {

  val blocker: Blocker

}

object CustomBlocker {

  case class IOBlocker(blocker: Blocker) extends CustomBlocker

  case class CpuBlocker(blocker: Blocker) extends CustomBlocker

  implicit def toBlocker(customBlocker: CustomBlocker): Blocker = customBlocker.blocker
}
