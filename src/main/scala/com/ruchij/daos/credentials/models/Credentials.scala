package com.ruchij.daos.credentials.models

import org.joda.time.DateTime

case class Credentials(userId: String, createdAt: DateTime, modifiedAt: DateTime, saltedPasswordHash: String)
