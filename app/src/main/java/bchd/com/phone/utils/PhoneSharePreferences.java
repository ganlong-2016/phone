package bchd.com.phone.utils;

import android.app.Activity;
import android.content.SharedPreferences;

import bchd.com.phone.PhoneApplication;


/**
 * Created by ikomacbookpro on 2017/6/2.
 */

public class PhoneSharePreferences {
    private 	static final String Login_Info				=	"logininfo";
    private 	static final String Key_Account				=	"account";
    private 	static final String Key_Password			=	"password";
    private 	static final String Key_ID					=	"uid";
    private 	static final String Key_Name				=	"name";
    private 	static final String Key_Avatar				=	"avatar";
    private 	static final String Key_IsLoginSuccess		=	"islogin";

    public static void saveLoginInfo(String account, String password, String uid){
        defaultSp().edit().putString(Key_Account, account).putString(Key_Password, password).putString(Key_ID, uid).apply();
    }

    public static void savePassword(String password){
        defaultSp().edit().putString(Key_Password,password);
    }

    public static void setLoginSuccess(){
        defaultSp().edit().putBoolean(Key_IsLoginSuccess, true).apply();
    }

    public static boolean isLoginSuccess(){
        return defaultSp().getBoolean(Key_IsLoginSuccess, false);
    }

    public static void cleanPassword(){
        defaultSp().edit().remove(Key_Password).remove(Key_ID).remove(Key_Name).remove(Key_Avatar).remove(Key_IsLoginSuccess).apply();
    }

    public static void cleanLoginInfo(){
        defaultSp().edit().remove(Key_Account).remove(Key_Password).remove(Key_ID).remove(Key_Name).remove(Key_Avatar).remove(Key_IsLoginSuccess).apply();
    }

    public static String getUserAccount(){
        return defaultSp().getString(Key_Account, "");
    }

    public static String getUserPassword(){
        return defaultSp().getString(Key_Password, "");
    }

    public static String getUserId(){
        return defaultSp().getString(Key_ID, "");
    }

    private static SharedPreferences defaultSp(){
        return PhoneApplication.getApplication().getSharedPreferences(Login_Info, Activity.MODE_PRIVATE);
    }

}
