package com.ruchij.api.web.requests

import com.ruchij.core.daos.user.models.Email
import com.ruchij.core.services.user.models.Password

case class NewUserRequest(firstName: String, lastName: String, email: Email, password: Password)
