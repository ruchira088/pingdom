package com.ruchij.web.requests

import com.ruchij.daos.user.models.Email

case class AuthenticationRequest(email: Email, password: String)
