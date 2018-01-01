package com.ledway.scanmaster.nfc;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.text.TextUtils;
import android.util.Log;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * Created by togb on 2016/10/30.
 */

public class GNdef extends GNfc {
  private final static String TAG = GNfc.class.getSimpleName();
  private final Ndef ndef;
  private String cachedText = null;
  public GNdef(Tag tag) {
    super(tag);
    this. ndef = Ndef.get(tag);
  }

  @Override public String read() throws IOException {
    if (!TextUtils.isEmpty(cachedText)){
      return cachedText;
    }
    NdefMessage ndefMessage = ndef.getCachedNdefMessage();
    if (ndefMessage != null) {
      NdefRecord[] records = ndefMessage.getRecords();
      for (NdefRecord ndefRecord : records) {
        if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(),
            NdefRecord.RTD_TEXT)) {
          try {
            return readText(ndefRecord);
          } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unsupported Encoding", e);
          }
        }
      }
    }
    return "";
  }
  private String readText(NdefRecord record) throws UnsupportedEncodingException {
        /*
         * See NFC forum specification for "Text Record Type Definition" at 3.2.1
         *
         * http://www.nfc-forum.org/specs/
         *
         * bit_7 defines encoding
         * bit_6 reserved for future use, must be 0
         * bit_5..0 length of IANA language code
         */

    byte[] payload = record.getPayload();

    // Get the Text Encoding
    String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

    // Get the Language Code
    int languageCodeLength = payload[0] & 0063;

    // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
    // e.g. "en"

    // Get the Text
    return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
  }
  @Override public void write(String text) throws IOException {
    NdefRecord[] records = { createRecord(text) };
    NdefMessage message = new NdefMessage(records);
    try {
      ndef.writeNdefMessage(message);
      cachedText = text;
    } catch (FormatException e) {
      e.printStackTrace();
      Log.e("error", e.getMessage(), e);
      throw  new IOException(e.getMessage());
    }
  }

  private NdefRecord createRecord(String text) throws UnsupportedEncodingException {

    //create the message in according with the standard
    String lang = "en";
    byte[] textBytes = text.getBytes();
    byte[] langBytes = lang.getBytes("UTF-8");
    int langLength = langBytes.length;
    int textLength = textBytes.length;

    byte[] payload = new byte[1 + langLength + textLength];
    payload[0] = (byte) langLength;

    // copy langbytes and textbytes into payload
    System.arraycopy(langBytes, 0, payload, 1, langLength);
    System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

    NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
    return recordNFC;
  }

  @Override public void connect() throws IOException {
    ndef.connect();
  }

  @Override public int getProperty() {
    return PROPERTY_READABLE;
  }

  @Override public void close() throws IOException {
    ndef.close();
  }
}
