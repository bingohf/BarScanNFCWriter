package com.ledway.btprinter;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;

/**
 * Created by togb on 2016/7/10.
 */
public class AgreementActivity extends AppCompatActivity {
  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_agreement);
    findViewById(R.id.btn_agree).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        setResult(RESULT_OK);
        finish();
      }
    });
  }
}
