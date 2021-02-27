package com.ruchij.api.web.requests

import com.ruchij.core.daos.user.models.Email

case class AuthenticationRequest(email: Email, password: String)
