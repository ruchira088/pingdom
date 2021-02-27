package com.ruchij.core.daos.user.models

import org.joda.time.DateTime

case class User(
  id: String,
  createdAt: DateTime,
  modifiedAt: DateTime,
  accountId: String,
  firstName: String,
  lastName: String,
  email: Email
)
