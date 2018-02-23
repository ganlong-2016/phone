package bchd.com.phone.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;

import java.util.ArrayList;
import java.util.List;

import bchd.com.phone.model.CallInfo;

/**
 * phone
 * Created by VeronicaRen on 2017/6/15.
 */

public class CallInfoQuery {
    /**
     * 获取通话记录
     *
     * @param context 上下文。通话记录需要从系统的【通话应用】中的内容提供者中获取，
     *                内容提供者需要上下文。通话记录保存在联系人数据库中：
     *                data/data/com.android.provider.contacts/databases/contacts2.db库中的calls表。
     * @return 包含所有通话记录的一个集合
     */
    public static List<CallInfo> getCallInfos(Context context) {
        List<CallInfo> infos = new ArrayList<CallInfo>();
        ContentResolver resolver = context.getContentResolver();
        // uri的写法需要查看源码JB\packages\providers\ContactsProvider\AndroidManifest.xml中内容提供者的授权
        // 从清单文件可知该提供者是CallLogProvider，且通话记录相关操作被封装到了Calls类中
        Uri uri = CallLog.Calls.CONTENT_URI;
        String[] projection = new String[]{
                CallLog.Calls.NUMBER, // 号码
                CallLog.Calls.DATE,   // 日期
                CallLog.Calls.TYPE,    // 类型：1来电、2去电、3未接
                CallLog.Calls.DURATION  // 通话时长
        };

        Cursor cursor = resolver.query(uri, projection, null, null, null);
        while (cursor.moveToNext()) {
            String number = cursor.getString(0);
            long date = cursor.getLong(1);
            int type = cursor.getInt(2);
            long duration = cursor.getInt(3);
            infos.add(new CallInfo(number, date, type, duration,"1"));
        }
        cursor.close();
        return infos;
    }
}
