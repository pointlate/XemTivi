package com.ltnstore.titvi.data

import com.ltnstore.titvi.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ChannelRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    private val m3uUrl = "https://raw.githubusercontent.com/vuminhthanh12/vuminhthanh12/refs/heads/main/vmttv"

    /** Tải + parse M3U. Ném exception nếu lỗi mạng. */
    suspend fun loadChannels(): List<Channel> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(m3uUrl).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val body = response.body?.string() ?: error("Empty body")
            M3uParser.parse(body)
        }
    }
}