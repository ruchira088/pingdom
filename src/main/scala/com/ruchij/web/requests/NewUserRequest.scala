package com.ruchij.web.requests

import com.ruchij.daos.user.models.Email
import com.ruchij.services.user.models.Password

case class NewUserRequest(firstName: String, lastName: String, email: Email, password: Password)
