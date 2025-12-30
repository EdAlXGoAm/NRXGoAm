package com.edalxgoam.nrxgoam.services

import android.os.Build
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ZonayummyAuthApi {
    private const val BASE_URL = "https://functions.zonayummy.com/api"

    sealed class LoginResult {
        data class Success(val token: String, val username: String) : LoginResult()
        data class Error(val message: String, val useGoogleAuth: Boolean = false) : LoginResult()
    }

    suspend fun loginWithUsername(
        contextDeviceId: String,
        username: String,
        password: String,
    ): LoginResult = postLogin(
        path = "/auth/username/login",
        payload = JSONObject()
            .put("username", username)
            .put("password", password)
            .put("deviceId", contextDeviceId)
            .put("userAgent", defaultUserAgent())
    )

    suspend fun loginWithEmail(
        contextDeviceId: String,
        email: String,
        password: String,
    ): LoginResult = postLogin(
        path = "/auth/email/login",
        payload = JSONObject()
            .put("email", email)
            .put("password", password)
            .put("deviceId", contextDeviceId)
            .put("userAgent", defaultUserAgent())
    )

    suspend fun loginWithPhone(
        contextDeviceId: String,
        phone: String,
        password: String,
    ): LoginResult = postLogin(
        path = "/auth/phone/login",
        payload = JSONObject()
            .put("phone", phone)
            .put("password", password)
            .put("deviceId", contextDeviceId)
            .put("userAgent", defaultUserAgent())
    )

    private fun defaultUserAgent(): String {
        return "NRXGoAm/${Build.MODEL} Android/${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
    }

    private suspend fun postLogin(path: String, payload: JSONObject): LoginResult {
        val conn = (URL(BASE_URL + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            doInput = true
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 25000
            readTimeout = 25000
        }

        return try {
            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val code = conn.responseCode
            val bodyText = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()
                ?.readText()
                ?: ""

            val json = try {
                if (bodyText.isNotBlank()) JSONObject(bodyText) else JSONObject()
            } catch (_: Exception) {
                JSONObject().put("error", bodyText)
            }

            if (code !in 200..299) {
                return LoginResult.Error(
                    message = json.optString("error", "Error (${code})"),
                    useGoogleAuth = json.optBoolean("useGoogleAuth", false)
                )
            }

            if (!json.optBoolean("ok", true) || json.optString("token").isNullOrBlank()) {
                return LoginResult.Error(json.optString("error", "Respuesta inválida del servidor"))
            }

            LoginResult.Success(
                token = json.getString("token"),
                username = json.optString("username", "Usuario")
            )
        } catch (e: Exception) {
            LoginResult.Error(e.message ?: "Error de red")
        } finally {
            conn.disconnect()
        }
    }
}


