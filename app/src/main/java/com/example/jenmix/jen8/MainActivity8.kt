package com.example.jenmix.jen8


import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.*
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import com.example.jenmix.jen8.RetrofitClient
import android.widget.TextView
import android.content.pm.ApplicationInfo
import com.example.jenmix.storage.UserPrefs
import com.example.jenmix.R
import com.example.jenmix.LoginActivity
import android.graphics.Color
import android.media.MediaPlayer
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt
import java.util.*
import java.util.Locale


@SuppressLint("MissingPermission")
class MainActivity8 : AppCompatActivity() {

    private lateinit var tvUserInfo: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvWeight: TextView
    private lateinit var tvUploadStatus: TextView
    private lateinit var recyclerCards: RecyclerView
    private lateinit var btnChart: Button
    private lateinit var btnHistory: Button

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothScanner: BluetoothLeScanner
    private var bluetoothGatt: BluetoothGatt? = null

    private val targetMac = "60:E8:5B:D6:90:77"
    private val serviceUuid = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    private val charUuid = UUID.fromString("0000ffe4-0000-1000-8000-00805f9b34fb")

    private val handler = Handler(Looper.getMainLooper())
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private var hasUploaded = false
    private val cardList = mutableListOf<HealthItem>()
    private lateinit var adapter: HealthCardAdapter

    private lateinit var tts: TextToSpeech

    private var lastParsedDataHash: Int? = null // ✅ 加入資料去重判斷用
    private var connectionEstablishedTime: Long = 0L


    companion object {
        const val PREF_GUIDE_WEIGHT_ANALYSIS_SHOWN = "guide_weight_analysis_shown"
    }

    // ✅ 提供外部手動啟動導覽（例如從設定頁）呼叫
    fun forceStartIntroGuide(context: Context) {
        val prefs = context.getSharedPreferences("onboarding_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_GUIDE_WEIGHT_ANALYSIS_SHOWN, false).apply()
        val intent = Intent(context, MainActivity8::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        // ✅ 先確認是否已登入
        val username = UserPrefs.getUsername(this)
        if (username.isNullOrBlank() || username.equals("guest", ignoreCase = true)) {
            Toast.makeText(this, "⚠️ 未登入，請重新登入", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }


        super.onCreate(savedInstanceState)

        // ✅ 強制重設導覽 flag（來自設定頁）
        if (intent.getBooleanExtra("restart_guide", false)) {
            val prefs = getSharedPreferences("onboarding_prefs", MODE_PRIVATE)
            prefs.edit().putBoolean(PREF_GUIDE_WEIGHT_ANALYSIS_SHOWN, false).apply()
        }

        if (!UserPrefs.isSetupCompleted(this)) {
            startActivity(Intent(this, FirstTimeSetupActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main8)

        // ✅ Debug 模式顯示 TTS 狀態
        val isDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebug) {
            val ttsStatus = if (UserPrefs.isTTSEnabled(this)) "✅ 語音提示已啟用" else "❌ 語音提示已停用"
            val debugTextView = TextView(this).apply {
                text = ttsStatus
                textSize = 14f
                setTextColor(Color.GRAY)
                setPadding(16, 32, 16, 0)
            }

            val toggleTTSButton = Button(this).apply {
                text = "切換語音提示"
                setOnClickListener {
                    val enabled = !UserPrefs.isTTSEnabled(this@MainActivity8)
                    UserPrefs.setTTSEnabled(this@MainActivity8, enabled)
                    Toast.makeText(this@MainActivity8, if (enabled) "語音提示已啟用" else "語音提示已停用", Toast.LENGTH_SHORT).show()
                    debugTextView.text = if (enabled) "✅ 語音提示已啟用" else "❌ 語音提示已停用"
                }
            }

            val openDebugButton = Button(this).apply {
                text = "⚙️ 開啟 Debug 畫面"
                setOnClickListener {
                    startActivity(Intent(this@MainActivity8, DebugActivity::class.java))
                }
            }

            val rootLayout = findViewById<LinearLayout>(R.id.rootLayout)
            rootLayout.addView(debugTextView)
            rootLayout.addView(toggleTTSButton)

        }

        tvUserInfo = findViewById(R.id.tvUserInfo)
        tvStatus = findViewById(R.id.tvStatus)
        tvWeight = findViewById(R.id.tvWeight)
        tvUploadStatus = findViewById(R.id.tvUploadStatus)
        recyclerCards = findViewById(R.id.recyclerCards)
        btnChart = findViewById(R.id.btnChart)
        btnHistory = findViewById(R.id.btnHistory)

        recyclerCards.layoutManager = GridLayoutManager(this, 2)
        adapter = HealthCardAdapter(cardList)
        recyclerCards.adapter = adapter
        recyclerCards.layoutAnimation = android.view.animation.LayoutAnimationController(
            AnimationUtils.loadAnimation(this, R.anim.card_fade_in)
        )

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, 0
        ) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                Collections.swap(cardList, from, to)
                adapter.notifyItemMoved(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        }).attachToRecyclerView(recyclerCards)

        val scaleAnim = AnimationUtils.loadAnimation(this, R.anim.button_scale)
        btnChart.setOnClickListener {
            it.startAnimation(scaleAnim)
            startActivity(Intent(this, ChartActivity::class.java))
        }
        btnHistory.setOnClickListener {
            it.animate().rotationY(360f).setDuration(400).withEndAction {
                it.rotationY = 0f
                startActivity(Intent(this, HistoryActivity::class.java))
            }.start()
        }

        val btMgr = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = btMgr.adapter
        bluetoothScanner = bluetoothAdapter.bluetoothLeScanner

        if (!bluetoothAdapter.isEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 2002)
                }
            } else {
                startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        }

        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }

