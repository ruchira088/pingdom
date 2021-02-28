package com.ruchij.core.test.mixins

import cats.effect.IO

trait IOSupport {
  def run[A](block: => IO[A]): A =
    block.unsafeRunSync()
}
