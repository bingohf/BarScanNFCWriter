package com.ledway.btprinter.biz.main;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.ledway.btprinter.R;
import com.ledway.scanmaster.domain.TimeIDGenerator;
import java.util.Locale;
import timber.log.Timber;

/**
 * Created by togb on 2017/12/16.
 */

public class WebViewFragment extends Fragment implements OnKeyPress {
  private static final String URL = "http://ledwayazure.cloudapp.net/mobile";
  @BindView(R.id.webView) WebView mWebView;
  @BindView(R.id.progressBar) ProgressBar mProgressBar;
  private String param;
  private String pdaGuid;
  private String mLastUrl;

  public WebViewFragment() {
    setRetainInstance(true);
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    TimeIDGenerator timeIDGenerator = new TimeIDGenerator(getContext());
    pdaGuid = timeIDGenerator.genID() + "~" + getLanguage();
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

  }

  private String getLanguage() {
    Locale locale = Locale.getDefault();
    return locale.getLanguage() + "_" + locale.getCountry();
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
        if (url.contains("retry")) {
          if(TextUtils.isEmpty(mLastUrl)){
            mLastUrl = URL;
          }
          view.loadUrl(mLastUrl);
          return true;
        } else if (url != null && url.endsWith("pdf")) {
          view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
          return true;
        } else {
          if (url != null && !url.contains("pdaGuid")) {
            if (url.contains("?")) {
              url += "&pdaGuid=" + pdaGuid;
            } else {
              url += "?pdaGuid=" + pdaGuid;
            }
          }
          Timber.i(url);
          view.loadUrl(url);
          return true;
        }
      }

      @Override public void onPageFinished(WebView view, String url) {
        mProgressBar.setVisibility(View.GONE);
      }

      @Override public void onReceivedError(WebView view, int errorCode, String description,
          String failingUrl) {
        mProgressBar.setVisibility(View.GONE);
        if (errorCode == ERROR_HOST_LOOKUP
            || errorCode == ERROR_CONNECT
            || errorCode == ERROR_TIMEOUT) {
          view.loadUrl("about:blank"); // 避免出现默认的错误界面
          view.loadUrl("file:///android_asset/error.html");
          mLastUrl = failingUrl;
        }
      }

      @RequiresApi(api = Build.VERSION_CODES.M) @Override
      public void onReceivedError(WebView view, WebResourceRequest request,
          WebResourceError error) {
        onReceivedError(view, error.getErrorCode(), error.getDescription().toString(),
            request.getUrl().toString());
        // super.onReceivedError(view, request, error);
      }
    });

    //super.onViewCreated(view, savedInstanceState);
  }

  @Override public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    mWebView.saveState(outState);
  }

  @Override public void onPause() {
    super.onPause();
  }

  @Override public void onStop() {
    super.onStop();
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
  }

  @Override public void onDestroy() {
    super.onDestroy();
  }

  @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (event.getAction() == KeyEvent.ACTION_DOWN) {
      if (keyCode == KeyEvent.KEYCODE_BACK && mWebView != null) {
        if (mWebView.canGoBack()) {
          mWebView.goBack();
          return true;
        }
      }
    }
    return false;
  }
}
