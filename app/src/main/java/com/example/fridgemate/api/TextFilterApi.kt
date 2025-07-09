package com.example.fridgemate.api

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object TextFilterApi {

    private const val TAG = "TextFilterApi"

    // FlaskサーバーのIPアドレスとポート（実機と同じネットワークにいる前提）
    private const val SERVER_URL = "http://192.168.50.77:5000/filter_text"

    private val client = OkHttpClient()

    fun filterTextFromOcr(ocrText: String, onResult: (String) -> Unit) {
        val json = JSONObject()
        json.put("text", ocrText)

        val requestBody = json.toString()
            .toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(SERVER_URL)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "通信失敗: ${e.message}", e)
                onResult("通信エラー: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        onResult("エラー: ${response.code}")
                        return
                    }

                    val responseText = response.body?.string()
                    val cleanedText = try {
                        JSONObject(responseText ?: "{}").optString("cleaned_text", "")
                    } catch (e: Exception) {
                        "パースエラー: ${e.message}"
                    }
                    onResult(cleanedText)
                }
            }
        })
    }
}