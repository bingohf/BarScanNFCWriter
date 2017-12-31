package com.ledway.btprinter.biz;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import butterknife.ButterKnife;
import com.ledway.btprinter.R;
import com.ledway.btprinter.biz.main.ProductListFragment;

/**
 * Created by togb on 2017/12/3.
 */

public class ProductPickerActivity extends AppCompatActivity {


  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_picker_product);
    ButterKnife.bind(this);
    ProductListFragment fragment = new ProductListFragment();
    Bundle arguments = getIntent().getExtras();
    arguments.putBoolean("select", true);
    fragment.setArguments(arguments);
    getSupportFragmentManager().beginTransaction()
        .replace(R.id.container, fragment, "tag")
        .commit();
  }
}
