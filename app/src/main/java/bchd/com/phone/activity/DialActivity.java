package bchd.com.phone.activity;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.CallLog;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import bchd.com.phone.BaseActivity;
import bchd.com.phone.R;
import bchd.com.phone.model.CallInfo;
import bchd.com.phone.utils.CallInfoQuery;
import bchd.com.phone.utils.CommonCode;
import okhttp3.Call;
import okhttp3.Response;

public class DialActivity extends BaseActivity {

    private final String TAG="DialActivity";
    protected TextView tvWaiting;

    protected Button btnRecall;

    private Handler mHandler;

    private String token;

    final String URL_CALLLOG = CommonCode.IP + "/appLogin/callHistory";

    final String URL_LAST_CALL = CommonCode.IP + "/appLogin/endCallHistory";

    private List<CallInfo> mCallList = new ArrayList<>();

    private Date mDateTime;

    /**
     * 录音号码
     */
    private String phone;

    /**
     * 录音时间
     */
    private String createTime;

    /**
     * 录音阿里云路径
     */
    private String ossPath;

    /**
     * 当前录音文件
     */
    private File currentFile;

    final String URL_DIAL = CommonCode.IP + "/appLogin/dialing";

    final String URL_OVER = CommonCode.IP + "/appLogin/endDialing";

    final String URL_CONTINUED = CommonCode.IP + "/appLogin/continuedDialing";

    private String callNumber;

    /**
     * 开始拨打电话的时间戳
     */
    private long startCallTime;

    /**
     * 结束拨打电话的时间戳
     */
    private long endCallTime;
    private boolean isConnect = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dial);

        TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        manager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
        tvWaiting = (TextView) findViewById(R.id.dial_tv_waiting);

        btnRecall = (Button) findViewById(R.id.dial_btn_recall);

        token = getIntent().getStringExtra("token");
        mDateTime = new Date(getIntent().getLongExtra("date", 0));

        Animation animation = new AlphaAnimation(1, 0);
        animation.setDuration(800);
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.REVERSE);
        tvWaiting.setAnimation(animation);

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {

                }
            }
        };

        mHandler.post(questDial);

        btnRecall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnRecall.setVisibility(View.GONE);
//                uplodeCallHistory();
//                mHandler.post(queryLastCall);
                //上报正在执行轮询的状态
                OkHttpUtils.get().url(URL_CONTINUED)
                        .addParams("token", token)
                        .build().execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {

                    }

                    @Override
                    public void onResponse(String response, int id) {
                        Log.i(TAG, "onResponse: 上报正在执行轮询的状态" + response);
                    }
                });
                mHandler.post(questDial);
            }
        });

    }


    /**
     * 轮询通话
     */
    private Runnable questDial = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "执行轮询通话");
            Log.i(TAG, "run: token"+token);
            OkHttpUtils.get().url(URL_DIAL).addParams("token", token)
                    .build().execute(new StringCallback() {
                @Override
                public void onError(Call call, Exception e, int id) {
                    Log.i(TAG, e.toString());
                    Toast.makeText(DialActivity.this, "发生错误：" + e.toString(), Toast.LENGTH_SHORT).show();
                    mHandler.removeCallbacks(questDial);
                }

                @Override
                public void onResponse(String response, int id) {
                    Log.i(TAG, response);
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if (jsonObject.getBoolean("success")) {
                            mHandler.removeCallbacks(questDial);
                            callNumber = jsonObject.getString("msg");
                            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + callNumber));
                            startActivityForResult(intent, 1);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
            mHandler.postDelayed(questDial, 3000);
        }
    };

    private void getCallLogState() {
//        btnRecall.setVisibility(View.VISIBLE);
        ContentResolver cr = getContentResolver();
        final Cursor cursor = cr.query(CallLog.Calls.CONTENT_URI,
                new String[]{CallLog.Calls.NUMBER, CallLog.Calls.TYPE,
                        CallLog.Calls.DURATION, CallLog.Calls.DATE},
                CallLog.Calls.NUMBER + "=? and " +
                        CallLog.Calls.TYPE + "= ? or " + CallLog.Calls.TYPE + "= ?",
                new String[]{callNumber, "2", "3"}, "_id desc limit 1");
        while (cursor.moveToNext()) {
            int durationIndex = cursor.getColumnIndex(CallLog.Calls.DURATION);
            long durationTime = cursor.getLong(durationIndex);
            int dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE);
            long date = cursor.getLong(dateIndex);
            long duration = (endCallTime - startCallTime) / 1000;
            if (durationTime == 0) {
                isConnect = false;
            } else if (duration - durationTime > 3) {
                isConnect = true;
            } else {
                isConnect = false;
            }
        }
        cursor.close();
