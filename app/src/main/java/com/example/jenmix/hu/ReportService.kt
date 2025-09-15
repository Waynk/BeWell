package com.example.jenmix.hu




import android.app.IntentService
import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.app.AlarmManager
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileOutputStream
import javax.mail.*
import javax.mail.internet.*
import android.content.Context
import javax.activation.*




import com.google.gson.annotations.SerializedName




// 定義數據類，用於解析伺服器返回的 JSON 數據
data class BloodPressureData(
    val measurementTime: String,
    val systolicPressure: Int,
    val diastolicPressure: Int
)




class ReportService : IntentService("ReportService") {




    override fun onHandleIntent(intent: Intent?) {




        // 获取报表数据并筛选
        val bloodPressureData = fetchBloodPressureDataAndFilter()




        // 生成报表（即使数据为空）
        val reportFile = generateReport(bloodPressureData)




        // 获取用户的 Gmail
        val userGmail = getUserGmail()
        if (!userGmail.isNullOrEmpty()) {
            val subject = "定期健康报告"
            val body = if (bloodPressureData.isEmpty()) {
                "本次报告中没有符合条件的血压数据，附件中仅包含表头。"
            } else {
                "这是您的定期健康报告内容。"
            }
            sendEmail(userGmail, subject, body, reportFile)
        }




        // 保存报表
        saveReport(reportFile)


        val nextReportTime = calculateNextReportTime(getSelectedReportTime(), getReportInterval())
        saveNextReportTime(nextReportTime)




    }






    // 計算下一次報表產出時間
    private fun calculateNextReportTime(currentTime: Long, interval: Long): Long {
        return currentTime + interval
    }


