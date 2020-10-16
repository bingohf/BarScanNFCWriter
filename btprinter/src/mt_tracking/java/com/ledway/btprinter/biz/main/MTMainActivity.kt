package com.ledway.btprinter.biz.main

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import com.ledway.btprinter.R
import com.tbruyelle.rxpermissions2.RxPermissions

class MTMainActivity : AppCompatActivity(){
  internal val rxPermissions = RxPermissions(this)
  internal val REQUEST_PERMISSIONS_SETTING = 10
  internal val PERMISSIONS = arrayOf(Manifest.permission.NFC, Manifest.permission.CAMERA,
      Manifest.permission.CALL_PHONE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
      Manifest.permission.ACCESS_WIFI_STATE)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_mt_tracking_main)
    requestPermission()
  }

  private fun requestPermission() {
    rxPermissions.requestEachCombined(*PERMISSIONS).subscribe({ permission ->
      if (permission.granted) {
     //   Toast.makeText(this, "granted", Toast.LENGTH_LONG).show();
      } else if (permission.shouldShowRequestPermissionRationale) {
        AlertDialog.Builder(this).setTitle(R.string.re_grant)
            .setMessage(R.string.re_grant_message)
            .setCancelable(false)
            .setPositiveButton(R.string.app_setting) { dialogInterface, i -> requestPermission() }
            .setNegativeButton(R.string.exit) { dialogInterface, i -> finish() }
            .create()
            .show()
      } else {

        AlertDialog.Builder(this).setTitle(R.string.re_grant)
            .setMessage(R.string.re_grant_message)
            .setCancelable(false)
            .setPositiveButton(R.string.re_grant) { dialogInterface, i ->
              val intent = Intent()
              intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
              val uri = Uri.fromParts("package", packageName, null)
              intent.data = uri
              startActivityForResult(intent, REQUEST_PERMISSIONS_SETTING)
            }
            .setNegativeButton(R.string.exit) { dialogInterface, i -> finish() }
            .create()
            .show()
      }
    })
  }
}