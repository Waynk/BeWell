package com.example.jenmix.jen7

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.jenmix.R
import com.google.android.gms.location.LocationServices
import kotlin.apply
import kotlin.collections.isNotEmpty

class MainActivity7 : AppCompatActivity() {

    private companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    private lateinit var btnShareLocation: Button
    private lateinit var btnNeedHelp: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main7)

        btnShareLocation = findViewById(R.id.btnShareLocation)
        btnNeedHelp = findViewById(R.id.btnNeedHelp)

        btnShareLocation.setOnClickListener {
            checkLocationPermission { shareLocation("這是我的位置：") }
        }
        btnNeedHelp.setOnClickListener {
            checkLocationPermission { shareLocation("⚠️ 我需要幫助！請幫我，位置：") }
        }
    }

    private fun checkLocationPermission(onGranted: () -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            onGranted()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "定位權限已允許，請再按一次按鈕", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "需要定位權限才能繼續", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun shareLocation(prefixText: String) {
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)

        val locationRequest = com.google.android.gms.location.LocationRequest.create().apply {
            priority = com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
            interval = 1000
            numUpdates = 1 // 只要一次更新
        }

        val locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                val location = result.lastLocation
                if (location != null) {
                    val lat = location.latitude
                    val lng = location.longitude
                    val mapsUrl = "https://maps.google.com/?q=$lat,$lng"
                    val shareText = "$prefixText $mapsUrl"

                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    startActivity(Intent.createChooser(intent, "分享位置"))
                } else {
                    Toast.makeText(this@MainActivity7, "無法取得位置", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 發起定位請求
        fusedClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )
    }
}