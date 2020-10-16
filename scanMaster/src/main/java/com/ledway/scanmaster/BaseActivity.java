package com.ledway.scanmaster;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.ledway.scanmaster.data.Settings;
import javax.inject.Inject;

public class BaseActivity extends AppCompatActivity {
   @Inject public Settings settings;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ((MApp) getApplication()).getAppComponet().inject(this);
  }
}
