package com.ltnstore.titvi.model

data class Channel(
    val number: Int,          // số kênh (1..19) dùng cho phím remote
    val name: String,         // tên hiển thị (VĨNH LONG 1, HTV 1, ...)
    val url: String,          // URL phát trực tuyến
    val logo: String = ""     // URL logo (tuỳ chọn)
)