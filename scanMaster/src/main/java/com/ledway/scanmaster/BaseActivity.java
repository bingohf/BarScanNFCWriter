package com.ledway.scanmaster;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import com.ledway.scanmaster.data.Settings;
import javax.inject.Inject;

public class BaseActivity extends AppCompatActivity {
   @Inject public Settings settings;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ((MApp) getApplication()).getAppComponet().inject(this);
  }
}
