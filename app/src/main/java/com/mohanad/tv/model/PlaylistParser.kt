package com.mohanad.tv.model

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

/**
 * محلل قوائم M3U / M3U8 يدوي بالكامل (بدون مكتبات خارجية).
 * يدعم أيضاً روابط Xtream Codes العادية بصيغة get.php القياسية لـ M3U.
 */
object PlaylistParser {

    private const val CONNECT_TIMEOUT_MS = 15000
    private const val READ_TIMEOUT_MS = 20000

    /**
     * يحمّل ويحلل قائمة القنوات من رابط معطى.
     * يُنفَّذ هذا التابع دوماً على Thread خلفي (انظر MainActivity).
     */
    @Throws(Exception::class)
    fun fetch(rawUrl: String): List<Channel> {
        val finalUrl = normalizeUrl(rawUrl)
        val content = downloadText(finalUrl)
        return parseM3U(content)
    }

    /**
     * يطبّع الرابط المُدخل: يضيف http:// إن لزم، ويتركه كما هو إن كان
     * بصيغة get.php أو m3u جاهزة.
     */
    private fun normalizeUrl(input: String): String {
        var url = input.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        return url
    }

    private fun downloadText(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "MohanadTV/1.0")
        connection.instanceFollowRedirects = true

        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw Exception("HTTP $responseCode")
            }
            val reader = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8))
            val builder = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                builder.append(line).append('\n')
            }
            reader.close()
            return builder.toString()
        } finally {
            connection.disconnect()
        }
    }

    private val EXTINF_PATTERN: Pattern = Pattern.compile(
        "#EXTINF:-?\\d+(?:\\s+[^,]*)?,(.*)"
    )
    private val ATTR_PATTERN: Pattern = Pattern.compile(
        "([a-zA-Z0-9_-]+)=\"([^\"]*)\""
    )

    /**
     * تحليل نص M3U خطوة بخطوة.
     * صيغة عامة:
     * #EXTM3U
     * #EXTINF:-1 tvg-logo="..." group-title="...",اسم القناة
     * http://رابط_البث
     */
    fun parseM3U(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()

        var pendingName: String? = null
        var pendingLogo: String? = null
        var pendingGroup: String = "عام"

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            if (line.startsWith("#EXTINF")) {
                val matcher = EXTINF_PATTERN.matcher(line)
                pendingName = if (matcher.find()) {
                    matcher.group(1)?.trim().orEmpty().ifEmpty { "بدون اسم" }
                } else {
                    "بدون اسم"
                }

                // استخراج tvg-logo و group-title من نفس السطر
                val attrMatcher = ATTR_PATTERN.matcher(line)
                pendingLogo = null
                pendingGroup = "عام"
                while (attrMatcher.find()) {
                    val key = attrMatcher.group(1)
                    val value = attrMatcher.group(2)
                    when (key) {
                        "tvg-logo" -> pendingLogo = value
                        "group-title" -> if (!value.isNullOrBlank()) pendingGroup = value
                    }
                }
            } else if (!line.startsWith("#")) {
                // هذا سطر رابط البث الفعلي
                if (pendingName != null) {
                    channels.add(
                        Channel(
                            name = pendingName,
                            url = line,
                            logoUrl = pendingLogo,
                            groupTitle = pendingGroup
                        )
                    )
                    pendingName = null
                    pendingLogo = null
                    pendingGroup = "عام"
                }
            }
            // الأسطر التي تبدأ بـ # وليست EXTINF (مثل #EXTM3U, #EXTGRP) يتم تجاوزها بأمان
        }
        return channels
    }
}
