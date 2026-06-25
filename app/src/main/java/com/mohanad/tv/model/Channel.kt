package com.mohanad.tv.model

/**
 * يمثل قناة واحدة داخل قائمة M3U / Xtream.
 * كلاس بيانات خفيف بدون أي اعتماديات خارجية (لا Room، لا Gson).
 */
data class Channel(
    val name: String,
    val url: String,
    val logoUrl: String? = null,
    val groupTitle: String = "عام"
)
