package com.ledway.btprinter.biz.startup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import com.ledway.btprinter.R
import com.ledway.framework.FullScannerActivity


class StartupActivity: AppCompatActivity() {
  private val REQUEST_CAMERA_SET_GROUP = 1
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_startup)
    val sp = getSharedPreferences("group", Context.MODE_PRIVATE)
    val myTaxNo = sp.getString("MyTaxNo","")
    val line = sp.getString("Line", "")
    if(TextUtils.isEmpty(myTaxNo) || TextUtils.isEmpty(line)){
      if(savedInstanceState== null) {
        startActivityForResult(Intent(this, FullScannerActivity::class.java),
            REQUEST_CAMERA_SET_GROUP)
      }
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode){
      REQUEST_CAMERA_SET_GROUP ->{
        if(resultCode == Activity.RESULT_OK){
          val code = intent.getStringExtra("barcode")
          saveGroupInfo(code)
          finish()
        }else{
          finish()
        }
      }
    }
  }

  private fun saveGroupInfo(code: String) {
    val ss = code.split('~')
    val myTaxNo = ss[1]
    val line = ss[2]
    getSharedPreferences("group", Context.MODE_PRIVATE).edit()
        .putString("MyTaxNo", myTaxNo).putString("Line", line)
        .apply()

  }
}