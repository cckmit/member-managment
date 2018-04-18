package com.mmiholdings.member.money.api.util;

/**
 * Created by prince on 9/4/17.
 */
public class SouthAfricanMobileFormatter {
    public static String addSouthAfricanCodeToPhoneNumber(String mobilePhoneNumber) {
        if (mobilePhoneNumber != null) {
            if (mobilePhoneNumber.startsWith("0"))
                return "+27" + mobilePhoneNumber.substring(1);
            else
                return mobilePhoneNumber;
        }
        return mobilePhoneNumber;
    }
}
