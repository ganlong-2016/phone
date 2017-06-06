package bchd.com.phone.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by ikomacbookpro on 2017/6/5.
 */

public class PhoneCallLog implements Serializable{
    private String nickname;
    private String callduration;
    private Date callDate;
    private String phoneNumber;

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setCallduration(String callduration) {
        this.callduration = callduration;
    }

    public void setCallDate(Date callDate) {
        this.callDate = callDate;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getNickname() {
        return nickname;
    }

    public String getCallduration() {
        return callduration;
    }

    public Date getCallDate() {
        return callDate;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
