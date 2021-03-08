package com.ruchij.core.daos.permission.models

import enumeratum.{Enum, EnumEntry}

sealed trait PermissionType extends EnumEntry

object PermissionType extends Enum[PermissionType] {
  case object Administrator extends PermissionType
  case object ReadWrite extends PermissionType
  case object ReadOnly extends PermissionType

  val ordering: List[PermissionType] = List(ReadOnly, ReadWrite, Administrator)

  override def values: IndexedSeq[PermissionType] = findValues
}
