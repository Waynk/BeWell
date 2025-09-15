package com.example.jenmix.hu

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class ThirdActivity : AppCompatActivity() {

    private var receivedFileUri: Uri? = null  // 來自分享的檔案 URI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 不需要設定 layout，專門處理 CSV 檔案分享上傳
        handleIncomingFile()
    }

    private fun handleIncomingFile() {
        if (intent?.action == Intent.ACTION_SEND && intent.type != null) {
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (uri != null) {
                receivedFileUri = uri
                val fileName = getFileName(uri)

                // 檢查副檔名是否為 .csv
                if (!fileName.lowercase().endsWith(".csv")) {
                    Toast.makeText(this, "收到的檔案非 CSV 格式：$fileName", Toast.LENGTH_LONG).show()
                    finish()
                    return
                }

                // 從 SharedPreferences 拿 username
                val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                val username = sharedPref.getString("username", null)
                if (username == null) {
                    Toast.makeText(this, "找不到使用者帳號，無法上傳", Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }

                uploadFile(uri, username)
            } else {
                Toast.makeText(this, "未接收到檔案", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            val type = intent?.type ?: "null"
            val action = intent?.action ?: "null"
            Toast.makeText(this, "未接收到正確的 CSV 檔案 (action: $action, type: $type)", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // 上傳 CSV 檔案
    private fun uploadFile(fileUri: Uri, username: String) {
        val file = uriToFile(fileUri)
        if (file == null) {
            finish()
            return
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "csvFile",
                file.name,
                RequestBody.create("text/csv".toMediaTypeOrNull(), file)
            )
            .addFormDataPart("username", username)  // 傳 username 給後端
            .build()

        val serverUrl = "https://test-9wne.onrender.com/upload" // 你的伺服器路由

        val request = Request.Builder()
            .url(serverUrl)
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ThirdActivity, "上傳失敗: ${e.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ThirdActivity, "檔案上傳成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ThirdActivity, "伺服器錯誤，請稍後再試", Toast.LENGTH_SHORT).show()
                    }
                    finish()
                }
            }
        })
    }

    // 將 URI 轉換為 File
    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
                ?: throw FileNotFoundException("無法開啟檔案 InputStream")

            val file = File(cacheDir, getFileName(uri))
            val outputStream = FileOutputStream(file)

            inputStream.use { it.copyTo(outputStream) }
            outputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "處理檔案失敗: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
    }

    // 擷取檔名
    private fun getFileName(uri: Uri): String {
        var name = "uploaded.csv"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }
}