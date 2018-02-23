package bchd.com.phone.model;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * phone
 * Created by VeronicaRen on 2017/6/15.
 */

public class CallInfo {
    public String number; // 号码
    public long date;     // 日期
    public int type;      // 类型：来电、去电、未接
    public long duration;

    private String success;

    public CallInfo(String number, long date, int type, long duration,String success) {
        this.number = number;
        this.date = date;
        this.type = type;
        this.duration = duration;
        this.success = success;
    }

    public JSONObject getCallJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("number", number);
            json.put("date", date);
            json.put("duration", duration);
            json.put("success",success);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i("post", "转换完成的" + json.toString());
        return json;
    }
}
