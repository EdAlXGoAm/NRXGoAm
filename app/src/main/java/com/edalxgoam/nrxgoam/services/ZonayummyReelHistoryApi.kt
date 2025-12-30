package com.edalxgoam.nrxgoam.services

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class ZyFolder(
    val id: String,
    val name: String,
    val color: String? = null,
    val icon: String? = null,
    val parentId: String? = null,
    val order: Int = 0,
)

data class ZyLabel(
    val id: String,
    val name: String,
    val color: String,
    val usageCount: Int = 0,
)

data class ZyHistoryItem(
    val id: String, // downloadId
    val platform: String,
    val url: String,
    val blobPath: String,
    val blobUrl: String? = null,
    val downloadUrlWithSas: String? = null,
    val contentType: String? = null,
    val size: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long? = null,
    val folderId: String? = null,
    val labelIds: List<String> = emptyList(),
    val description: String? = null,
    val title: String? = null,
    val thumbnailPath: String? = null,
    val thumbnailUrlWithSas: String? = null,
)

data class ZyListLinksResult(
    val items: List<ZyHistoryItem>,
    val expiryTime: Long? = null,
)

data class ZyUploadUrlResult(
    val downloadId: String,
    val blobPath: String,
    val blobUrl: String,
    val uploadUrlWithSas: String,
    val downloadUrlWithSas: String,
    val expiryTime: Long,
    val contentType: String,
    val platform: String,
    val url: String,
)

object ZonayummyReelHistoryApi {
    private const val BASE_URL = "https://functions.zonayummy.com/api"

    sealed class ApiResult<out T> {
        data class Ok<T>(val value: T) : ApiResult<T>()
        data class Err(val message: String) : ApiResult<Nothing>()
    }

    fun listUserLinks(token: String): ApiResult<ZyListLinksResult> {
        val conn = (URL("$BASE_URL/reel-downloader/user-links").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 25000
            readTimeout = 25000
        }
        return readJson(conn) { json ->
            val itemsJson = json.optJSONArray("items") ?: JSONArray()
            val items = (0 until itemsJson.length()).mapNotNull { idx ->
                parseItem(itemsJson.optJSONObject(idx))
            }
            ApiResult.Ok(ZyListLinksResult(items = items, expiryTime = json.optLong("expiryTime").takeIf { it > 0 }))
        }
    }

    fun listFolders(token: String): ApiResult<List<ZyFolder>> {
        val conn = (URL("$BASE_URL/reel-downloader/folders").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 25000
            readTimeout = 25000
        }
        return readJson(conn) { json ->
            val arr = json.optJSONArray("folders") ?: JSONArray()
            val folders = (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                ZyFolder(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    color = o.optString("color").takeIf { it.isNotBlank() && it != "null" },
                    icon = o.optString("icon").takeIf { it.isNotBlank() && it != "null" },
                    parentId = o.optString("parentId").takeIf { it.isNotBlank() && it != "null" },
                    order = o.optInt("order", 0),
                ).takeIf { it.id.isNotBlank() }
            }
            ApiResult.Ok(folders)
        }
    }

    fun createFolder(
        token: String,
        name: String,
        icon: String? = null,
        color: String? = null,
        parentId: String? = null,
        order: Int? = null,
    ): ApiResult<ZyFolder> {
        val payload = JSONObject().put("name", name.trim())
        if (!icon.isNullOrBlank()) payload.put("icon", icon)
        if (!color.isNullOrBlank()) payload.put("color", color)
        if (!parentId.isNullOrBlank()) payload.put("parentId", parentId)
        if (order != null) payload.put("order", order)

        val conn = (URL("$BASE_URL/reel-downloader/folders").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            doInput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 25000
            readTimeout = 25000
        }
        return readJson(conn, payload) { json ->
            if (!json.optBoolean("ok", true)) {
                return@readJson ApiResult.Err(json.optString("error", "No se pudo crear la carpeta"))
            }
            val folder = json.optJSONObject("folder") ?: return@readJson ApiResult.Err("Respuesta inválida (folder)")
            ApiResult.Ok(parseFolder(folder) ?: return@readJson ApiResult.Err("Respuesta inválida (folder)"))
        }
    }

    fun deleteFolder(token: String, folderId: String): ApiResult<List<String>> {
        val safeId = URLEncoder.encode(folderId, "UTF-8")
        val conn = (URL("$BASE_URL/reel-downloader/folders/$safeId").openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            doInput = true
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 25000
            readTimeout = 25000
        }
        return readJson(conn) { json ->
            if (!json.optBoolean("ok", true)) {
                return@readJson ApiResult.Err(json.optString("error", "No se pudo eliminar la carpeta"))
            }
            val arr = json.optJSONArray("deletedIds") ?: JSONArray()
            val ids = (0 until arr.length()).mapNotNull { i ->
                arr.optString(i).takeIf { it.isNotBlank() }
            }
            ApiResult.Ok(ids)
        }
    }

