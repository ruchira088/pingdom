package com.ruchij.api.daos.authentication.models

import org.joda.time.DateTime

case class AuthenticationToken(issuedAt: DateTime, expiresAt: DateTime, userId: String, secret: String, renewals: Long)
