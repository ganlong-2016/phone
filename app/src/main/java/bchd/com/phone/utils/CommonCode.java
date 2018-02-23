package bchd.com.phone.utils;

/**
 * phone
 * Created by VeronicaRen on 2017/6/7.
 */

public class CommonCode {

    /**
     * 服务器地址
     */
    public static final String IP = "https://www.ywd360.com:8543";

    /**
     * 登录
     */
    public static final String URL_LOGIN = IP + "/appLogin/login";

    /**
     * 通话记录
     */
    public static final String URL_CALLLOG = IP + "/appLogin/callHistory";

    /**
     * 拨号
     */
    public static final String URL_DIAL = IP + "/appLogin/dialing";

    /**
     * 结束拨号
     */
    public static final String URL_OVER = IP + "/appLogin/endDialing";

    /**
     * 通话录音上传
     */
    public static final String URL_TAPE = IP + "/number/saveTape";

    /**
     * 检查更新地址
     */
    public static final String URL_UPDATE = IP + "/appLogin/getVersionNo";
}