    // 保存下一次報表產出時間
    private fun saveNextReportTime(nextTime: Long) {
        val sharedPreferences = getSharedPreferences("ReportSettings", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putLong("nextReportTime", nextTime)
        editor.apply()
    }




    // 获取并筛选血压数据
    private fun fetchBloodPressureDataAndFilter(): List<BloodPressureData> {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("http://172.20.10.8:3000/getBloodPressureData") // 替换为你的服务器 URL
            .build()




        val filteredData = mutableListOf<BloodPressureData>()




        try {
            val response = client.newCall(request).execute() // 同步请求
            if (response.isSuccessful) {
                val resStr = response.body?.string()
                if (!resStr.isNullOrEmpty()) {
                    Log.d("ReportService", "Response body: $resStr") // 打印响应内容，查看所有数据




                    // 反序列化数据
                    val bloodPressureData = Gson().fromJson(resStr, Array<BloodPressureData>::class.java)




                    val selectedReportTime = getSelectedReportTime()
                    val reportInterval = getReportInterval()




                    val startTime = getStartOfReportPeriod(selectedReportTime)
                    val endTime = getEndOfReportPeriod(selectedReportTime, reportInterval)




                    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("UTC") // 解析为 UTC 时间
                    }




                    val localTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("Asia/Taipei") // 设置台北时区
                    }




                    // 过滤数据并修正时间
                    bloodPressureData.filter { record ->
                        val measurementTime = record.measurementTime
                        Log.d("ReportService", "Checking record with measurementTime: $measurementTime") // 打印每个 measurementTime




                        if (measurementTime.isNullOrEmpty()) {
                            // 如果 measurementTime 为空或 null，跳过此条记录
                            Log.d("ReportService", "Skipping record with invalid measurementTime: $measurementTime")
                            return@filter false
                        }
                        try {
                            // 解析 UTC 时间
                            val utcTime = dateFormat.parse(measurementTime) ?: return@filter false




                            // 不加 8 小时，仅进行时区转换
                            val localTimeStr = localTimeFormat.format(utcTime)




                            Log.d("ReportService", "Adjusted time (Asia/Taipei): $localTimeStr")




                            // 更新记录的测量日期
                            record.measurementTime = localTimeStr




                            // 检查时间是否在有效范围内
                            val localTime = localTimeFormat.parse(localTimeStr)
                            return@filter localTime?.after(startTime) == true && localTime?.before(endTime) == true
                        } catch (e: Exception) {
                            Log.e("ReportService", "Error parsing or adjusting measurementTime: $measurementTime", e)
                            false
                        }
                    }.also {
                        filteredData.addAll(it)
                    }
                } else {
                    Log.e("ReportService", "Response body is null or empty!")
                }
            } else {
                Log.e("ReportService", "GET request failed: ${response.message}")
            }
        } catch (e: Exception) {
            Log.e("ReportService", "Error occurred during request or filtering", e)
        }
        return filteredData
    }




    // 获取用户选择的报告时间
    private fun getSelectedReportTime(): Long {
        val sharedPreferences = getSharedPreferences("ReportSettings", MODE_PRIVATE)
        return sharedPreferences.getLong("nextReportTime", System.currentTimeMillis())
    }




    // 获取当天的 00:00 时间戳
    private fun getStartOfReportPeriod(reportTime: Long): Date {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = reportTime
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }




    // 获取根据用户选择的报告间隔来动态计算的结束时间
    private fun getEndOfReportPeriod(reportTime: Long, interval: Long): Date {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = reportTime
        when (interval) {
            AlarmManager.INTERVAL_DAY -> {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 7)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
            }
            AlarmManager.INTERVAL_DAY * 7 -> {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 7)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
            }
            AlarmManager.INTERVAL_DAY * 30 -> {
                calendar.add(Calendar.MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 7)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
            }
            else -> {
                val customIntervalDays = (interval / AlarmManager.INTERVAL_DAY).toInt()
                calendar.add(Calendar.DAY_OF_MONTH, customIntervalDays)
                calendar.set(Calendar.HOUR_OF_DAY, 7)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
            }
        }
        return calendar.time
    }




    // 生成报表
    private fun generateReport(data: List<BloodPressureData>): File {
        // 创建 Excel 工作簿
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Blood Pressure Report")




        // 添加标题行
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("測量日期")
        headerRow.createCell(1).setCellValue("收縮壓")
        headerRow.createCell(2).setCellValue("舒張壓")
        headerRow.createCell(3).setCellValue("平均收縮壓")
        headerRow.createCell(4).setCellValue("平均舒張壓")
        headerRow.createCell(5).setCellValue("最大收縮壓")
        headerRow.createCell(6).setCellValue("最大舒張壓")
        headerRow.createCell(7).setCellValue("舒張壓標準")
        headerRow.createCell(8).setCellValue("收縮壓標準")




        // 如果数据不为空，填充数据行
        if (data.isNotEmpty()) {
            var totalSystolicPressure = 0.0
            var totalDiastolicPressure = 0.0
            var maxSystolicPressure = Int.MIN_VALUE
            var maxDiastolicPressure = Int.MIN_VALUE




            for (record in data) {
                totalSystolicPressure += record.systolicPressure
                totalDiastolicPressure += record.diastolicPressure
                if (record.systolicPressure > maxSystolicPressure) {
                    maxSystolicPressure = record.systolicPressure
                }
                if (record.diastolicPressure > maxDiastolicPressure) {
                    maxDiastolicPressure = record.diastolicPressure
                }
            }




            val averageSystolicPressure = totalSystolicPressure / data.size
            val averageDiastolicPressure = totalDiastolicPressure / data.size




            for ((index, record) in data.withIndex()) {
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(record.measurementTime)
                row.createCell(1).setCellValue(record.systolicPressure.toDouble())
                row.createCell(2).setCellValue(record.diastolicPressure.toDouble())
                row.createCell(3).setCellValue(averageSystolicPressure)
                row.createCell(4).setCellValue(averageDiastolicPressure)
                row.createCell(5).setCellValue(maxSystolicPressure.toDouble())
                row.createCell(6).setCellValue(maxDiastolicPressure.toDouble())
                row.createCell(7).setCellValue(getDiastolicPressureStandard(record.diastolicPressure))
                row.createCell(8).setCellValue(getSystolicPressureStandard(record.systolicPressure))
            }
        }




        // 确保文件夹已创建
        val reportDir = File(getExternalFilesDir(null), "reports")
        if (!reportDir.exists()) {
            reportDir.mkdirs() // 创建文件夹
        }




        // 保存 Excel 文件
        val reportFile = File(reportDir, "report_${System.currentTimeMillis()}.xlsx")
        FileOutputStream(reportFile).use { fos ->
            workbook.write(fos)
        }
        workbook.close()




        return reportFile
    }




    // 根据舒张压值获取标准
    private fun getDiastolicPressureStandard(diastolicPressure: Int): String {
        return when {
            diastolicPressure < 80 -> "正常"
            diastolicPressure in 80..89 -> "高血壓前期"
            diastolicPressure in 90..99 -> "第一期高血壓"
            else -> "第二期高血壓"
        }
    }




    // 根据收缩压值获取标准
    private fun getSystolicPressureStandard(systolicPressure: Int): String {
        return when {
            systolicPressure < 120 -> "正常"
            systolicPressure in 120..139 -> "高血壓前期"
            systolicPressure in 140..159 -> "第一期高血壓"
            else -> "第二期高血壓"
        }
    }




    // 保存报表
    private fun saveReport(report: File) {
        val outputFile = File(getExternalFilesDir(null), report.name)
        report.copyTo(outputFile, overwrite = true)
    }




    // 保存报告间隔
    private fun saveReportInterval(interval: Long) {
        val sharedPreferences = getSharedPreferences("ReportSettings", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putLong("interval", interval)
        editor.apply()
    }




    // 获取报告间隔
    private fun getReportInterval(): Long {
        val sharedPreferences = getSharedPreferences("ReportSettings", MODE_PRIVATE)
        return sharedPreferences.getLong("interval", AlarmManager.INTERVAL_DAY) // 默认为每日
    }




    // 数据类：血压数据
    data class BloodPressureData(
        @SerializedName("測量日期") var measurementTime: String,  // 映射 JSON 中的中文字段
        @SerializedName("收縮壓(mmHg)") val systolicPressure: Int,
        @SerializedName("舒張壓(mmHg)") val diastolicPressure: Int
    )




    fun sendEmail(toEmail: String, subject: String, body: String, attachmentFile: File) {
        val fromEmail = "ealthh10@gmail.com" // 你的 Gmail 地址
        val password = "svvn thtm euff inpu"   // 你的 Gmail 應用專用密碼




        val props = Properties().apply {
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
        }




        val session = Session.getInstance(props, object : javax.mail.Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(fromEmail, password)
            }
        })




        try {
            // 確保收件人地址不為空
            if (toEmail.isEmpty()) {
                Log.e("sendEmail", "Recipient email address is empty.")
                return
            }




            // 確保附件存在且可讀
            if (!attachmentFile.exists() || !attachmentFile.canRead()) {
                Log.e("sendEmail", "Attachment file does not exist or cannot be read: ${attachmentFile.absolutePath}")
                return
            }




            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(fromEmail))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                setSubject(subject)




                // 建立多部分內容
                val multipart = MimeMultipart()




                // 添加正文部分
                val textBodyPart = MimeBodyPart().apply {
                    setText(body, "UTF-8")
                }
                multipart.addBodyPart(textBodyPart)




                // 添加附件部分
                val attachmentBodyPart = MimeBodyPart().apply {
                    val fileSource = FileDataSource(attachmentFile)
                    dataHandler = DataHandler(fileSource)
                    fileName = attachmentFile.name
                }
                multipart.addBodyPart(attachmentBodyPart)




                setContent(multipart)
            }




            // 發送郵件
            Transport.send(message)
            Log.d("sendEmail", "Email sent successfully to $toEmail with attachment: ${attachmentFile.absolutePath}")
        } catch (e: MessagingException) {
            Log.e("sendEmail", "Error sending email: ${e.message}", e)
        } catch (e: Exception) {
            Log.e("sendEmail", "Unexpected error: ${e.message}", e)
        }
    }




    // 获取用户保存的 Gmail 地址
    private fun getUserGmail(): String? {
        val sharedPreferences = getSharedPreferences("UserSettings", Context.MODE_PRIVATE)
        return sharedPreferences.getString("gmail", null)  // 默认返回 null
    }












}
