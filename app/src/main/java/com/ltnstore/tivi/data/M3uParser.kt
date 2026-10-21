package com.ltnstore.titvi.data

import com.ltnstore.titvi.model.Channel
import java.text.Normalizer
import java.util.Locale

/**
 * Parser M3U + lọc đúng 19 kênh mục tiêu theo yêu cầu.
 * So khớp mờ: chuẩn hoá Unicode, uppercase, bỏ khoảng trắng thừa.
 */
object M3uParser {

    // Danh sách 19 kênh mục tiêu, đúng thứ tự số kênh 1..19
    val TARGET_CHANNELS: List<String> = listOf(
        "VĨNH LONG 1",
        "VĨNH LONG 2",
        "VĨNH LONG 3",
        "VĨNH LONG 4",
        "HTV 1",
        "HTV 7",
        "HTV 9",
        "AN GIANG 1",
        "AN GIANG 2",
        "CÀ MAU",
        "CẦN THƠ 1",
        "CẦN THƠ 2",
        "CẦN THƠ 3",
        "ĐỒNG THÁP 1",
        "TÂY NINH 1",
        "VTV 3",
        "VTV 6",
        "SCTV 9",
        "DISCOVERY ASIA"
    )

    /** Chuẩn hoá tên để so khớp: bỏ dấu tiếng Việt, uppercase, gộp khoảng trắng. */
    private fun normalize(s: String): String {
        val noAccent = Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return noAccent
            .uppercase(Locale.ROOT)
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    // Map normalized-name -> số kênh (1..19) để tra cứu nhanh
    private val nameToNumber: Map<String, Int> = TARGET_CHANNELS
        .mapIndexed { index, name -> normalize(name) to (index + 1) }
        .toMap()

    /**
     * Parse nội dung M3U, trả về danh sách Channel khớp 19 kênh mục tiêu,
     * đã sắp xếp theo số kênh tăng dần.
     */
    fun parse(m3uContent: String): List<Channel> {
        val result = mutableListOf<Channel>()
        val lines = m3uContent.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        var pendingName: String? = null
        var pendingLogo: String? = null

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                // Ưu tiên tvg-name nếu có, nếu không lấy phần sau dấu phẩy cuối
                pendingName = extractAttr(line, "tvg-name") ?: extractDisplayName(line)
                pendingLogo = extractAttr(line, "tvg-logo") ?: ""
            } else if (!line.startsWith("#") && line.startsWith("http", ignoreCase = true)) {
                val rawName = pendingName
                if (!rawName.isNullOrBlank()) {
                    val key = normalize(rawName)
                    val number = nameToNumber[key]
                        ?: findFuzzy(rawName)?.let { nameToNumber[normalize(it)] }
                    if (number != null) {
                        result.removeAll { it.number == number } // tránh trùng, giữ bản ghi đầu tiên
                        result.add(Channel(number = number, name = TARGET_CHANNELS[number - 1],
                            url = line, logo = pendingLogo.orEmpty()))
                    }
                }
                pendingName = null
                pendingLogo = null
            }
            i++
        }

        // Sắp xếp theo số kênh 1..19, bỏ kênh thiếu
        return result.sortedBy { it.number }
    }

    /** Tìm tên gần đúng nếu normalize không khớp tuyệt đối (ví dụ có tiền tố "Kênh "). */
    private fun findFuzzy(rawName: String): String? {
        val n = normalize(rawName)
        return TARGET_CHANNELS.firstOrNull { target ->
            val t = normalize(target)
            n == t || n.endsWith(t) || n.contains(t)
        }
    }

    private fun extractAttr(line: String, attr: String): String? {
        val regex = Regex("""$attr\s*=\s*"([^"]*)"""", RegexOption.IGNORE_CASE)
        return regex.find(line)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
    }

    private fun extractDisplayName(line: String): String? {
        val idx = line.lastIndexOf(',')
        if (idx < 0 || idx == line.length - 1) return null
        return line.substring(idx + 1).trim().takeIf { it.isNotBlank() }
    }
}