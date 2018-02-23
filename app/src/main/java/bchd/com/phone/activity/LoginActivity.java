package bchd.com.phone.activity;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import bchd.com.phone.BaseActivity;
import bchd.com.phone.BuildConfig;
import bchd.com.phone.R;
import bchd.com.phone.model.CallInfo;
import bchd.com.phone.utils.CallInfoQuery;
import bchd.com.phone.utils.CommonCode;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.Manifest.permission.CALL_PHONE;
import static android.Manifest.permission.READ_CALL_LOG;
import static android.Manifest.permission.READ_PHONE_STATE;


public class LoginActivity extends BaseActivity {

    private final String TAG = "LoginActivity";
    private EditText accountText;
    private EditText passwordEditText;

    private TextView tvVersionName;

    private Button loginButton;

    private CheckBox checkPassword;

    final String URL_LOGIN = CommonCode.IP + "/appLogin/login";

    final String URL_CALLLOG = CommonCode.IP + "/appLogin/callHistory";

    final String KEY_ACCOUNT = "loginname";

    final String KEY_PASS = "password";

    private Handler mHandler;

    private SharedPreferences preferences;

    private SharedPreferences.Editor editor;

    private String mToken;

    private Date mDateTime;

    private List<CallInfo> mCallList = new ArrayList<>();

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
    /**
     * 检查更新中
     */
    private ProgressDialog updateHint;
    /**
     * 下载提示
     */
    private ProgressDialog downloadHint;
    public static final int UPDATE_INFO = 10000;

    public static final int END_UPDATE = 10001;

    public static final int PROGRESS = 10002;

    public static final int NETWORK_AVAILABLE = 10003;

    private long downloadId = 0;
    String apkDir;

    private final String APK_NAME = "daochengphone.apk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        preferences = getSharedPreferences("user_info", MODE_PRIVATE);

        List<CallInfo> info = CallInfoQuery.getCallInfos(this);


        for (CallInfo call : info) {
            Log.i(TAG, "读取的记录：" + call.date);
        }

        accountText = (EditText) findViewById(R.id.account_edittext);
        passwordEditText = (EditText) findViewById(R.id.password_edittext);
        checkPassword = (CheckBox) findViewById(R.id.check_remember);

        tvVersionName = (TextView) findViewById(R.id.tv_version_name);

        tvVersionName.setText("版本号：" + BuildConfig.VERSION_NAME + "\n" + getMac());
        loginButton = (Button) findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
        loginButton.setEnabled(false);
        if (preferences.getString("user_name", null) != null || preferences.getString("user_pass", null) != null) {
            accountText.setText(preferences.getString("user_name", null));
            passwordEditText.setText(preferences.getString("user_pass", null));
            checkPassword.setChecked(true);
        }

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        loginButton.setText(R.string.btn_loging);
                        break;
                    case 2:
                        loginButton.setText(R.string.btn_login);
                        break;
                    case 3:
                        Log.i(TAG, "分支3");
                        List<CallInfo> tempList = CallInfoQuery.getCallInfos(LoginActivity.this);

                        JSONArray array = new JSONArray();

                        for (int i = 0; i < tempList.size(); i++) {
                            CallInfo tempInfo = tempList.get(i);
                            if (tempInfo.type == 2 && compareDate(mDateTime, new Date(tempInfo.date))) {
                                mCallList.add(tempInfo);
                                array.put(tempInfo.getCallJson());
                            }
                        }

                        OkHttpUtils.post()
                                .url(URL_CALLLOG)
                                .addParams("token", mToken)
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
                                        try {
                                            Thread.sleep(1000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        mHandler.post(postRecord);//上传通话录音文件
                                    }
                                });
                        Log.i(TAG, array.toString());
                        break;
                    case 4:
                        Log.i(TAG, "分支4");
                        List<CallInfo> tempList1 = CallInfoQuery.getCallInfos(LoginActivity.this);

                        JSONArray array1 = new JSONArray();

                        for (int i = 0; i < tempList1.size(); i++) {
                            CallInfo tempInfo = tempList1.get(i);
                            if (tempInfo.type == 2 && compareDate(mDateTime, new Date(tempInfo.date))) {
                                mCallList.add(tempInfo);
                                array1.put(tempInfo.getCallJson());
                                Log.i(TAG, "位置:" + i + " 服务器时间:" + mDateTime.getTime() + " 当前时间:" + new Date(tempInfo.date).getTime());
                            }
                        }

                        OkHttpUtils.post()
                                .url(URL_CALLLOG)
                                .addParams("token", mToken)
                                .addParams("jsonList", array1.toString())
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
                                        mHandler.post(postRecord);//上传通话录音文件
                                    }
                                });

                        Log.i(TAG, array1.toString());
                        break;
                    case UPDATE_INFO:
                        String info = (String) msg.obj;
                        update(info);
                        break;
                    case END_UPDATE:
                        endUpdate();
                        break;
