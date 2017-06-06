package bchd.com.phone.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ikomacbookpro on 2017/6/2.
 */

public class Mutils {
    final public static String baseurl = "http://box.bocaihd.com/api";

    public static  boolean isPhone(String str) {
        // 将给定的正则表达式编译并赋予给Pattern类
        Pattern pattern = Pattern.compile("1[0-9]{10}");
        // 对指定输入的字符串创建一个Matcher对象
        Matcher matcher = pattern.matcher(str);
        // 尝试对整个目标字符展开匹配检测,也就是只有整个目标字符串完全匹配时才返回真值.
        if (matcher.matches()) {
            return true;
        } else {
            return false;
        }
    }


    public static  boolean isPassword(String str) {
        // 将给定的正则表达式编译并赋予给Pattern类
        Pattern pattern = Pattern.compile("^[0-9A-Za-z]{6,20}$");
        // 对指定输入的字符串创建一个Matcher对象
        Matcher matcher = pattern.matcher(str);
        // 尝试对整个目标字符展开匹配检测,也就是只有整个目标字符串完全匹配时才返回真值.
        if (matcher.matches()) {
            return true;
        } else {
            return false;
        }
    }

    public static Intent createSingleTaskIntent(Context context, Class<?> cls){
        Intent i = new Intent(context, cls);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return i;
    }

    public static int dip2px(Context context,int dpValue){
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * @param timeStamp 时间戳（秒）
     * @param format 时间格式
     * <p>eg: "HH:mm:ss"; "yyyy/MM/dd"; "yyyy-MM-dd HH:mm" ....</p>
     * @return 格式化后的时间字符串
     */
    public static String getFormatDate(String timeStamp, String format){
        if(TextUtils.isEmpty(timeStamp)){
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        return sdf.format(new Date(Long.parseLong(timeStamp)*1000));
    }

    public static String bytes2HexString(byte[] bytes) {
        String ret = "";
        for (byte b : bytes) {
            // 将每个字节与0xFF进行与运算，然后转化为10进制，然后借助于Integer再转化为16进制
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) { //每个字节8为，转为16进制标志，2个16进制位
                hex = "0" + hex;
            }
            ret += hex.toUpperCase();
        }
        return ret;
    }

    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    public static String hexString2IntegerString(String hexString){
        if (TextUtils.isEmpty(hexString)) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i + 2 <= hexString.length(); i += 2){
            String cBit = hexString.substring(i, i + 2);
            int num = Integer.parseInt(cBit, 16);
            sb.append((char)num);
        }
        return sb.toString();
    }

    public static String integerString2HexString(String integerString){
        if (TextUtils.isEmpty(integerString)) return "";
        StringBuilder sb = new StringBuilder();
        char[] chars = integerString.toCharArray();
        for (char c : chars){
            String hex = Integer.toHexString((int)c);
            if (hex.length() == 1){
                hex = "0" + hex;
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    public static boolean isInBackground(Context context){
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> listAppProcessInfo = am.getRunningAppProcesses();
        if(listAppProcessInfo != null){
            final String strPackageName = context.getPackageName();
            for(ActivityManager.RunningAppProcessInfo pi : listAppProcessInfo){
                if(pi.processName.equals(strPackageName)){
                    if(pi.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                            pi.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE){
                        return true;
                    }else{
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static File fullFile(){
        File file = Environment.getExternalStorageDirectory();
        String filename = PhoneSharePreferences.getUserAccount() + "phonelogfile";
        File fullfile = new File(file,filename);
        return fullfile;
    }
}
