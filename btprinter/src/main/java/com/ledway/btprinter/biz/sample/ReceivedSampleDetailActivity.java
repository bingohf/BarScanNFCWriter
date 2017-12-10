package com.ledway.btprinter.biz.sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import com.activeandroid.Cache;
import com.activeandroid.Model;
import com.activeandroid.query.Select;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledway.btprinter.R;
import com.ledway.btprinter.models.ReceivedSample;
import com.ledway.btprinter.models.SampleProdLink;
import java.io.IOException;
import java.util.List;

/**
 * Created by togb on 2017/12/10.
 */

public class ReceivedSampleDetailActivity extends AppCompatActivity {
  private SampleProdLink mList;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.fragment_sample_list);
    String guid = getIntent().getStringExtra("guid");
    try {
      loadData(guid);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void loadData(String guid) throws IOException {
    List<Model> list = new Select().from(ReceivedSample.class).where("holdId =?", guid).execute();
    ReceivedSample item = (ReceivedSample) list.get(0);
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mList = objectMapper.readValue(item.detailJson, SampleProdLink.class);
  }
}
