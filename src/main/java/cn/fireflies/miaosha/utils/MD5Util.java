package cn.fireflies.miaosha.utils;

import org.apache.commons.codec.digest.DigestUtils;

public class MD5Util {
    public static String md5(String src){
        return DigestUtils.md5Hex(src);
    }

    private static final String salt = "1a2b3c4d";

    public static String inputPassToFormPass(String inputPass){
        //加密过程
        String str = ""+salt.charAt(0) + salt.charAt(2) + inputPass + salt.charAt(5) + salt.charAt(1);
        return md5(str);
    }

    public static String formPassToDBPass(String formPass, String salt){
        //加密过程
        String str = ""+salt.charAt(0) + salt.charAt(2) + formPass + salt.charAt(5) + salt.charAt(1);
        return md5(str);
    }

    public static String inputPassToDBPass(String input, String saltDB){
        String formPass = inputPassToFormPass(input);
        String DBPass = formPassToDBPass(formPass,saltDB);
        return DBPass;
    }

    public static void main(String[] args) {
        System.out.println(inputPassToFormPass("123456"));//c75dc0342535653ceb6d410492d4f113
        System.out.println(inputPassToDBPass("123456","1a2b3c4d"));//a7a93ae37f4d4ec6ed2d2762282da6da
    }
}