//        OkHttpUtils.post()
//                .url(URL_OVER)
//                .addParams("token", token)
//                .addParams("success",isConnect?"0":"1")
//                .build().execute(new StringCallback() {
//            @Override
//            public void onError(Call call, Exception e, int id) {
//                Log.i("callback", e.toString());
//            }
//
//            @Override
//            public void onResponse(String response, int id) {
//                Log.i("dial","response--->"+response);
//            }
//        });
//        Log.i("callback", "电话打完了");

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "RESULT");
        if (requestCode == 1) {

        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
//        startActivity(new Intent(this, LoginActivity.class));
        mHandler.removeCallbacks(questDial);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);
        }
        return super.onKeyDown(keyCode, event);
    }

    private final String bucketName = "daorongoss01";
    private final String endpoint = "oss-cn-shanghai.aliyuncs.com";
    private final String accessKeyId = "LTAIX4Fbg8shLCPW";
    private final String accessKeySecret = "dm9wmxyZlfUUflqLcJeiOUON63BDc1";

    private StringCallback postRecordCallback = new StringCallback() {
        @Override
        public void onError(Call call, Exception e, int id) {
            Log.e(TAG, e.toString());
        }

        @Override
        public void onResponse(String response, int id) {
            try {
                if (response.contains("200")) {
                    if (currentFile.exists()) {
                        currentFile.delete();
                    }
                }
                Log.i(TAG, response);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
    };

    /**
     * 上传录音文件
     */
    private Runnable postRecord = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "执行上传录音");
            File recordFolder = new File(Environment.getExternalStorageDirectory() + "/record");//通话录音路径
            OSSCredentialProvider credentialProvider = new OSSPlainTextAKSKCredentialProvider(accessKeyId, accessKeySecret);
            ClientConfiguration conf = new ClientConfiguration();

            OSS oss = new OSSClient(getApplicationContext(), endpoint, credentialProvider);

            File[] files = recordFolder.listFiles();
            if (files != null && files.length > 0) {
                for (File f : files) {
                    if (f.exists() && f.isFile() && f.getName().endsWith(".amr")) {
                        try {
                            currentFile = f;
                            String fileName = f.getName();
                            String operateName = UUID.randomUUID() + fileName.substring(fileName.lastIndexOf("."), fileName.length());
                            ossPath = "https://" + bucketName + "." + endpoint + "/" + operateName;
                            PutObjectRequest put = new PutObjectRequest(bucketName, operateName, f.getPath());
                            PutObjectResult putResult = oss.putObject(put);

                            if (f.getName().contains("@")) {
                                String[] nameArray = f.getName().split("@");
                                nameArray = nameArray[1].split("_");
                                phone = nameArray[0];
                                createTime = nameArray[1].replace(".amr", "");
                            } else if (!f.getName().contains("@")) {
                                String[] nameArray = f.getName().split("_");
                                phone = nameArray[0].replace(" ", "");
                                createTime = nameArray[1];
                            }

                            Log.i(TAG, "路径:" + ossPath);
                            Log.i(TAG, "录音电话号码:" + phone);
                            Log.i(TAG, "录音时间:" + createTime);
                            Log.i(TAG, "TOKEN:" + token);

                            OkHttpUtils.get().url(CommonCode.URL_TAPE)
                                    .addParams("phone", phone)
                                    .addParams("ossPath", ossPath)
                                    .addParams("token", token)
                                    .addParams("createTime", createTime)
                                    .build().execute(postRecordCallback);
                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        } finally {
                            if (f.exists()) {
                                f.delete();
                            }
                        }
                    }
                }
            }
        }
    };

    /**
     * 上传通话记录信息
     */
    private void uplodeCallHistory() {

        CallInfo info = null;
        ContentResolver cr = getContentResolver();
        final Cursor cursor = cr.query(CallLog.Calls.CONTENT_URI,
                new String[]{CallLog.Calls.NUMBER, CallLog.Calls.TYPE,
                        CallLog.Calls.DURATION, CallLog.Calls.DATE},
                CallLog.Calls.NUMBER + "=? and " +
                        CallLog.Calls.TYPE + "= ? or " + CallLog.Calls.TYPE + "= ?",
                new String[]{callNumber, "2", "3"}, "_id desc limit 1");
        while (cursor.moveToNext()) {
            int numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
            String number = cursor.getString(numberIndex);
            int dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE);
            long date = cursor.getLong(dateIndex);
            int durationIndex = cursor.getColumnIndex(CallLog.Calls.DURATION);
            long duration = cursor.getLong(durationIndex);
            int typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE);
            int type = cursor.getInt(typeIndex);


            long durationTime = (endCallTime - startCallTime) / 1000;
            if (duration == 0) {
                isConnect = false;
            } else if (durationTime - duration > 3) {
                isConnect = true;
            } else {
                isConnect = false;
            }
            info = new CallInfo(number, date, type, duration, isConnect ? "0" : "1");
        }
        JSONArray array = new JSONArray();
        array.put(info.getCallJson());
        OkHttpUtils.post()
                .url(URL_CALLLOG)
                .addParams("token", token)
                .addParams("jsonList", array.toString())
                .build()
                .execute(new com.zhy.http.okhttp.callback.Callback() {
                    @Override
                    public Object parseNetworkResponse(Response response, int id) throws Exception {
                        return null;
                    }

                    @Override
                    public void onError(Call call, Exception e, int id) {
                        Log.e(TAG, e.toString() + "----id:" + id);
                    }

                    @Override
                    public void onResponse(Object response, int id) {
                        Log.i(TAG, "update success");
                    }
                });
    }

    /**
     * 比较时间前后
     *
     * @param d1
     * @param d2
     * @return
     */
    private boolean compareDate(Date d1, Date d2) {
        return d1.getTime() < d2.getTime();
    }

    /**
     * 手机状态监听
     */
    private boolean isOutgoing = false;
    private boolean isIncoming = false;
    PhoneStateListener listener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            switch (state) {
                //无状态或者挂断电话的状态
                case TelephonyManager.CALL_STATE_IDLE:
                    if (isIncoming) {
                        isIncoming = false;
                        return;
                    }
                    if (isOutgoing) {
                        isOutgoing = false;
                        endCallTime = System.currentTimeMillis();
                        Log.i(TAG, "onCallStateChanged: 挂断电话时间--->" + endCallTime);
                        uploadCallInfo();
//                        getCallLogState();
                    }
                    break;
                //响铃状态，（有电话呼入）
                case TelephonyManager.CALL_STATE_RINGING:
                    isIncoming = true;
                    break;
                //开始拨打电话（呼出） 或者接来电（呼入）
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    if (isIncoming) return;
                    if (isOutgoing) return;
                    isOutgoing = true;
                    startCallTime = System.currentTimeMillis();
                    Log.i(TAG, "onCallStateChanged: 开始拨打时间--->" + startCallTime);
                    break;
                default:
                    break;
            }
        }
    };
    private ProgressDialog dialog;
    private void uploadCallInfo() {
//        dialog = new ProgressDialog(this);
//        dialog.setTitle("上传通话记录中，请稍等...");
//        dialog.setCancelable(false);
//        dialog.show();
        btnRecall.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                uplodeCallHistory();
                new Thread(postRecord).start();

                OkHttpUtils.post()
                        .url(URL_OVER)
                        .addParams("token", token)
//                    .addParams("success",isConnect?"0":"1")
                        .build().execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {
                        Log.i(TAG, e.toString());
                    }

                    @Override
                    public void onResponse(String response, int id) {
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                dialog.dismiss();
//
//                            }
//                        });
                    }
                });
                Log.i(TAG, "电话打完了");

            }
        }).start();

    }
}
