package com.kakarote.ai_crm.common.enums;

/**
 * 授权权限类型枚举
 */
public enum RealmTypeEnum {

    DATA(1,"数据"),
    API(2,"API"),
    MENU(3,"菜单"),
    BUTTON(4,"按钮"),
    FUNCTION(5,"功能");

    RealmTypeEnum(int value, String name) {
        this.value = value;
        this.name = name;
    }

    private int value;
    private String name;

    public static String  parseName(int type){
        for(RealmTypeEnum value : RealmTypeEnum.values()){
            if(value.value == type){
                return value.name;
            }
        }
        return "";
    }


    public static int  valueOfType(String name){
        for(RealmTypeEnum value : RealmTypeEnum.values()){
            if(value.name.equals(name)){
                return value.value;
            }
        }
        return -1;
    }
    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }
}
