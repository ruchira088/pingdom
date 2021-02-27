package com.ruchij.api.daos.permission.models

import enumeratum.{Enum, EnumEntry}

sealed trait PermissionType extends EnumEntry

object PermissionType extends Enum[PermissionType] {
  case object Administrator extends PermissionType
  case object ReadWrite extends PermissionType
  case object ReadOnly extends PermissionType

  override def values: IndexedSeq[PermissionType] = findValues
}