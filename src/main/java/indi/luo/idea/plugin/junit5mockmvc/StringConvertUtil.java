package indi.luo.idea.plugin.junit5mockmvc;

import com.google.common.base.CaseFormat;

import java.util.ArrayList;
import java.util.List;


public class StringConvertUtil {
    public static void main(String[] args) {
        System.out.println(StringConvertUtil.lowerCamel2LowerUnderscore("aaBbCc"));
    }

    public static  String lowerUnderscore2LowerCamel(String tableName) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, tableName.toLowerCase());
    }
    public static  String lowerCamel2LowerUnderscore(String name) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name);
    }

    public static  String lowerUnderscore2UpperCamel(String tableName) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, tableName.toLowerCase());
    }
    public static  String lowerHyphen2UpperCamel(String tableName) {
        return CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, tableName.toLowerCase());
    }
    public static  String lowerCamel2UpperCamel(String lowerCamel) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, lowerCamel);
    }
    public static  String UpperCamel2LowerCamel(String lowerCamel) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, lowerCamel);
    }
    public static  String UpperCamel2lowerUnderscore(String lowerCamel) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, lowerCamel);
    }

    public static String packageConvertPath(String packageName) {
        return String.format("%s/", packageName.contains(".") ? packageName.replaceAll("\\.", "/") : packageName);
    }
}
