package com.project2.urlshortner.util;


public class Base62Encoder {

    private static final String CHARSET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int BASE = 62;

    public static String encode(Long num){
        StringBuilder sb = new StringBuilder();

        while(num > 0){
            int remainder = (int) (num % BASE);
            sb.append(CHARSET.charAt(remainder));
            num = num / BASE;
        }
        return sb.reverse().toString();
    }
}
