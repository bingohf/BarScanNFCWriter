package com.ledway.scanmaster

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.widget.TextView

class AboutActivity:AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_about)
    val version =  packageManager.getPackageInfo(packageName,0).versionName
    findViewById<TextView>(R.id.txt_app).setText("版本：" + version)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    if (item != null) {
      if(item.itemId == android.R.id.home){
        finish()
      }
    }
    return super.onOptionsItemSelected(item)
  }
}