package com.ledway.barcodescannfcwriter;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
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
import com.zkc.beep.ServiceBeepManager;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
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
    private Settings settings;
    private ServiceBeepManager beepManager;
    private int todayActionCount = 0;
    private BroadcastReceiver scanBroadcastReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getExtras().getString("code");
            if (text.length()<10){
                Toast.makeText(MainActivity.this, R.string.invalid_barcode, Toast.LENGTH_LONG).show();
            }
            Log.i(TAG, "MyBroadcastReceiver code:" + text);
            Pattern pattern = Pattern.compile("[^0-9a-zA-Z_ ]");
            if(!pattern.matcher(text).matches()) {
                mEdtBarCode.setText(text);
                if (settings.getDeviceType().equals("ReadNFC")){
                    Record record = new Record();
                    record.readings = text;
                    insertRecordLog(record);
                }
            }else{
                vibrator.vibrate(1000);
            }
        }
    };


    private void insertRecordLog(Record record) {
        record.wk_date = new Date();
        record.reader = settings.getReader();
        record.line = settings.getLine();
        record.lwGuid =  UUID.randomUUID().toString();
        record.save();
        mListAdapter.insert(record, 0);
        ++todayActionCount;
        invalidateOptionsMenu();
       //mListAdapter.notifyDataSetChanged();
        if (settings.isAutoUpload()) {
            MApp.getInstance().getUploadService().uploadRecord(record)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Record>() {
                    @Override public void onCompleted() {
                        mListAdapter.notifyDataSetChanged();
                    }

                    @Override public void onError(Throwable e) {

                    }

                    @Override public void onNext(Record record) {
                        Log.v("record", record.wk_date.toLocaleString());
                    }
                });
        }
    }

    private View mBtnClear;
    private ListView mListRecord;
    private RecordListAdapter mListAdapter;
    private CompositeSubscription subscriptions = new CompositeSubscription();
    private Vibrator vibrator;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = MApp.getInstance().getSettings();
        setContentView(R.layout.activity_main);
        findViewById(R.id.my_layout).requestFocus();
        preCheck();
        mEdtBarCode = (EditText) findViewById(R.id.txt_barcode);
        mListRecord = (ListView) findViewById(R.id.list_record);
        mListAdapter = new RecordListAdapter(this, 0);
        getRecordHistory();
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
        newIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP );
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

    @Override public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_count);
        item.setTitle(todayActionCount +"");
        return  true;
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
        CleanService.getInstance().checkAndStart().subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<String>() {
                @Override public void onCompleted() {

                }

                @Override public void onError(Throwable e) {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }

                @Override public void onNext(String s) {

                }
            });




    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            // 处理该intent
            intents = intent;
            if (settings.getDeviceType().equals("ReadNFC")){
                Tag tagFromIntent = intents
                        .getParcelableExtra(NfcAdapter.EXTRA_TAG);
                MifareClassic mfc = MifareClassic
                        .get(tagFromIntent);

                try {
                    mfc.connect();
                    boolean auth = mfc.authenticateSectorWithKeyA(
                            1,
                            keyA);
                    String mifareID = readMifareId(intent);
                    if (auth) {
                        byte[] bytes = mfc.readBlock(4);
                        String barcode = new String(bytes).trim();
                        if(!TextUtils.isEmpty(barcode)) {
                            mEdtBarCode.setText(barcode);
                            Record record = new Record();
                            record.readings = barcode;
                            record.rfidSeries = mifareID;
                            insertRecordLog(record);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else {
                writeToIC();
            }
        }
    }
    private String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        char[] buffer = new char[2];
        for (int i = 0; i < src.length; i++) {
            buffer[0] = Character.forDigit((src[i] >>> 4) & 0x0F, 16);
            buffer[1] = Character.forDigit(src[i] & 0x0F, 16);

            stringBuilder.append(buffer);

        }
        return stringBuilder.toString();
    }

    private String readMifareId(Intent intent){
        byte[] myNFCID = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
        int before = (int) Long.parseLong(bytesToHexString(myNFCID), 16);
        int r24 = before >> 24 & 0x000000FF;
        int r8 = before >> 8 & 0x0000FF00;
        int l8 = before << 8 & 0x00FF0000;
        int l24 = before << 24 & 0xFF000000;

        long mifareId = Long.parseLong(Integer.toHexString((r24 | r8 | l8 | l24)), 16);
        return mifareId +"";
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
            startActivityForResult(intent,1);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void writeToIC(){
        final String barcode = mEdtBarCode.getText().toString().trim();
        Pattern pattern = Pattern.compile("[^0-9a-zA-Z_ ]");
        if (TextUtils.isEmpty(barcode) || pattern.matcher(barcode).matches()){
            vibrator.vibrate(1000);
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
                inWrite = false;
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
                            r.rfidSeries = readMifareId(intents);
                            insertRecordLog(r);
                            mListRecord.smoothScrollByOffset(0);
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

    private long clearTime(Date date){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }


    private void getRecordHistory() {
        todayActionCount = 0;
        List<Record> records = new Select().from(Record.class).where("uploaded_datetime is null").orderBy("uploaded_datetime desc").execute();
        List<Record> uploadedRecords = new Select().from(Record.class).where("uploaded_datetime is not null").orderBy("uploaded_datetime desc").execute();
        for(Record r : records){
            mListAdapter.add(r);
        }
        for(Record r : uploadedRecords){
            mListAdapter.add(r);
        }
        long today = clearTime(new Date());
        for(Record r : uploadedRecords){
            long day = clearTime(r.wk_date);
            if (day != today ){
                break;
            }
            ++todayActionCount;
        }
        for(Record r : records){
            long day = clearTime(r.wk_date);
            if (day != today ){
                break;
            }
            ++todayActionCount;
        }
        invalidateOptionsMenu();
    }

   private void showTotalActionCount(){

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
        settings.reload();
        if(TextUtils.isEmpty(settings.getLine()) || TextUtils.isEmpty(settings.getReader())){
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
            String type = settings.getDeviceType().equals("ReadNFC") ?" ": " W ";
            getSupportActionBar().setTitle(settings.getLine() + " - " + settings.getReader() + type + getString(R.string.company_short));
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 1){
            preCheck();
            MApp.getInstance().getUploadService().reset();
            mListAdapter.clear();
            getRecordHistory();
            mListAdapter.notifyDataSetChanged();
            mListRecord.smoothScrollByOffset(0);
        }
    }
}