//                    case PROGRESS:
//                        int progerss = (int) msg.obj;
//                        if (progerss==100&&downloadHint!=null){
//                            downloadHint.dismiss();
//                        }
//                        break;
                    default:
                        break;
                }
            }
        };


        checkUpdate();

    }



    private boolean requestPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        if (checkSelfPermission(READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
////                && checkSelfPermission(CALL_PHONE) == PackageManager.PERMISSION_GRANTED
////                && checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
//                && checkSelfPermission(WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
////                && checkSelfPermission(READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        ) {
            return true;
        }
//        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) { // 6.0之前的手机返回的都是true(清单文件配置了的前提下)
//            Toast.makeText(this, "Please grant the permission this time", Toast.LENGTH_LONG).show();
//        }
        requestPermissions(new String[]{
                CALL_PHONE,
                READ_CALL_LOG,
        }, 200);
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 200) {
            loginButton.setEnabled(true);
//            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//            } else {
//                Toast.makeText(this, "拒绝权限将导致某些功能失效！", Toast.LENGTH_SHORT).show();
//            }
        }
    }


    private void login() {
        Log.i(TAG, "MAC地址:" + getMac());

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

        mHandler.sendEmptyMessage(1);

        if (checkPassword.isChecked()) {
            saveInfo();
        } else {
            clearInfo();
        }

        if (TextUtils.isEmpty(accountText.getText()) && TextUtils.isEmpty(passwordEditText.getText())) {
            Toast.makeText(this, R.string.note_full_info, Toast.LENGTH_SHORT).show();
            mHandler.sendEmptyMessage(2);
        } else {
            OkHttpUtils.get().url(URL_LOGIN)
                    .addParams(KEY_ACCOUNT, accountText.getText().toString())
                    .addParams(KEY_PASS, passwordEditText.getText().toString())
                    .addParams("mac", getMac())
                    .build().execute(new StringCallback() {
                @Override
                public void onError(Call call, Exception e, int id) {
                    Log.i(TAG, e.toString());
                    Toast.makeText(LoginActivity.this, "发生错误：" + id, Toast.LENGTH_SHORT).show();
                    mHandler.sendEmptyMessage(2);
                }

                @Override
                public void onResponse(String response, int id) {
                    Log.i(TAG, response);
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        Log.i(TAG, "最后一次通话记录：" + response);
                        if (jsonObject.getBoolean("success")) {

                            mToken = jsonObject.getString("msg");
                            if (jsonObject.getLong("obj") == 0) {
                                mDateTime = new Date(jsonObject.getLong("obj"));
                                mHandler.sendEmptyMessage(4);
                                saveToken();
                                Intent intent = new Intent(LoginActivity.this, DialActivity.class);
                                intent.putExtra("token", jsonObject.getString("msg"));
                                intent.putExtra("date", jsonObject.getLong("obj"));
                                startActivity(intent);
                                finish();
                                return;
                            }
                            mDateTime = new Date(jsonObject.getLong("obj") + 1000);
                            saveToken();
                            Intent intent = new Intent(LoginActivity.this, DialActivity.class);
                            intent.putExtra("token", jsonObject.getString("msg"));
                            intent.putExtra("date", jsonObject.getLong("obj") + 1000);
                            startActivity(intent);
                            finish();
                            mHandler.sendEmptyMessage(2);
                            mHandler.sendEmptyMessage(3);
                        } else {
                            Toast.makeText(LoginActivity.this, jsonObject.getString("msg"), Toast.LENGTH_SHORT).show();
                            mHandler.sendEmptyMessage(2);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void saveInfo() {
        editor = preferences.edit();
        editor.putString("user_name", accountText.getText().toString());
        editor.putString("user_pass", passwordEditText.getText().toString());

        editor.apply();
    }

    private void clearInfo() {
        editor = preferences.edit();
        editor.putString("user_name", null);
        editor.putString("user_pass", null);
        editor.putString("user_token", null);
        editor.apply();
    }

    private void saveToken() {
        editor = preferences.edit();
        editor.putString("user_token", mToken);
        editor.apply();
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
            File recordFolder = new File(Environment.getExternalStorageDirectory() + "/record");//通话录音路径
            OSSCredentialProvider credentialProvider = new OSSPlainTextAKSKCredentialProvider(accessKeyId, accessKeySecret);
            ClientConfiguration conf = new ClientConfiguration();

            OSS oss = new OSSClient(getApplicationContext(), endpoint, credentialProvider);

            File[] files = recordFolder.listFiles();
            if (files != null && files.length > 0) {
                for (File f : files) {
                    if (f.isFile() && f.exists() && f.getName().endsWith(".amr")) {
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
                            Log.i(TAG, "TOKEN:" + mToken);

                            OkHttpUtils.get().url(CommonCode.URL_TAPE)
                                    .addParams("phone", phone)
                                    .addParams("ossPath", ossPath)
                                    .addParams("token", mToken)
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
     * 检查更新
     */
    private void checkUpdate() {

        updateHint = new ProgressDialog(this);
        updateHint.setTitle(R.string.label_check_update);
        updateHint.setCancelable(false);
        updateHint.show();
        try {
            Request request = new Request.Builder().url(CommonCode.URL_UPDATE).build();
            Call call = new OkHttpClient().newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "检查更新出现异常");
                    e.printStackTrace();
                    mHandler.sendEmptyMessage(END_UPDATE);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Message message = Message.obtain();
                    String info = response.body().string();
                    Log.i(TAG, "更新接口返回的信息:" + info);
                    message.what = UPDATE_INFO;
                    message.obj = info;
                    mHandler.sendMessage(message);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            mHandler.sendEmptyMessage(END_UPDATE);
        }
    }

    /**
     * 通过接口返回的信息进行更新
     *
     * @param info 接口返回的信息
     *             {"content":"asdasdasd","workItem":"empty","versionNo":"v.8.0"}
     */
    private void update(String info) {
        try {
            com.alibaba.fastjson.JSONObject versioninfo = com.alibaba.fastjson.JSON.parseObject(info);
            if (versioninfo != null && versioninfo.getString("versionNo") != null) {
                int newVersionCode = Integer.parseInt(
                        versioninfo.getString("versionNo").
                                replace("V", "").replace("v", "").replace(".", ""));
                String pkName = getPackageName();
                String versionName = getPackageManager().getPackageInfo(
                        pkName, 0).versionName;
                int oldVersionCode = Integer.parseInt(
                        versionName.replace("V", "").replace("v", "").replace(".", ""));
                if (newVersionCode > oldVersionCode &&
                        versioninfo.getString("workItem") != null
                        && !versioninfo.getString("workItem").equals("empty")) {
                    if (updateHint != null)
                        updateHint.dismiss();
                    downNewApk(versioninfo.getString("workItem"));
                } else {
                    mHandler.sendEmptyMessage(END_UPDATE);
                }
            } else {
                mHandler.sendEmptyMessage(END_UPDATE);
            }

        } catch (Exception e) {
            e.printStackTrace();
            mHandler.sendEmptyMessage(END_UPDATE);
        }
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent2) {
            if (downloadId == intent2.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)) {
                unregisterReceiver(receiver);
                File file = new File(apkDir);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                } else {
                    downloadHint.dismiss();
                    Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".updatefileprovider", file);
                    intent.setDataAndType(uri, "application/vnd.android.package-archive");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                mHandler.sendEmptyMessage(END_UPDATE);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                System.exit(0);

            }
        }
    };

    /**
     * 下载APK
     *
     * @param installPath apk地址
     */
    private void downNewApk(String installPath) {

//        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_network_state, null);
//        TextView dialogText = (TextView) dialogView.findViewById(R.id.dialog_network_tv_state);
//        dialogText.setText(getText(R.string.label_download_update));
        downloadHint = new ProgressDialog(this);
        downloadHint.setTitle(R.string.label_download_update);
        downloadHint.setCancelable(false);
        downloadHint.show();
        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

//        apkDir = Environment.getExternalStorageDirectory() + File.separator+"download"+File.separator + APK_NAME;
        apkDir =getExternalCacheDir()+File.separator+APK_NAME;
        File fileSave = new File(apkDir);
        fileSave.delete();
        Uri uri = Uri.parse(installPath);
        DownloadManager.Request req = new DownloadManager.Request(uri);
        req.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String mimeString = mimeTypeMap.getMimeTypeFromExtension(MimeTypeMap
                .getFileExtensionFromUrl(installPath));
//        req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, APK_NAME);
        req.setMimeType(mimeString);
        req.setVisibleInDownloadsUi(true);
        req.setDestinationUri(Uri.fromFile(new File(apkDir)));
        req.setTitle(getString(R.string.label_download_update));
        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        downloadId = dm.enqueue(req);

    }


    /**
     * 检查更新结束
     */
    private void endUpdate() {
        if (updateHint != null) {
            updateHint.dismiss();
        }
        if (requestPermission()) {
            loginButton.setEnabled(true);
        }
    }
}
