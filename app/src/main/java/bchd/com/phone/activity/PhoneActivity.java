package bchd.com.phone.activity;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.orhanobut.logger.Logger;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;

import bchd.com.phone.R;
import bchd.com.phone.model.PhoneCallLog;
import bchd.com.phone.utils.Mutils;
import bchd.com.phone.utils.PhoneSharePreferences;

public class PhoneActivity extends AppCompatActivity implements View.OnClickListener {
    private Button sendButton;
    private Button historyButton;
    private EditText phoneEditText;

    private ArrayList<PhoneCallLog> mDatas = new ArrayList<PhoneCallLog>();
    private AsyncQueryHandler asyncQuery;

    @Override
    protected void onRestart() {
        super.onRestart();

        checkShouldUpdatePhone();
        ForRead();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone);

        asyncQuery = new myAsyncQueryHandler(getContentResolver());

        sendButton = (Button) findViewById(R.id.button_send);
        historyButton = (Button) findViewById(R.id.button_history);
        phoneEditText = (EditText) findViewById(R.id.phone_edittext);

        sendButton.setOnClickListener(this);
        historyButton.setOnClickListener(this);

        checkShouldUpdatePhone();
        ForRead();
    }


    private void ForRead() {
        ObjectInputStream ois = null;

        try {
            ois = new ObjectInputStream(new FileInputStream(Mutils.fullFile()));
            mDatas = (ArrayList) ois.readObject();
            if (mDatas == null){
                mDatas = new ArrayList<PhoneCallLog>();
            }
            Logger.d(mDatas.size());
        } catch (Exception e) {
            e.printStackTrace();
        }finally{
            try {
                if (ois != null)
                     ois.close();
            } catch (IOException e) {
            }

        }
    }

    void checkShouldUpdatePhone(){
        String key =  PhoneSharePreferences.getUserAccount() + "lastCallPhone";
        SharedPreferences sharePreferences = getSharedPreferences(key, Context.MODE_APPEND);
        String lastcallphone = sharePreferences.getString("lastCallPhone","");
        if (!TextUtils.isEmpty(lastcallphone)){
            Logger.d(lastcallphone);
            recordPhone(lastcallphone);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_send){
            if (TextUtils.isEmpty(phoneEditText.getText()) ||
                    !Mutils.isPhone(phoneEditText.getText().toString())){
                Toast.makeText(this,"请输入正确的电话号码",Toast.LENGTH_SHORT).show();
                return;
            }

            String key =  PhoneSharePreferences.getUserAccount() + "lastCallPhone";
            SharedPreferences sharePreferences = getSharedPreferences(key, Context.MODE_APPEND);
            sharePreferences.edit().putString("lastCallPhone",phoneEditText.getText().toString()).
                    putLong("lastCallPhoneTime",System.currentTimeMillis()).commit();

            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"+phoneEditText.getText().toString()));
            startActivity(intent);
        }
        else if(v.getId() == R.id.button_history){
            Intent intent = new Intent(this,CalllogActivity.class);
            intent.putExtra("mDatas",mDatas);
            startActivity(intent);
        }
    }

    private void recordPhone(String phone){
        Uri uri = CallLog.Calls.CONTENT_URI;
        String[] projection = {CallLog.Calls.DATE,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.DURATION};
        asyncQuery.startQuery(0, null, uri, projection, null, null,
                CallLog.Calls.DEFAULT_SORT_ORDER);
    }

    private class myAsyncQueryHandler extends AsyncQueryHandler{
        public myAsyncQueryHandler(ContentResolver contentResolver){
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            String key =  PhoneSharePreferences.getUserAccount() + "lastCallPhone";
            SharedPreferences sharePreferences = getSharedPreferences(key, Context.MODE_APPEND);
            String lastcallphone = sharePreferences.getString("lastCallPhone","");
            long lastcallphoneTime = sharePreferences.getLong("lastCallPhoneTime",0);

            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                for (int i = 0; i < cursor.getCount(); i++) {
                    cursor.moveToPosition(i);
                    int type = cursor.getInt(cursor
                            .getColumnIndex(CallLog.Calls.TYPE));
                    if (type == CallLog.Calls.OUTGOING_TYPE){
                        Date date = new Date(cursor.getLong(cursor
                                .getColumnIndex(CallLog.Calls.DATE)));
                        String number = cursor.getString(cursor
                                .getColumnIndex(CallLog.Calls.NUMBER));

                        String cachedName = cursor.getString(cursor
                                .getColumnIndex(CallLog.Calls.CACHED_NAME));// 缓存的名称与电话号码，如果它的存在
                        String duration = cursor.getString(cursor.getColumnIndex(CallLog.Calls.DURATION));
                        PhoneCallLog callLogBean = new PhoneCallLog();
                        callLogBean.setCallDate(date);
                        callLogBean.setPhoneNumber(number);
                        if (null == cachedName || TextUtils.isEmpty(cachedName)) {
                            callLogBean.setNickname(number);
                        }
                        else
                            callLogBean.setNickname(cachedName);
                        callLogBean.setCallduration(duration);
                        Logger.d(Math.abs(date.getTime() - lastcallphoneTime));
                        if (Math.abs(date.getTime() - lastcallphoneTime) <= 10000 &&
                                TextUtils.equals(lastcallphone,number)){
                            saveCalllog(callLogBean);
                            break;
                        }
                    }
                }
            }
            super.onQueryComplete(token, cookie, cursor);
        }

        private void saveCalllog(PhoneCallLog callLog){
            Logger.d(callLog);
            ObjectOutputStream outputStream = null;
            try {
                outputStream = new ObjectOutputStream(new FileOutputStream(Mutils.fullFile()));
                outputStream.reset();
                mDatas.add(0,callLog);
                outputStream.writeObject(mDatas);
                String key =  PhoneSharePreferences.getUserAccount() + "lastCallPhone";
                SharedPreferences sharePreferences = getSharedPreferences(key, Context.MODE_APPEND);
                sharePreferences.edit().remove("lastCallPhone").remove("lastCallPhoneTime").apply();
            }catch (IOException e){
                e.printStackTrace();
            }finally {
                try {
                    if (outputStream != null)
                       outputStream.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }
}