    fun listLabels(token: String): ApiResult<List<ZyLabel>> {
        val conn = (URL("$BASE_URL/reel-downloader/labels").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 25000
            readTimeout = 25000
        }
        return readJson(conn) { json ->
            val arr = json.optJSONArray("labels") ?: JSONArray()
            val labels = (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                ZyLabel(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    color = o.optString("color", "#BAE1FF"),
                    usageCount = o.optInt("usageCount", 0),
                ).takeIf { it.id.isNotBlank() }
            }
            ApiResult.Ok(labels)
        }
    }

    fun createLabels(token: String, names: List<String>): ApiResult<List<ZyLabel>> {
        val clean = names.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (clean.isEmpty()) return ApiResult.Ok(emptyList())

        val payload = JSONObject().put("names", JSONArray(clean))
        val conn = (URL("$BASE_URL/reel-downloader/labels").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            doInput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 25000
            readTimeout = 25000
        }
        return readJson(conn, payload) { json ->
            val arr = json.optJSONArray("labels") ?: JSONArray()
            val labels = (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                ZyLabel(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    color = o.optString("color", "#BAE1FF"),
                    usageCount = o.optInt("usageCount", 0),
                ).takeIf { it.id.isNotBlank() }
            }
            ApiResult.Ok(labels)
        }
    }

    fun getUploadUrl(token: String, platform: String, url: String, contentType: String): ApiResult<ZyUploadUrlResult> {
        val payload = JSONObject()
            .put("platform", platform)
            .put("url", url)
            .put("contentType", contentType)

        val conn = (URL("$BASE_URL/reel-downloader/user-links/get-upload-url").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            doInput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 25000
            readTimeout = 25000
        }
        return readJson(conn, payload) { json ->
            if (!json.optBoolean("ok", true)) {
                return@readJson ApiResult.Err(json.optString("error", "Error obteniendo uploadUrl"))
            }
            ApiResult.Ok(
                ZyUploadUrlResult(
                    downloadId = json.optString("downloadId"),
                    blobPath = json.optString("blobPath"),
                    blobUrl = json.optString("blobUrl"),
                    uploadUrlWithSas = json.optString("uploadUrlWithSas"),
                    downloadUrlWithSas = json.optString("downloadUrlWithSas"),
                    expiryTime = json.optLong("expiryTime"),
                    contentType = json.optString("contentType", contentType),
                    platform = json.optString("platform", platform),
                    url = json.optString("url", url),
                )
            )
        }
    }

    fun createRecord(
        token: String,
        downloadId: String,
        platform: String,
        url: String,
        blobPath: String,
        contentType: String,
        size: Long,
    ): ApiResult<Long> {
        val payload = JSONObject()
            .put("downloadId", downloadId)
            .put("platform", platform)
            .put("url", url)
            .put("blobPath", blobPath)
            .put("contentType", contentType)
            .put("size", size)

        val conn = (URL("$BASE_URL/reel-downloader/user-links").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            doInput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 25000
            readTimeout = 25000
        }
        return readJson(conn, payload) { json ->
            if (!json.optBoolean("ok", true)) {
                return@readJson ApiResult.Err(json.optString("error", "No se pudo crear el registro"))
            }
            ApiResult.Ok(json.optLong("createdAt", 0L))
        }
    }

    fun generateThumbnail(token: String, downloadId: String): ApiResult<Unit> {
        val safeId = URLEncoder.encode(downloadId, "UTF-8")
        val conn = (URL("$BASE_URL/reel-downloader/user-links/$safeId/thumbnail").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doInput = true
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 25000
            readTimeout = 25000
        }
        return readJson(conn) {
            ApiResult.Ok(Unit)
        }
    }

    fun updateItem(
        token: String,
        downloadId: String,
        folderId: String?,
        labelIds: List<String>?,
        description: String?,
        title: String?,
    ): ApiResult<ZyHistoryItem> {
        val payload = JSONObject()
        if (folderId != null) payload.put("folderId", if (folderId.isBlank()) JSONObject.NULL else folderId)
        if (labelIds != null) payload.put("labelIds", JSONArray(labelIds))
        if (description != null) payload.put("description", if (description.isBlank()) JSONObject.NULL else description)
        if (title != null) payload.put("title", if (title.isBlank()) JSONObject.NULL else title)

        val safeId = URLEncoder.encode(downloadId, "UTF-8")
        val conn = (URL("$BASE_URL/reel-downloader/user-links/$safeId").openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            doOutput = true
            doInput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 25000
            readTimeout = 25000
        }
        return readJson(conn, payload) { json ->
            if (!json.optBoolean("ok", true)) {
                return@readJson ApiResult.Err(json.optString("error", "No se pudo actualizar"))
            }
            val item = parseItem(json.optJSONObject("item"))
                ?: return@readJson ApiResult.Err("Respuesta inválida (item)")
            ApiResult.Ok(item)
        }
    }

