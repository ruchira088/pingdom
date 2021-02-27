package com.ruchij.core.daos.account.models

import org.joda.time.DateTime

case class Account(id: String, createdAt: DateTime, modifiedAt: DateTime, name: String)
