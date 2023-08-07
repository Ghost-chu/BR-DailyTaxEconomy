package com.ghostchu.plugins.dailytaxeconomy;

import java.util.UUID;

public class Util {
    public static boolean isUUID(String str){
        try{
            UUID.fromString(str);
            return true;
        }catch (IllegalArgumentException e){
            return false;
        }
    }
}