    fun moveItems(token: String, downloadIds: List<String>, folderId: String?): ApiResult<Int> {
        val payload = JSONObject()
            .put("downloadIds", JSONArray(downloadIds))
            .put("folderId", if (folderId == null) JSONObject.NULL else folderId)

        val conn = (URL("$BASE_URL/reel-downloader/user-links/move").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            doInput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 25000
            readTimeout = 25000
        }
        return readJson(conn, payload) { json ->
            if (!json.optBoolean("ok", true)) {
                return@readJson ApiResult.Err(json.optString("error", "No se pudo mover"))
            }
            ApiResult.Ok(json.optInt("movedCount", 0))
        }
    }

    fun deleteUserLink(token: String, downloadId: String, deleteBlob: Boolean = true): ApiResult<Boolean> {
        val safeId = URLEncoder.encode(downloadId, "UTF-8")
        val deleteBlobParam = if (deleteBlob) "" else "?deleteBlob=false"
        val conn = (URL("$BASE_URL/reel-downloader/user-links/$safeId$deleteBlobParam").openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            doInput = true
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 25000
            readTimeout = 25000
        }
        return readJson(conn) { json ->
            if (!json.optBoolean("ok", true)) {
                return@readJson ApiResult.Err(json.optString("error", "No se pudo eliminar"))
            }
            ApiResult.Ok(json.optBoolean("deleted", true))
        }
    }

    private fun parseItem(o: JSONObject?): ZyHistoryItem? {
        if (o == null) return null
        val id = o.optString("id")
        if (id.isNullOrBlank()) return null

        val labels = mutableListOf<String>()
        val labelArr = o.optJSONArray("labelIds")
        if (labelArr != null) {
            for (i in 0 until labelArr.length()) {
                val v = labelArr.optString(i)
                if (!v.isNullOrBlank()) labels.add(v)
            }
        }

        return ZyHistoryItem(
            id = id,
            platform = o.optString("platform"),
            url = o.optString("url"),
            blobPath = o.optString("blobPath"),
            blobUrl = o.optString("blobUrl").takeIf { it.isNotBlank() && it != "null" },
            downloadUrlWithSas = o.optString("downloadUrlWithSas").takeIf { it.isNotBlank() && it != "null" },
            contentType = o.optString("contentType").takeIf { it.isNotBlank() && it != "null" },
            size = if (o.has("size")) try { o.getLong("size") } catch (_: Exception) { null } else null,
            createdAt = o.optLong("createdAt", 0L),
            updatedAt = if (o.has("updatedAt")) o.optLong("updatedAt").takeIf { it > 0 } else null,
            folderId = o.optString("folderId").takeIf { it.isNotBlank() && it != "null" },
            labelIds = labels,
            description = o.optString("description").takeIf { it.isNotBlank() && it != "null" },
            title = o.optString("title").takeIf { it.isNotBlank() && it != "null" },
            thumbnailPath = o.optString("thumbnailPath").takeIf { it.isNotBlank() && it != "null" },
            thumbnailUrlWithSas = o.optString("thumbnailUrlWithSas").takeIf { it.isNotBlank() && it != "null" },
        )
    }

    private fun parseFolder(o: JSONObject?): ZyFolder? {
        if (o == null) return null
        val id = o.optString("id")
        if (id.isNullOrBlank()) return null
        return ZyFolder(
            id = id,
            name = o.optString("name"),
            color = o.optString("color").takeIf { it.isNotBlank() && it != "null" },
            icon = o.optString("icon").takeIf { it.isNotBlank() && it != "null" },
            parentId = o.optString("parentId").takeIf { it.isNotBlank() && it != "null" },
            order = o.optInt("order", 0),
        )
    }

    private inline fun <T> readJson(
        conn: HttpURLConnection,
        body: JSONObject? = null,
        onOk: (JSONObject) -> ApiResult<T>,
    ): ApiResult<T> {
        return try {
            if (body != null) {
                conn.outputStream.use { os ->
                    os.write(body.toString().toByteArray(Charsets.UTF_8))
                    os.flush()
                }
            }

            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()
                ?.readText()
                ?: ""
            val json = try {
                if (text.isNotBlank()) JSONObject(text) else JSONObject()
            } catch (_: Exception) {
                JSONObject().put("error", text)
            }

            if (code !in 200..299) {
                ApiResult.Err(json.optString("error", "Error (${code})"))
            } else {
                onOk(json)
            }
        } catch (e: Exception) {
            ApiResult.Err(e.message ?: "Error de red")
        } finally {
            conn.disconnect()
        }
    }
}


