package com.example.data.network

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import com.example.data.local.CourseEntity
import com.example.data.local.LectureEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ServerHealth(
    val isOnline: Boolean,
    val latencyMs: Long,
    val statusCode: Int,
    val message: String
)

data class AdminStats(
    val totalUsers: Int = 0,
    val activeStreams: Int = 0,
    val totalCourses: Int = 0,
    val storageUsedGb: Double = 0.0,
    val workerStatus: String = "Online"
)

data class Announcement(
    val id: String,
    val title: String,
    val message: String,
    val createdAt: String
)

class KoyebApiClient private constructor() {

    var baseUrl: String = "https://sick-nikaniki-shubham8208989-6d7c863a.koyeb.app"
        private set

    // Shared in-memory and persistent cookie store
    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()
    private var appContext: Context? = null

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(35, TimeUnit.SECONDS)
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                val host = url.host
                val existing = cookieStore.getOrPut(host) { mutableListOf() }
                for (c in cookies) {
                    existing.removeAll { it.name == c.name }
                    existing.add(c)
                }
                syncCookiesToWebView(url.toString(), cookies)
                persistCookies()
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.host] ?: emptyList()
            }
        })
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun initContext(context: Context) {
        appContext = context.applicationContext
        loadPersistedCookies()
    }

    private fun syncCookiesToWebView(urlStr: String, cookies: List<Cookie>) {
        try {
            val cm = CookieManager.getInstance()
            cm.setAcceptCookie(true)
            for (c in cookies) {
                cm.setCookie(urlStr, "${c.name}=${c.value}; Path=/")
            }
            cm.flush()
        } catch (e: Exception) {
            Log.e("KoyebApiClient", "Error syncing cookies to WebView: ${e.message}")
        }
    }

    fun syncAllCookiesToWebView() {
        try {
            val cm = CookieManager.getInstance()
            cm.setAcceptCookie(true)
            val host = baseUrl.toHttpUrlOrNull()?.host ?: return
            val cookies = cookieStore[host] ?: return
            for (c in cookies) {
                cm.setCookie(baseUrl, "${c.name}=${c.value}; Path=/")
            }
            cm.flush()
        } catch (_: Exception) {}
    }

    private fun persistCookies() {
        val ctx = appContext ?: return
        try {
            val prefs = ctx.getSharedPreferences("koyeb_auth_cookies", Context.MODE_PRIVATE)
            val host = baseUrl.toHttpUrlOrNull()?.host ?: return
            val list = cookieStore[host] ?: return
            val sb = StringBuilder()
            for (c in list) {
                sb.append("${c.name}=${c.value};")
            }
            prefs.edit().putString("cookies_$host", sb.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun loadPersistedCookies() {
        val ctx = appContext ?: return
        try {
            val prefs = ctx.getSharedPreferences("koyeb_auth_cookies", Context.MODE_PRIVATE)
            val host = baseUrl.toHttpUrlOrNull()?.host ?: return
            val raw = prefs.getString("cookies_$host", null) ?: return
            val parts = raw.split(";").filter { it.isNotBlank() }
            val existing = cookieStore.getOrPut(host) { mutableListOf() }
            for (p in parts) {
                val kv = p.split("=", limit = 2)
                if (kv.size == 2) {
                    val cookie = Cookie.Builder()
                        .domain(host)
                        .path("/")
                        .name(kv[0].trim())
                        .value(kv[1].trim())
                        .build()
                    existing.removeAll { it.name == cookie.name }
                    existing.add(cookie)
                }
            }
            syncAllCookiesToWebView()
        } catch (_: Exception) {}
    }

    fun getCookieHeader(): String {
        val host = baseUrl.toHttpUrlOrNull()?.host ?: return ""
        val list = cookieStore[host] ?: return ""
        return list.joinToString("; ") { "${it.name}=${it.value}" }
    }

    fun hasActiveSession(): Boolean {
        val host = baseUrl.toHttpUrlOrNull()?.host ?: return false
        val list = cookieStore[host] ?: return false
        return list.any { (it.name == "ak_session" || it.name == "fsb_admin") && it.value.isNotBlank() }
    }

    fun clearSession() {
        val host = baseUrl.toHttpUrlOrNull()?.host
        if (host != null) {
            cookieStore.remove(host)
            appContext?.getSharedPreferences("koyeb_auth_cookies", Context.MODE_PRIVATE)
                ?.edit()?.remove("cookies_$host")?.apply()
            try {
                val cm = CookieManager.getInstance()
                cm.removeAllCookies(null)
                cm.flush()
            } catch (_: Exception) {}
        }
    }

    fun updateBaseUrl(newUrl: String) {
        val trimmed = newUrl.trim().trimEnd('/')
        if (trimmed.isNotEmpty()) {
            baseUrl = trimmed
        }
    }

    suspend fun checkServerHealth(): ServerHealth = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val request = Request.Builder()
                .url("$baseUrl/login")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                ServerHealth(
                    isOnline = response.isSuccessful || response.code == 302,
                    latencyMs = latency,
                    statusCode = response.code,
                    message = if (response.isSuccessful || response.code == 302) "Akshayclasses Live (${latency}ms)" else "HTTP ${response.code}"
                )
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            ServerHealth(
                isOnline = false,
                latencyMs = latency,
                statusCode = -1,
                message = e.localizedMessage ?: "Cannot reach Koyeb server"
            )
        }
    }

    /**
     * Student Login via Password (matching Go handlePasswordLogin)
     */
    suspend fun studentPasswordLogin(usernameOrEmail: String, pass: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("username", usernameOrEmail.trim())
                .add("password", pass)
                .build()

            val request = Request.Builder()
                .url("$baseUrl/login/password")
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = try { JSONObject(bodyStr) } catch (_: Exception) { JSONObject() }
                    val redirect = json.optString("redirect", "/courses")
                    syncAllCookiesToWebView()
                    Result.success(redirect)
                } else {
                    // Try legacy POST /login form
                    val legacyBody = FormBody.Builder()
                        .add("email", usernameOrEmail.trim())
                        .add("password", pass)
                        .build()
                    val legacyReq = Request.Builder()
                        .url("$baseUrl/login")
                        .post(legacyBody)
                        .build()
                    client.newCall(legacyReq).execute().use { legResp ->
                        if (legResp.isSuccessful || legResp.code == 302) {
                            syncAllCookiesToWebView()
                            Result.success("/courses")
                        } else {
                            val errJson = try { JSONObject(bodyStr) } catch (_: Exception) { null }
                            val msg = errJson?.optString("error") ?: "Login failed (HTTP ${response.code})"
                            Result.failure(IOException(msg))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Student OTP Request via Telegram (matching Go sendOTPHandler)
     */
    suspend fun requestTelegramOtp(emailOrPhone: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("email", emailOrPhone.trim())
                .build()

            val request = Request.Builder()
                .url("$baseUrl/login/otp/send")
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful || response.code == 200) {
                    val json = try { JSONObject(bodyStr) } catch (_: Exception) { JSONObject() }
                    val msg = json.optString("message", "OTP sent via Telegram! Please enter 4-digit code.")
                    Result.success(msg)
                } else {
                    val errJson = try { JSONObject(bodyStr) } catch (_: Exception) { null }
                    val err = errJson?.optString("error") ?: "Failed to send OTP (HTTP ${response.code})"
                    Result.failure(IOException(err))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Student OTP Verification (matching Go verifyOTPHandler)
     */
    suspend fun verifyTelegramOtp(emailOrPhone: String, otp: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("email", emailOrPhone.trim())
                .add("otp", otp.trim())
                .build()

            val request = Request.Builder()
                .url("$baseUrl/login/otp/verify")
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = try { JSONObject(bodyStr) } catch (_: Exception) { JSONObject() }
                    syncAllCookiesToWebView()
                    Result.success(json.optString("redirect", "/courses"))
                } else {
                    val errJson = try { JSONObject(bodyStr) } catch (_: Exception) { null }
                    val err = errJson?.optString("error") ?: "Invalid OTP code"
                    Result.failure(IOException(err))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Admin Login (matching Go adminLogin: user, pass)
     */
    suspend fun adminLogin(user: String, pass: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("user", user.trim())
                .add("pass", pass)
                .build()

            val request = Request.Builder()
                .url("$baseUrl/api/admin/login")
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    syncAllCookiesToWebView()
                    Result.success(true)
                } else {
                    val errJson = try { JSONObject(bodyStr) } catch (_: Exception) { null }
                    val errMsg = errJson?.optString("error") ?: "HTTP ${response.code}"
                    Result.failure(IOException(errMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch Admin Courses: GET /api/admin/courses
     */
    suspend fun fetchAdminCourses(): Result<List<CourseEntity>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/admin/courses")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Admin courses failed: HTTP ${response.code}"))
                }
                val bodyStr = response.body?.string() ?: "[]"
                val jsonArr = JSONArray(bodyStr)
                val list = mutableListOf<CourseEntity>()
                for (i in 0 until jsonArr.length()) {
                    val item = jsonArr.getJSONObject(i)
                    val id = item.optString("id", "")
                    val title = item.optString("title", "Untitled Course")
                    val desc = item.optString("description", "")
                    val thumb = item.optString("thumbnail", "")
                    val price = item.optString("price", "")
                    val files = item.optInt("files", 0)

                    list.add(
                        CourseEntity(
                            id = if (id.isNotBlank()) id else "course_$i",
                            title = title,
                            category = if (price.isNotBlank()) "Available ($price)" else "Enrolled",
                            description = desc.ifBlank { "Real Course from Akshayclasses" },
                            instructor = "Akshayclasses Admin",
                            thumbnailUrl = thumb,
                            totalLectures = files,
                            durationText = "$files items",
                            isEnrolled = true
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch Real Student Courses by parsing HTML from /courses
     */
    suspend fun fetchStudentCoursesHtml(): Result<List<CourseEntity>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/courses")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Failed to load /courses (HTTP ${response.code})"))
                }
                val html = response.body?.string() ?: ""
                val list = mutableListOf<CourseEntity>()

                // Regex matches course cards in courses.html:
                // <a class="course-card..." href="/course/([a-zA-Z0-9_-]+)">
                val cardPattern = Pattern.compile("<a[^>]*class=\"course-card([^\"]*)\"[^>]*href=\"/course/([a-zA-Z0-9_-]+)\"[^>]*>(.*?)</a>", Pattern.DOTALL)
                val matcher = cardPattern.matcher(html)

                while (matcher.find()) {
                    val cardClass = matcher.group(1) ?: ""
                    val courseId = matcher.group(2) ?: continue
                    val innerHtml = matcher.group(3) ?: ""

                    val isAvailableOnly = cardClass.contains("available")

                    val titleMatcher = Pattern.compile("<div class=\"course-title\">([^<]+)</div>").matcher(innerHtml)
                    val title = if (titleMatcher.find()) titleMatcher.group(1)?.trim() ?: "Course $courseId" else "Course $courseId"

                    val descMatcher = Pattern.compile("<div class=\"course-desc\">([^<]*)</div>").matcher(innerHtml)
                    val desc = if (descMatcher.find()) descMatcher.group(1)?.trim() ?: "" else ""

                    val thumbMatcher = Pattern.compile("<img class=\"thumb\" src=\"([^\"]+)\"").matcher(innerHtml)
                    val thumb = if (thumbMatcher.find()) thumbMatcher.group(1) ?: "" else ""

                    val countMatcher = Pattern.compile("([0-9]+)\\s+video").matcher(innerHtml)
                    val filesCount = if (countMatcher.find()) countMatcher.group(1)?.toIntOrNull() ?: 0 else 0

                    list.add(
                        CourseEntity(
                            id = courseId,
                            title = title,
                            category = if (isAvailableOnly) "Available" else "Enrolled",
                            description = desc,
                            instructor = "Akshayclasses",
                            thumbnailUrl = thumb,
                            totalLectures = filesCount,
                            durationText = "$filesCount items",
                            isEnrolled = !isAvailableOnly
                        )
                    )
                }

                if (list.isNotEmpty()) {
                    Result.success(list)
                } else {
                    Result.failure(IOException("No courses found or session expired"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch Real Lectures for a Course from Koyeb server:
     * - Parses folder groups or flat course structure
     * - Generates deterministic, stable IDs to prevent duplicate Room rows
     * - Strictly deduplicates items by title and stream token
     */
    suspend fun fetchCourseLectures(courseId: String): Result<List<LectureEntity>> = withContext(Dispatchers.IO) {
        try {
            val rawList = mutableListOf<LectureEntity>()

            // 1. Fetch HTML of /course/$courseId to inspect folders or direct playlist
            val htmlReq = Request.Builder()
                .url("$baseUrl/course/$courseId")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                .get()
                .build()

            val htmlBody = client.newCall(htmlReq).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() ?: "" else ""
            }

            if (htmlBody.isNotBlank()) {
                // Check if page has folder cards: data-folder-id="([^"]+)"
                val folderPattern = Pattern.compile("data-folder-id=\"([a-zA-Z0-9_-]+)\"[^>]*>.*?<div class=\"folder-card-title\">([^<]+)</div>", Pattern.DOTALL)
                val folderMatcher = folderPattern.matcher(htmlBody)
                val folders = mutableListOf<Pair<String, String>>()
                while (folderMatcher.find()) {
                    val fid = folderMatcher.group(1) ?: ""
                    val fname = folderMatcher.group(2)?.trim() ?: "Folder"
                    if (fid.isNotBlank() && !folders.any { it.first == fid }) {
                        folders.add(Pair(fid, fname))
                    }
                }

                // If folders exist, fetch ONLY from folder APIs (avoids duplicating cards)
                if (folders.isNotEmpty()) {
                    var order = 1
                    for ((fid, fname) in folders) {
                        val fApiReq = Request.Builder()
                            .url("$baseUrl/api/course/$courseId/folder/$fid/files")
                            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                            .get()
                            .build()
                        try {
                            client.newCall(fApiReq).execute().use { fResp ->
                                if (fResp.isSuccessful) {
                                    val fBody = fResp.body?.string() ?: ""
                                    val obj = JSONObject(fBody)
                                    val arr = obj.optJSONArray("files") ?: JSONArray()
                                    for (i in 0 until arr.length()) {
                                        val f = arr.getJSONObject(i)
                                        val title = f.optString("title", f.optString("fileName", "Lecture $order")).trim()
                                        val streamUrl = f.optString("streamURL", "")
                                        val token = streamUrl.removePrefix("/watch/").trim()
                                        val mediaType = f.optString("mediaType", "video").uppercase()
                                        val fileSizeStr = f.optString("fileSize", "15 MB")

                                        val cleanTitleKey = title.trim().lowercase()
                                            .replace(Regex("(?i)\\.(mp4|pdf|mkv|mov|avi|jpg|jpeg|png)$"), "")
                                            .replace(Regex("^[0-9]+[.\\s-]+"), "")
                                            .replace(Regex("[^a-z0-9]"), "_")
                                            .trim()

                                        rawList.add(
                                            LectureEntity(
                                                id = "${courseId}_$cleanTitleKey",
                                                courseId = courseId,
                                                folderName = fname,
                                                title = title,
                                                streamToken = token,
                                                streamUrl = if (token.isNotBlank()) "$baseUrl/watch/$token" else "",
                                                durationSeconds = 900,
                                                fileType = when {
                                                    mediaType.contains("PDF") -> "PDF"
                                                    mediaType.contains("PHOTO") || mediaType.contains("IMAGE") -> "IMAGE"
                                                    mediaType.contains("AUDIO") -> "AUDIO"
                                                    else -> "VIDEO"
                                                },
                                                fileSizeMb = parseSizeMb(fileSizeStr),
                                                orderIndex = order++
                                            )
                                        )
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                } else {
                    // Flat mode: parse cards from HTML
                    val cardRegex = Pattern.compile(
                        "<div class=\"card type-([a-z]+)\"[^>]*>.*?<div class=\"file-name\">([^<]+)</div>.*?<a href=\"/watch/([a-zA-Z0-9_-]+)\"",
                        Pattern.DOTALL
                    )
                    val cardMatcher = cardRegex.matcher(htmlBody)
                    var order = 1
                    while (cardMatcher.find()) {
                        val rawType = cardMatcher.group(1) ?: "video"
                        val title = cardMatcher.group(2)?.trim() ?: "Item $order"
                        val token = cardMatcher.group(3)?.trim() ?: ""

                        val cleanTitleKey = title.trim().lowercase()
                            .replace(Regex("(?i)\\.(mp4|pdf|mkv|mov|avi|jpg|jpeg|png)$"), "")
                            .replace(Regex("^[0-9]+[.\\s-]+"), "")
                            .replace(Regex("[^a-z0-9]"), "_")
                            .trim()

                        rawList.add(
                            LectureEntity(
                                id = "${courseId}_$cleanTitleKey",
                                courseId = courseId,
                                folderName = "Course Content",
                                title = title,
                                streamToken = token,
                                streamUrl = if (token.isNotBlank()) "$baseUrl/watch/$token" else "",
                                durationSeconds = 900,
                                fileType = when {
                                    rawType.contains("pdf") -> "PDF"
                                    rawType.contains("photo") || rawType.contains("image") -> "IMAGE"
                                    rawType.contains("audio") -> "AUDIO"
                                    else -> "VIDEO"
                                },
                                fileSizeMb = 35.0f,
                                orderIndex = order++
                            )
                        )
                    }

                    // If flat cards were empty, check ungrouped API
                    if (rawList.isEmpty()) {
                        val apiReq = Request.Builder()
                            .url("$baseUrl/api/course/$courseId/folder/ungrouped/files")
                            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                            .get()
                            .build()
                        try {
                            client.newCall(apiReq).execute().use { resp ->
                                if (resp.isSuccessful) {
                                    val body = resp.body?.string() ?: "{}"
                                    val obj = JSONObject(body)
                                    val filesArr = obj.optJSONArray("files") ?: JSONArray()
                                    for (i in 0 until filesArr.length()) {
                                        val f = filesArr.getJSONObject(i)
                                        val title = f.optString("title", f.optString("fileName", "Item ${i + 1}")).trim()
                                        val streamUrl = f.optString("streamURL", "")
                                        val token = streamUrl.removePrefix("/watch/").trim()
                                        val mediaType = f.optString("mediaType", "video").uppercase()

                                        val cleanTitleKey = title.trim().lowercase()
                                            .replace(Regex("(?i)\\.(mp4|pdf|mkv|mov|avi|jpg|jpeg|png)$"), "")
                                            .replace(Regex("^[0-9]+[.\\s-]+"), "")
                                            .replace(Regex("[^a-z0-9]"), "_")
                                            .trim()

                                        rawList.add(
                                            LectureEntity(
                                                id = "${courseId}_$cleanTitleKey",
                                                courseId = courseId,
                                                folderName = "General",
                                                title = title,
                                                streamToken = token,
                                                streamUrl = if (token.isNotBlank()) "$baseUrl/watch/$token" else "",
                                                durationSeconds = 600,
                                                fileType = when {
                                                    mediaType.contains("PDF") -> "PDF"
                                                    mediaType.contains("PHOTO") || mediaType.contains("IMAGE") -> "IMAGE"
                                                    else -> "VIDEO"
                                                },
                                                fileSizeMb = 25.0f,
                                                orderIndex = i + 1
                                            )
                                        )
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }

            // CRITICAL DEDUPLICATION: eliminate duplicate titles completely!
            val deduplicated = mutableListOf<LectureEntity>()
            val seenKeys = mutableSetOf<String>()
            for (item in rawList) {
                val normKey = item.title.trim().lowercase()
                    .replace(Regex("(?i)\\.(mp4|pdf|mkv|mov|avi|jpg|jpeg|png)$"), "")
                    .replace(Regex("^[0-9]+[.\\s-]+"), "")
                    .trim()
                if (normKey.isNotBlank() && seenKeys.add(normKey)) {
                    deduplicated.add(item.copy(orderIndex = deduplicated.size + 1))
                }
            }

            if (deduplicated.isNotEmpty()) {
                Result.success(deduplicated)
            } else {
                Result.failure(IOException("No lectures found or login required"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refreshes the stream token for a lecture when 403 or token expiration occurs
     */
    suspend fun refreshLectureToken(courseId: String, lectureTitle: String): Result<String> = withContext(Dispatchers.IO) {
        val lecs = fetchCourseLectures(courseId)
        if (lecs.isSuccess) {
            val list = lecs.getOrNull() ?: emptyList()
            val normTarget = lectureTitle.trim().lowercase()
                .replace(Regex("(?i)\\.(mp4|pdf|mkv|mov|avi|jpg|jpeg|png)$"), "")
                .replace(Regex("^[0-9]+[.\\s-]+"), "")
                .trim()

            val match = list.find {
                val cur = it.title.trim().lowercase()
                    .replace(Regex("(?i)\\.(mp4|pdf|mkv|mov|avi|jpg|jpeg|png)$"), "")
                    .replace(Regex("^[0-9]+[.\\s-]+"), "")
                    .trim()
                cur == normTarget || cur.contains(normTarget) || normTarget.contains(cur)
            } ?: list.firstOrNull()

            val tok = match?.streamToken?.ifBlank { match.streamUrl.removePrefix(baseUrl).removePrefix("/watch/").trim() }
            if (!tok.isNullOrBlank()) {
                return@withContext Result.success(tok)
            }
        }
        Result.failure(IOException("Could not refresh stream token"))
    }

    /**
     * Resolves stream URL with nonce from /watch/:token for direct ExoPlayer / VideoView / OkHttp download
     */
    suspend fun resolveDirectStreamUrl(watchToken: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cookieHeader = getCookieHeader()
            val reqBuilder = Request.Builder()
                .url("$baseUrl/watch/$watchToken")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                .header("Referer", "$baseUrl/courses")

            if (cookieHeader.isNotBlank()) {
                reqBuilder.header("Cookie", cookieHeader)
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Watch page returned HTTP ${response.code}"))
                }
                val html = response.body?.string() ?: ""

                // 1. Extract <video id="vid" src="([^"]+)"
                val vidPattern = Pattern.compile("<video[^>]*src=\"([^\"]+)\"", Pattern.DOTALL)
                val m = vidPattern.matcher(html)
                if (m.find()) {
                    val rawSrc = m.group(1) ?: ""
                    val fullUrl = if (rawSrc.startsWith("http")) rawSrc else "$baseUrl$rawSrc"
                    return@withContext Result.success(fullUrl)
                }

                // 2. Extract <img id="preview-img" src="([^"]+)"
                val imgPattern = Pattern.compile("<img[^>]*src=\"([^\"]+)\"", Pattern.DOTALL)
                val im = imgPattern.matcher(html)
                if (im.find()) {
                    val rawSrc = im.group(1) ?: ""
                    val fullUrl = if (rawSrc.startsWith("http")) rawSrc else "$baseUrl$rawSrc"
                    return@withContext Result.success(fullUrl)
                }

                // 3. Extract iframe or generic stream src
                val anySrcPattern = Pattern.compile("(?:src|data-stream)=[\"'](/watch-stream/[^\"']+)[\"']", Pattern.DOTALL)
                val sm = anySrcPattern.matcher(html)
                if (sm.find()) {
                    val rawSrc = sm.group(1) ?: ""
                    return@withContext Result.success("$baseUrl$rawSrc")
                }

                // Fallback to direct watch-stream
                Result.success("$baseUrl/watch-stream/$watchToken")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resolves PDF view URL
     */
    fun buildPdfUrl(tokenOrUrl: String): String {
        val token = tokenOrUrl.removePrefix("/watch/").removePrefix("/pdf-view/").removePrefix("/pdf-download/").trim()
        return if (tokenOrUrl.startsWith("http")) tokenOrUrl else "$baseUrl/pdf-view/$token"
    }

    /**
     * Resolves PDF download URL
     */
    fun buildPdfDownloadUrl(tokenOrUrl: String): String {
        val token = tokenOrUrl.removePrefix("/watch/").removePrefix("/pdf-view/").removePrefix("/pdf-download/").trim()
        return if (tokenOrUrl.startsWith("http")) tokenOrUrl else "$baseUrl/pdf-download/$token"
    }

    /**
     * Resolves Web watch URL
     */
    fun buildWatchUrl(tokenOrUrl: String): String {
        val token = tokenOrUrl.removePrefix("/watch/").trim()
        return if (tokenOrUrl.startsWith("http")) tokenOrUrl else "$baseUrl/watch/$token"
    }

    private fun parseSizeMb(sizeStr: String): Float {
        return try {
            val num = sizeStr.replace(Regex("[^0-9.]"), "").toFloatOrNull() ?: 20.0f
            if (sizeStr.contains("GB", ignoreCase = true)) num * 1024f else num
        } catch (_: Exception) {
            20.0f
        }
    }

    /**
     * Trigger Telegram Channel Sync via Admin API
     */
    suspend fun triggerChannelSync(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val emptyBody = FormBody.Builder().build()
            val request = Request.Builder()
                .url("$baseUrl/api/admin/sync")
                .post(emptyBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Result.success("Telegram channel sync started on Koyeb server!")
                } else {
                    Result.failure(IOException("Sync response HTTP ${response.code}: $bodyStr"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch Admin Stats from /api/admin/stats
     */
    suspend fun fetchAdminStats(): AdminStats = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/admin/stats")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "{}"
                    val obj = JSONObject(body)
                    AdminStats(
                        totalUsers = obj.optInt("users", 0),
                        activeStreams = obj.optInt("streams", 0),
                        totalCourses = obj.optInt("courses", 0),
                        storageUsedGb = obj.optDouble("storage_gb", 0.0),
                        workerStatus = "Live & Connected"
                    )
                } else {
                    AdminStats(workerStatus = "HTTP ${response.code}")
                }
            }
        } catch (e: Exception) {
            AdminStats(workerStatus = "Offline")
        }
    }

    /**
     * Resolves stream URL or watch link
     */
    fun buildStreamUrl(streamTokenOrUrl: String): String {
        return when {
            streamTokenOrUrl.startsWith("http://") || streamTokenOrUrl.startsWith("https://") -> streamTokenOrUrl
            streamTokenOrUrl.startsWith("/watch-stream/") -> "$baseUrl$streamTokenOrUrl"
            streamTokenOrUrl.startsWith("/watch/") -> "$baseUrl$streamTokenOrUrl"
            else -> "$baseUrl/watch-stream/$streamTokenOrUrl"
        }
    }

    companion object {
        val instance: KoyebApiClient by lazy { KoyebApiClient() }
    }
}
