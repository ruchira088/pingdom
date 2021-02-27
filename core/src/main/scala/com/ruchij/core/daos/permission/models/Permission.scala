package com.ruchij.core.daos.permission.models

import org.joda.time.DateTime

case class Permission(
  createdAt: DateTime,
  modifiedAt: DateTime,
  userId: String,
  accountId: String,
  permissionType: PermissionType,
  grantedBy: Option[String]
)
