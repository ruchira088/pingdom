package com.ruchij.core.services.hash

import cats.effect.{ContextShift, Sync}
import com.ruchij.core.types.CustomBlocker.CpuBlocker
import org.mindrot.jbcrypt.BCrypt

class BCryptPasswordHashingService[F[_]: Sync: ContextShift](cpuBlocker: CpuBlocker) extends PasswordHashingService[F] {

  override def hash(input: String): F[String] =
    cpuBlocker.delay {
      BCrypt.hashpw(input, BCrypt.gensalt())
    }

  override def checkPassword(input: String, saltedPasswordHash: String): F[Boolean] =
    cpuBlocker.delay {
      BCrypt.checkpw(input, saltedPasswordHash)
    }

}
