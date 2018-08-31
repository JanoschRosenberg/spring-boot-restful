package org.jetbrains.kotlin.demo

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "pic not found")
class PicNotFoundException (override var message:String): RuntimeException(message) {
}