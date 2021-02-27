package com.ruchij.core.exceptions

case class ResourceConflictException(errorMessage: String) extends Exception(errorMessage)
