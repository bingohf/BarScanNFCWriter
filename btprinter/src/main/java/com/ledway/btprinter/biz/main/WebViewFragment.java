package com.ledway.btprinter.biz.main;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.ledway.btprinter.R;

/**
 * Created by togb on 2017/12/16.
 */

public class WebViewFragment extends Fragment {
  private static final String URL = "http://ledwayazure.cloudapp.net/mobile";
  @BindView(R.id.webView) WebView mWebView;
  @BindView(R.id.progressBar) ProgressBar mProgressBar;

  public WebViewFragment() {
    setRetainInstance(true);
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.activity_webview, container, false);
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    ButterKnife.bind(this, view);

    mWebView.getSettings().setUseWideViewPort(true);
    mWebView.getSettings().setBuiltInZoomControls(true);
    mWebView.getSettings().setDisplayZoomControls(false);
    mWebView.getSettings().setJavaScriptEnabled(true);
    if (savedInstanceState != null) {
      mWebView.restoreState(savedInstanceState);
    } else {
      mWebView.loadUrl(URL);
    }

    mWebView.setWebChromeClient(new WebChromeClient() {
      @Override public void onProgressChanged(WebView view, int newProgress) {
        super.onProgressChanged(view, newProgress);
        mProgressBar.setProgress(newProgress);
        mProgressBar.setVisibility(View.VISIBLE);
      }
    });

    mWebView.setWebViewClient(new WebViewClient() {
      @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
        // view.loadUrl(url);
        return false;
      }

      @Override public void onPageFinished(WebView view, String url) {
        mProgressBar.setVisibility(View.GONE);
      }
    });
    //super.onViewCreated(view, savedInstanceState);
  }

  @Override public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    mWebView.saveState(outState);
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
  }

  @Override public void onPause() {
    super.onPause();
  }

  @Override public void onStop() {
    super.onStop();
  }
}
