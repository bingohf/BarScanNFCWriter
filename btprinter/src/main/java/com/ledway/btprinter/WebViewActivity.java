package com.ledway.btprinter;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

/**
 * Created by togb on 2017/8/6.
 */

public class WebViewActivity extends AppCompatActivity {
  private WebView mVebView;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_webview);
    final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
    mVebView= (WebView) findViewById(R.id.webView);
    mVebView.getSettings().setUseWideViewPort( true);
    mVebView.getSettings().setBuiltInZoomControls(true);
    mVebView.getSettings().setJavaScriptEnabled(true);
    progressBar.setMax(100);
    String url;
    if(savedInstanceState != null){
      mVebView.restoreState(savedInstanceState);
    }else{
      url = getIntent().getStringExtra("url");
      mVebView.loadUrl(url);
    }
;


    mVebView.setWebChromeClient(new WebChromeClient(){
      @Override public void onProgressChanged(WebView view, int newProgress) {
        super.onProgressChanged(view, newProgress);
        progressBar.setProgress(newProgress);
        progressBar.setVisibility(View.VISIBLE);
      }
    });
    mVebView.setWebViewClient(new WebViewClient(){
      @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
       // view.loadUrl(url);
        return false;
      }

      @Override public void onPageFinished(WebView view, String url) {
        getSupportActionBar().setTitle(view.getTitle());
        progressBar.setVisibility(View.GONE);
      }
    });
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mVebView.saveState(outState);
  }

  @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
    if(event.getAction() == KeyEvent.ACTION_DOWN){
      if(keyCode == KeyEvent.KEYCODE_BACK){
        if(mVebView.canGoBack()){
          mVebView.goBack();
          return true;
        }else {
          return super.onKeyDown(keyCode, event);
        }
      }
    }
    return super.onKeyDown(keyCode, event);
  }
}
