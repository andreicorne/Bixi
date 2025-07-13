package com.example.bixi.models

import java.util.UUID

data class CheckItem(
//    val id: String = UUID.randomUUID().toString(),
    var text: String,
    var done: Boolean
){
}