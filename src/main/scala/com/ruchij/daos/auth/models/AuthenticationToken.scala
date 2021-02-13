package com.ruchij.daos.auth.models

import org.joda.time.DateTime

case class AuthenticationToken(issuedAt: DateTime, expiresAt: DateTime, userId: String, secret: String, renewals: Long)