        if (!hasPermissions()) {
            requestPermissions()
        } else {
            handler.postDelayed(scanLoop, 1000)
        }

        showUserInfo()
        setCardClickListener()
        setupTTS()

        showIntroDialogIfNeeded() // ✅ 啟動導覽對話框

        val btnSettings = findViewById<Button>(R.id.btnSettings)
        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // ✅ 跳過導覽按鈕處理
        val btnSkipIntro = findViewById<Button>(R.id.btnSkipIntro)
        btnSkipIntro.setOnClickListener {
            val introOverlay = findViewById<View>(R.id.introOverlay)
            introOverlay.animate()
                .alpha(0f)
                .setDuration(500)
                .withEndAction {
                    introOverlay.visibility = View.GONE
                    speak("已跳過導覽，祝你使用愉快！")
                }.start()

            val prefs = getSharedPreferences("onboarding_prefs", MODE_PRIVATE)
            prefs.edit().putBoolean(PREF_GUIDE_WEIGHT_ANALYSIS_SHOWN, true).apply()
        }
    }

    override fun onResume() {
        super.onResume()
// ✅ 補強 BLE 掃描初始化問題
        if (hasPermissions() && bluetoothAdapter.isEnabled && bluetoothGatt == null) {
            handler.postDelayed(scanLoop, 1500)
        }
    }

    private fun setupTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.TAIWAN
                tts.setSpeechRate(0.95f)
            }
        }
    }

    private fun speak(text: String) {
        if (UserPrefs.isTTSEnabled(this)) {
            if (tts.isSpeaking) tts.stop()
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
        }
    }

    private fun showIntroDialogIfNeeded() {
        val prefs = getSharedPreferences("onboarding_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean(PREF_GUIDE_WEIGHT_ANALYSIS_SHOWN, false)) {
            AlertDialog.Builder(this)
                .setTitle("啟用導覽？")
                .setMessage("我們可以帶你快速了解體重分析畫面，是否啟用導覽？")
                .setPositiveButton("啟用") { _, _ ->
                    showIntroGuide(prefs)
                }
                .setNegativeButton("跳過") { _, _ ->
                    prefs.edit().putBoolean(PREF_GUIDE_WEIGHT_ANALYSIS_SHOWN, true).apply()
                }
                .setCancelable(false)
                .show()
        }
        // ✅ 跳過導覽按鈕邏輯
        val btnSkipIntro = findViewById<Button>(R.id.btnSkipIntro)
        btnSkipIntro.setOnClickListener {
            val introOverlay = findViewById<View>(R.id.introOverlay)
            introOverlay.animate()
                .alpha(0f)
                .setDuration(500)
                .withEndAction {
                    introOverlay.visibility = View.GONE
                    speak("已跳過導覽，祝你使用愉快！")
                }.start()

            // 標記導覽已完成，避免下次再出現
            val prefs = getSharedPreferences("onboarding_prefs", MODE_PRIVATE)
            prefs.edit().putBoolean(PREF_GUIDE_WEIGHT_ANALYSIS_SHOWN, true).apply()
        }

    }


    private fun showIntroGuide(prefs: SharedPreferences) {
        val introOverlay = findViewById<View>(R.id.introOverlay)
        introOverlay.alpha = 0f
        introOverlay.visibility = View.VISIBLE
        introOverlay.animate()
            .alpha(1f)
            .setDuration(400)
            .withEndAction {
                speak("導覽即將開始，請跟著我一起認識這個畫面吧！")
                introOverlay.animate()
                    .alpha(0f)
                    .setDuration(500)
                    .setStartDelay(1200)
                    .withEndAction {
                        introOverlay.visibility = View.GONE
                        speak("這裡會顯示你的體重分析結果，點擊每張卡片可以查看詳細建議唷～")
                        startIntroSteps(prefs)
                    }.start()
            }.start()
    }

    private fun startIntroSteps(prefs: SharedPreferences) {
        MaterialTapTargetPrompt.Builder(this)
            .setTarget(recyclerCards)
            .setPrimaryText("第 1 步：這裡會顯示你的體重分析結果")
            .setSecondaryText("點擊每張卡片可以查看詳細建議唷～")
            .setBackButtonDismissEnabled(true)
            .setPromptStateChangeListener { _, state ->
                if (state == MaterialTapTargetPrompt.STATE_DISMISSED) {
                    speak("點擊這裡可以查看體重變化趨勢圖唷！")
                    MaterialTapTargetPrompt.Builder(this)
                        .setTarget(btnChart)
                        .setPrimaryText("第 2 步：查看圖表")
                        .setSecondaryText("點擊這裡可以查看體重變化趨勢圖唷！")
                        .setBackButtonDismissEnabled(true)
                        .setPromptStateChangeListener { _, state2 ->
                            if (state2 == MaterialTapTargetPrompt.STATE_DISMISSED) {
                                speak("這裡會顯示每次測量的時間與數據")
                                MaterialTapTargetPrompt.Builder(this)
                                    .setTarget(btnHistory)
                                    .setPrimaryText("第 3 步：查看歷史紀錄")
                                    .setSecondaryText("這裡會顯示每次測量的時間與數據")
                                    .setBackButtonDismissEnabled(true)
                                    .setPromptStateChangeListener { _, state3 ->
                                        if (state3 == MaterialTapTargetPrompt.STATE_DISMISSED) {
                                            prefs.edit().putBoolean(PREF_GUIDE_WEIGHT_ANALYSIS_SHOWN, true).apply()
                                            speak("導覽完成，祝你健康愉快！")
                                            AlertDialog.Builder(this)
                                                .setTitle("\uD83C\uDF89 導覽完成")
                                                .setMessage("你已完成導覽，現在可以自由探索體重分析功能囉！")
                                                .setPositiveButton("太棒了") { dialog, _ -> dialog.dismiss() }
                                                .setCancelable(false)
                                                .show()
                                        }
                                    }
                                    .show()
                            }
                        }
                        .show()
                }
            }
            .show()
    }

    private fun applyScaleAnimation(view: View, onClick: () -> Unit) {
        view.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            onClick()
        }.start()
    }

    private fun showUserInfo() {
        val displayName = UserPrefs.getDisplayName(this) // ✅ 正確抓取顯示名稱
        val username = UserPrefs.getUsername(this)
        val gender = UserPrefs.getGender(this)
        val age = UserPrefs.getAge(this)
        val height = UserPrefs.getHeight(this)
        tvUserInfo.text = "使用者：$displayName（帳號：$username）｜性別：$gender｜年齡：$age｜身高：${height}cm"
    }

    private fun hasPermissions(): Boolean {
        val perms = listOfNotNull(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_SCAN else null,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_CONNECT else null
        )
        return perms.all { ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
    }

    private fun requestPermissions() {
        val perms = listOfNotNull(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_SCAN else null,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_CONNECT else null
        )
        ActivityCompat.requestPermissions(this, perms.toTypedArray(), 2001)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 2001 && hasPermissions()) {
            handler.postDelayed(scanLoop, 1000)
        } else {
            Toast.makeText(this, getString(R.string.permission_denied_msg), Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        // ✅ 安全檢查 TTS 是否已初始化再 shutdown
        if (::tts.isInitialized) {
            tts.shutdown()
        }

        // ✅ 停止 Handler 任務
        handler.removeCallbacks(scanLoop)

        // ✅ 關閉藍牙連線
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()

        super.onDestroy()
    }


    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (device.address == targetMac) {
                runOnUiThread { tvStatus.text = getString(R.string.msg_connecting) }
                stopBleScan()
                bluetoothGatt?.disconnect()
                bluetoothGatt?.close()

                if (ActivityCompat.checkSelfPermission(this@MainActivity8, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothGatt = device.connectGatt(this@MainActivity8, false, gattCallback)
                } else {
                    runOnUiThread { tvStatus.text = getString(R.string.msg_no_ble_permission) }
                }
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectionEstablishedTime = System.currentTimeMillis() // ✅ 紀錄連線時間
                runOnUiThread { tvStatus.text = getString(R.string.msg_connected) }
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread {
                    tvStatus.text = getString(R.string.msg_disconnected)
                    bluetoothGatt = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val svc = gatt.getService(serviceUuid) ?: return
            val char = svc.getCharacteristic(charUuid) ?: return
            gatt.setCharacteristicNotification(char, true)
            val desc = char.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                ?: return
            desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(desc)
        }


        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val data = characteristic.value
            runOnUiThread { tvStatus.text = getString(R.string.msg_data_receiving) }

            if (data.size == 20 &&
                data[0] == 0x0D.toByte() &&
                data[1] == 0x1F.toByte() &&
                data[2] == 0x14.toByte()
            ) {
                val now = System.currentTimeMillis()

                // ✅ 過濾：連線後 3 秒內的資料（可能是舊資料）
                if (now - connectionEstablishedTime < 3000) {
                    Log.w("BLE", "⏳ 忽略連線後 3 秒內的資料（可能是舊資料）")
                    return
                }

                val parsed = BLEDataParser.parse(data)

                // ✅ 過濾重複資料
                if (parsed.dataHash == lastParsedDataHash) {
                    Log.w("BLE", "⚠️ 忽略重複 BLE 資料")
                    return
                }
                lastParsedDataHash = parsed.dataHash

                runOnUiThread {
                    tvStatus.text = getString(R.string.msg_measuring_done)
                    tvWeight.text = getString(R.string.label_weight, parsed.weight)

                    // ✅ 顯示完整測量時間（格式 yyyy/MM/dd HH:mm）
                    val dateFormat = android.text.format.DateFormat.format("yyyy/MM/dd HH:mm", now)
                    findViewById<TextView>(R.id.tvMeasuredTime)?.text = "🕒 測量時間：$dateFormat"

                    // ✅ 提示體重或阻抗為 0（姿勢錯誤）
                    if (parsed.weight <= 0f || parsed.impedance == 0) {
                        Toast.makeText(this@MainActivity8, "⚠️ 姿勢錯誤請雙腳站穩測量", Toast.LENGTH_SHORT).show()
                        return@runOnUiThread
                    }

                    if (!hasUploaded) {
                        hasUploaded = true
                        val username = UserPrefs.getUsername(this@MainActivity8)
                        if (username.isNullOrBlank()) {
                            Toast.makeText(this@MainActivity8, "⚠️ 未登入，請重新登入", Toast.LENGTH_SHORT).show()
                            return@runOnUiThread
                        }
                        uploadWeightToServer(username, parsed.weight)

                        showHealthCards(parsed.weight, parsed.impedance, parsed.bmr)

                        // ✅ 顯示儲存提示 Toast（延遲 1 秒顯示）
                        tvUploadStatus.postDelayed({
                            hasUploaded = false
                            tvUploadStatus.text = ""
                            Toast.makeText(
                                this@MainActivity8,
                                "✅ 已儲存紀錄至歷史以及圖表",
                                Toast.LENGTH_SHORT
                            ).show()
                        }, 1000)
                    }
                }
            }
        }
    }

    private val scanLoop = object : Runnable {
        override fun run() {
            if (bluetoothGatt == null) {
                runOnUiThread { tvStatus.text = getString(R.string.msg_scan) }
                startBleScan()
                handler.postDelayed({
                    stopBleScan()
                    handler.postDelayed(this, 5000)
                }, 5000)
            }
        }
    }

    private fun startBleScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bluetoothScanner.startScan(null, scanSettings, scanCallback)
        } else {
            tvStatus.text = getString(R.string.msg_no_ble_permission)
        }
    }

    private fun stopBleScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bluetoothScanner.stopScan(scanCallback)
        }
    }

    private fun uploadWeightToServer(username: String, weight: Float) {
        val gender = UserPrefs.getGender(this)
        val height = UserPrefs.getHeight(this)
        val age = UserPrefs.getAge(this)
        val roundedWeight = String.format("%.1f", weight).toFloat()

        val request = WeightUploadRequest(
            username = username, // ✅ 直接使用參數
            weight = roundedWeight,
            gender = gender,
            height = height,
            age = age
        )

        val api = RetrofitClient.create<UploadApi>()
        api.uploadWeight(request)
            .enqueue(object : Callback<WeightUploadResponse> {
                override fun onResponse(call: Call<WeightUploadResponse>, response: Response<WeightUploadResponse>) {
                    tvUploadStatus.text = if (response.isSuccessful) {
                        getString(R.string.msg_upload_success)
                    } else {
                        getString(R.string.msg_upload_fail, response.message())
                    }
                }

                override fun onFailure(call: Call<WeightUploadResponse>, t: Throwable) {
                    tvUploadStatus.text = getString(R.string.msg_upload_error, t.message ?: "未知錯誤")
                }
            })
    }

    private fun showHealthCards(weight: Float, impedance: Int, bmr: Int) {
        val gender = UserPrefs.getGender(this)
        val age = UserPrefs.getAge(this)
        val height = UserPrefs.getHeight(this)
        if (gender.isNotBlank() && age > 0 && height > 0f) {
            val cards = HealthCardGenerator.generateCards(gender, age, height, weight, impedance, bmr)

            // ✅ 音效 + 動畫播放
            val mediaPlayer = MediaPlayer.create(this, R.raw.bling)
            mediaPlayer?.start()

            cardList.clear()
            cardList.addAll(cards)
            adapter.notifyDataSetChanged()
            recyclerCards.scheduleLayoutAnimation() // ✅ 使用 recyclerView 動畫
        }
    }

    private fun setCardClickListener() {
        adapter.setOnCardClickListener { item ->
            val dialog = HealthDetailDialogFragment.newInstance(item)
            dialog.show(supportFragmentManager, "HealthDetailDialog")

            // ✅ 朗讀內容
            val text = "${item.title}：${item.value}，狀態為 ${item.status}，建議：${item.suggestion}"
            speak(text)
        }
    }
}