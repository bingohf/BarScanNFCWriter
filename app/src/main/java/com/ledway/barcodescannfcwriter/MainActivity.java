package com.ledway.barcodescannfcwriter;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private byte keyA[] = { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff};

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] mFilters;
    private String[][] mTechLists;
    private Intent intents;
    private EditText mTxtBarCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTxtBarCode = (EditText) findViewById(R.id.txt_barcode);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null) {
            Toast.makeText(this, R.string.device_no_nfc,Toast.LENGTH_LONG).show();
        }
        if (!nfcAdapter.isEnabled()) {
            Toast.makeText(this, R.string.device_nfc_disabled,Toast.LENGTH_LONG).show();
            return;
        }

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        ndef.addCategory("*/*");
        mFilters = new IntentFilter[] { ndef };// 过滤器
        mTechLists = new String[][] {
                new String[] { MifareClassic.class.getName() },
                new String[] { NfcA.class.getName() } };// 允许扫描的标签类型

        findViewById(R.id.btn_write_nfc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Tag tagFromIntent = intents
                        .getParcelableExtra(NfcAdapter.EXTRA_TAG);
                if(tagFromIntent == null){
                    Toast.makeText(MainActivity.this, R.string.no_card_found,Toast.LENGTH_LONG).show();
                    return;
                }
                MifareClassic mfc = MifareClassic
                        .get(tagFromIntent);
                boolean auth = false;
                try {
                    mfc.connect();
                    auth = mfc.authenticateSectorWithKeyA(
                            1,
                            keyA);
                    if(!auth){
                        Toast.makeText(MainActivity.this, R.string.auth_fail_card,Toast.LENGTH_LONG).show();
                    }else{
                        byte[] d = mTxtBarCode.getText().toString().trim().getBytes();
                        byte[] f = new byte[16];
                        for (int j = 0; j < d.length; j++) {
                            f[j] = d[j];
                        }
                        if (d.length < 16) {
                            int j = 16 - d.length;
                            int k = d.length;
                            for (int j2 = 0; j2 < j; j2++) {
                                f[k + j2] = (byte) 0x00;
                            }
                        }
                        mfc.writeBlock(0, f);
                        byte[] bytes = mfc.readBlock(0);
                        Toast.makeText(getApplicationContext(),"write success:" +  new String(bytes),Toast.LENGTH_LONG).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this, R.string.check_nfc_card,Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
                finally {
                    try {
                        mfc.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
       // getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        nfcAdapter.enableForegroundDispatch(this, pendingIntent, mFilters,
                mTechLists);
            if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(getIntent()
                    .getAction())) {
                // 处理该intent
                intents = getIntent();
            }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            // 处理该intent
            intents = intent;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
