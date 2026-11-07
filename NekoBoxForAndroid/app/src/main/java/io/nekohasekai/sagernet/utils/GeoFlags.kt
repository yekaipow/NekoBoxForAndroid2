package io.nekohasekai.sagernet.utils

/**
 * 国家码 -> emoji 国旗。
 *
 * 原理是 ISO 3166-1 alpha-2 到 Regional Indicator Symbols 的映射：
 * 'A'..'Z' 依次对应 U+1F1E6..U+1F1FF，两个字母拼起来就是国旗。
 * 不依赖任何图片资源，但需要系统字体支持（Android 6+ 的 Noto Color Emoji 都有）。
 * 万一设备缺字形会显示成两个方形字母，此时调用方应退回只显示国家码。
 */
object GeoFlags {

    /** 有国旗字形的常见国家；不在这里的按非国家处理（如 private / tor / netflix） */
    private val NON_COUNTRY = setOf(
        "private", "tor", "cloudflare", "cloudfront", "fastly",
        "google", "facebook", "twitter", "telegram", "netflix", "lan", ""
    )

    fun emoji(countryCode: String?): String {
        val cc = normalize(countryCode) ?: return ""
        val base = 0x1F1E6
        val first = base + (cc[0] - 'A')
        val second = base + (cc[1] - 'A')
        return String(Character.toChars(first)) + String(Character.toChars(second))
    }

    /** 规范化成两位大写字母；不是合法国家码返回 null */
    fun normalize(countryCode: String?): String? {
        val cc = countryCode?.trim()?.uppercase() ?: return null
        if (cc.length != 2) return null
        if (cc.lowercase() in NON_COUNTRY) return null
        if (cc[0] !in 'A'..'Z' || cc[1] !in 'A'..'Z') return null
        return cc
    }

    /** "🇮🇹 IT"，无国旗时退回 "IT"，都没有则空串 */
    fun label(countryCode: String?, fallbackName: String? = null): String {
        val cc = normalize(countryCode)
        if (cc == null) return fallbackName?.takeIf { it.isNotBlank() } ?: ""
        val flag = emoji(cc)
        return if (flag.isEmpty()) cc else "$flag $cc"
    }
}
