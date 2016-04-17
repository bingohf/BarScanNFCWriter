package com.ledway.barcodescannfcwriter;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.serialport.api.SerialPort;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.activeandroid.Model;
import com.activeandroid.query.Select;
import com.ledway.barcodescannfcwriter.models.Record;
import com.zkc.Service.CaptureService;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private byte keyA[] = { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff};

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] mFilters;
    private String[][] mTechLists;
    private Intent intents;
    private EditText mEdtBarCode;
    private BroadcastReceiver scanBroadcastReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getExtras().getString("code");
            Log.i(TAG, "MyBroadcastReceiver code:" + text);
            mEdtBarCode.setText(text);
        }
    };
    private View mBtnClear;
    private ListView mListRecord;
    private ArrayList<String> mArrayRecord;
    private ArrayAdapter<String> mListAdapter;
    private CompositeSubscription subscriptions = new CompositeSubscription();
    private Vibrator vibrator;
    private String mLine;
    private String mReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.my_layout).requestFocus();
        preCheck();
        mEdtBarCode = (EditText) findViewById(R.id.txt_barcode);
        mListRecord = (ListView) findViewById(R.id.list_record);
        mArrayRecord = getRecordHistory();
        mListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,mArrayRecord);
        mListRecord.setAdapter(mListAdapter);
        //mListRecord.scrollTo(0,100000);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        vibrator=(Vibrator)getSystemService(Service.VIBRATOR_SERVICE);


        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        ndef.addCategory("*/*");
        mFilters = new IntentFilter[] { ndef };// 过滤器
        mTechLists = new String[][] {
                new String[] { MifareClassic.class.getName() },
                new String[] { NfcA.class.getName() } };// 允许扫描的标签类型

        findViewById(R.id.btn_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SerialPort.CleanBuffer();
                CaptureService.scanGpio.openScan();
            }
        });
        mBtnClear = findViewById(R.id.btn_clear);
        mBtnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEdtBarCode.setText("");
            }
        });
        mEdtBarCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mBtnClear.setVisibility(s.length() > 0? View.VISIBLE:View.GONE);
            }
        });
        Intent newIntent = new Intent(MainActivity.this, CaptureService.class);
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startService(newIntent);



        subscriptions.add(Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("com.zkc.scancode");
                MainActivity.this.registerReceiver(scanBroadcastReceiver, intentFilter);
                subscriber.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        MainActivity.this.unregisterReceiver(scanBroadcastReceiver);
                    }
                }));
            }
        }).subscribe());

        if (nfcAdapter == null) {
            Toast.makeText(this, R.string.device_no_nfc,Toast.LENGTH_LONG).show();
        }
        if (!nfcAdapter.isEnabled()) {
            Toast.makeText(this, R.string.device_nfc_disabled,Toast.LENGTH_LONG).show();
            return;
        }

    }
    private void exitActivity() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.exit)
                .setMessage(R.string.exit_confirm)
                .setPositiveButton(R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {

                                CaptureService.scanGpio.closeScan();
                                CaptureService.scanGpio.closePower();
                                finish();
                            }
                        }).setNegativeButton(R.string.no, null).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
       getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    protected void onDestroy() {

        super.onDestroy();
        subscriptions.clear();
    }

    @Override
    public void onBackPressed() {
        exitActivity();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter.isEnabled()) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, mFilters,
                    mTechLists);
            if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(getIntent()
                    .getAction())) {
                // 处理该intent
                intents = getIntent();
            }
        }




    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            // 处理该intent
            intents = intent;
            writeToIC();
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
            Intent intent = new Intent(this, AppPreferences.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void writeToIC(){
        final String barcode = mEdtBarCode.getText().toString().trim();
        Pattern pattern = Pattern.compile("[^0-9a-zA-Z_ ]");
        if (TextUtils.isEmpty(barcode) || pattern.matcher(barcode).matches()){
            Toast.makeText(this, R.string.barcode_invalid, Toast.LENGTH_LONG).show();
            return;
        }
        if(intents == null){
            Toast.makeText(MainActivity.this, R.string.no_card_found,Toast.LENGTH_LONG).show();
            return;
        }
        final Tag tagFromIntent = intents
                .getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if(intents == null){
            Toast.makeText(MainActivity.this, R.string.no_card_found,Toast.LENGTH_LONG).show();
            return;
        }

        subscriptions.add(validateBarcode(barcode).subscribe(new Action1() {
            @Override
            public void call(Object o) {
                MifareClassic mfc = MifareClassic
                        .get(tagFromIntent);
                boolean auth = false;
                try {
                    mfc.connect();
                    auth = mfc.authenticateSectorWithKeyA(
                            1,
                            keyA);
                    if (!auth) {
                        Toast.makeText(MainActivity.this, R.string.auth_fail_card, Toast.LENGTH_LONG).show();
                    } else {
                        byte[] d = mEdtBarCode.getText().toString().trim().getBytes();
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
                        mfc.writeBlock(4, f);
                        mfc.sectorToBlock(4);
                        byte[] bytes = mfc.readBlock(4);
                        if (barcode.equals(new String(bytes).trim())) {
                            Record r = new Record();
                            r.readings = barcode;
                            r.wk_date = new Date();
                            r.reader = mReader;
                            r.line = mLine;
                            r.lwGuid =  UUID.randomUUID().toString();
                            r.save();
                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                            mListAdapter.add(simpleDateFormat.format(r.wk_date) + ":\t" + barcode);
                            mListRecord.smoothScrollByOffset(100000);

                        }

                    }
                } catch (IOException e) {
                    vibrator.vibrate(1000);
                    Toast.makeText(MainActivity.this, R.string.check_nfc_card, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                } finally {
                    try {
                        mfc.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }));


    }

    private ArrayList<String> getRecordHistory() {
        List<Record> records = new Select().from(Record.class).orderBy("uploaded_datetime desc").execute();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        ArrayList<String> list = new ArrayList<>();
        for(Record r : records){
            list.add(simpleDateFormat.format(r.wk_date) + ":\t" + r.readings);
        }
        return  list;
    }
    private Record findRecord(String barcode){
        List<Record> records = new Select().from(Record.class).where("readings =?", barcode).orderBy("wk_date desc").limit(1).execute();
        if (records.size() > 0 ){
            return records.get(0);
        }
        return null;
    }
    private boolean inWrite = false;
    private Observable validateBarcode(final String barcode){
        return Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(final Subscriber<? super Object> subscriber) {
                Record record = findRecord(barcode);
                if (record == null){
                    subscriber.onNext(null);
                }else{
                    if (inWrite){
                        subscriber.onCompleted();
                    }else {
                        inWrite = true;
                        vibrator.vibrate(1000);
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(R.string.had_to_mifare)
                                .setMessage(simpleDateFormat.format(record.wk_date) + ":\t" + record.readings + "\r\n" + "确认再写一次？")
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        subscriber.onNext(null);
                                    }
                                })
                                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        subscriber.onCompleted();
                                        inWrite = false;
                                    }
                                })
                                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        subscriber.onCompleted();
                                        inWrite = false;
                                    }
                                }).create().show();
                    }
                }
            }
        });
    }

    private void preCheck(){
/*        ConnectivityManager manager = (ConnectivityManager)
                getSystemService(MainActivity.CONNECTIVITY_SERVICE);
*//*
 * 3G confirm
 *//*
        Boolean is3g = manager.getNetworkInfo(
                ConnectivityManager.TYPE_MOBILE).isConnectedOrConnecting();
*//*
 * wifi confirm
 *//*
        Boolean isWifi = manager.getNetworkInfo(
                ConnectivityManager.TYPE_WIFI).isConnectedOrConnecting();
        if (!is3g && !isWifi) {
            // Activity transfer to wifi settings
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        }
        */
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        mLine = sp.getString("Line","");
        mReader = sp.getString("Reader", "");
        if(TextUtils.isEmpty(mLine) || TextUtils.isEmpty(mReader)){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.invalid_setting)
                    .setMessage(R.string.goto_setting)
                    .setPositiveButton(R.string.setting, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(MainActivity.this, AppPreferences.class);
                            startActivityForResult(intent,1);
                        }
                    })
                    .setCancelable(false);
            builder.create().show();
        }else {
            getSupportActionBar().setTitle(mLine + " - " + mReader + "  " + getString(R.string.company_short));
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 1){
            preCheck();
        }
    }
}
