package com.ruchij.daos.permission.models

import org.joda.time.DateTime

case class Permission(
  userId: String,
  createdAt: DateTime,
  modifiedAt: DateTime,
  accountId: String,
  permissionType: PermissionType,
  grantedBy: Option[String]
)
