package bchd.com.phone;

import android.app.Application;

import com.orhanobut.logger.LogLevel;
import com.zhy.http.okhttp.OkHttpUtils;

import okhttp3.OkHttpClient;

/**
 * Created by ikomacbookpro on 2017/6/2.
 */

public class PhoneApplication extends Application {
    private static PhoneApplication sInstance;

    public static PhoneApplication getApplication(){
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10000L,   java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(10000L,  java.util.concurrent.TimeUnit.MILLISECONDS)
                .build();
        OkHttpUtils.initClient(okHttpClient);

        com.orhanobut.logger.Logger.init().logLevel(LogLevel.FULL);
    }
}
