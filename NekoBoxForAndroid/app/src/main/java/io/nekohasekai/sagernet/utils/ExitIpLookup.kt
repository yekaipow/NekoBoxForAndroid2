package io.nekohasekai.sagernet.utils

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.ktx.Logs
import libcore.Libcore
import moe.matsuri.nb4a.utils.Util

/**
 * 查询当前代理的真实出口 IP 与归属地。
 *
 * 走的是 [Libcore.newHttpClient] + `trySocks5(mixedPort)`：先尝试本地 socks 端口
 * （也就是当前节点），失败才退回直连。和订阅更新用的是同一套请求方式。
 *
 * 只查「真实出口」——即服务端看到的源地址，和节点名里写的国家无关，
 * 可以用来验证订阅商有没有标错。
 */
object ExitIpLookup {

    private val gson = Gson()

    data class Result(
        val ip: String,
        val countryCode: String?,   // 两位大写，如 "IT"
        val country: String?,       // 国家名，如 "Italy"
        val city: String?,
        val org: String?,           // ISP / ASN 组织
    ) {
        /** "🇮🇹 IT · 80.174.118.90"，缺字段时自动省略 */
        val display: String
            get() {
                val flag = GeoFlags.emoji(countryCode)
                val head = listOfNotNull(
                    flag.takeIf { it.isNotEmpty() },
                    countryCode?.takeIf { it.isNotBlank() }
                ).joinToString(" ")
                return if (head.isEmpty()) ip else "$head · $ip"
            }

        /** 更完整的描述，用于弹窗 */
        val detail: String
            get() = listOfNotNull(
                country?.takeIf { it.isNotBlank() },
                city?.takeIf { it.isNotBlank() },
                org?.takeIf { it.isNotBlank() },
            ).joinToString(" · ").ifBlank { "-" }
    }

    // api.ip.sb 的字段
    private data class IpSbBean(
        val ip: String? = null,
        val country: String? = null,
        @SerializedName("country_code") val countryCode: String? = null,
        val city: String? = null,
        val isp: String? = null,
        val organization: String? = null,
    )

    // ipinfo.io 的字段（作为兜底源）
    private data class IpInfoBean(
        val ip: String? = null,
        val country: String? = null,
        val city: String? = null,
        val org: String? = null,
    )

    /**
     * 同步查询（调用方自己放到后台线程）。失败返回 null，不抛异常。
     * 注：Libcore 的 httpClient 不支持单独设超时，超时由调用方外层协程控制。
     */
    fun query(): Result? {
        // 主源：api.ip.sb，字段最全（含 ISP/ASN），实测不限流
        queryOnce("https://api.ip.sb/geoip")?.let { body ->
            runCatching { gson.fromJson(body, IpSbBean::class.java) }
                .getOrNull()
                ?.takeIf { !it.ip.isNullOrBlank() }
                ?.let { bean ->
                    return Result(
                        ip = bean.ip!!,
                        countryCode = bean.countryCode?.uppercase(),
                        country = bean.country,
                        city = bean.city,
                        org = bean.isp ?: bean.organization,
                    )
                }
        }

        // 兜底：ipinfo.io（可能被限流，所以放第二位）
        queryOnce("https://ipinfo.io/json")?.let { body ->
            runCatching { gson.fromJson(body, IpInfoBean::class.java) }
                .getOrNull()
                ?.takeIf { !it.ip.isNullOrBlank() }
                ?.let { bean ->
                    return Result(
                        ip = bean.ip!!,
                        countryCode = bean.country?.uppercase(),
                        country = null,
                        city = bean.city,
                        org = bean.org,
                    )
                }
        }

        return null
    }

    private fun queryOnce(url: String): String? {
        return try {
            val response = Libcore.newHttpClient().apply {
                // 先走本地 socks（当前节点），连不上再直连
                trySocks5(DataStore.mixedPort)
                tryH3Direct()
            }.newRequest().apply {
                if (DataStore.allowInsecureOnRequest) {
                    allowInsecure()
                }
                setURL(url)
                setUserAgent("NekoBox")
            }.execute()

            Util.getStringBox(response.contentString)?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Logs.w("ExitIpLookup $url failed: ${e.message}")
            null
        }
    }
}
