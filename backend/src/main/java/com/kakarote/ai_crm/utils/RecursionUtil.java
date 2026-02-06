package com.kakarote.ai_crm.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.map.MapProxy;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ReflectUtil;
import com.kakarote.ai_crm.common.Const;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author zhangzhiwei
 * 递归相关的util包
 */
public class RecursionUtil {

    /**
     * 递归查询列表
     *
     * @param allList    总数据
     * @param parentName 上级的key
     * @param parentId   上级的值
     * @param idName     本级ID的key
     * @param <R>        list类型
     * @return
     */
    public static <R> List<R> getChildList(List<R> allList, String parentName, Object parentId, String idName) {
        return getChildList(allList, parentName, parentId, idName, null, Const.AUTH_DATA_RECURSION_NUM);
    }

    /**
     * 递归查询列表
     *
     * @param allList    总数据
     * @param parentName 上级的key
     * @param parentId   上级的值
     * @param idName     本级ID的key
     * @param returnName 返回的类型 null代表返回对象类型
     * @param <R>        list类型
     * @param <T>        返回类型
     * @return
     */
    public static <R, T> List<T> getChildList(List<R> allList, String parentName, Object parentId, String idName, String returnName) {
        return getChildList(allList, parentName, parentId, idName, returnName, Const.AUTH_DATA_RECURSION_NUM);
    }

    /**
     * 递归查询列表
     *
     * @param allList    总数据
     * @param parentName 上级的key
     * @param parentId   上级的值
     * @param idName     本级ID的key
     * @param returnName 返回的类型,null代表返回对象类型
     * @param depth      最大递归层级
     * @param <R>
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <R, T> List<T> getChildList(List<R> allList, String parentName, Object parentId, String idName, String returnName, Integer depth) {
        depth--;
        List<T> arrList = new ArrayList<>();
        //超过指定递归次数直接返回
        if (depth < 0) {
            return arrList;
        }
        for (R r : allList) {
            //如果bean为map类型
            if (r instanceof Map) {
                MapProxy proxy = MapUtil.createProxy((Map<?, ?>) r);
                //如果数据符合要求，继续往下递归
                if (Objects.equals(parentId, proxy.get(parentName))) {
                    if (returnName == null) {
                        arrList.add((T) r);
                    } else {
                        arrList.add((T) proxy.get(returnName));
                    }
                    arrList.addAll(getChildList(allList, parentName, proxy.get(idName), idName, returnName, depth));
                }
            } else {
                //获取ID字段和上级字段
                Field idField = ReflectUtil.getField(r.getClass(), idName), parentField = ReflectUtil.getField(r.getClass(), parentName);
                //字段不存在直接抛异常
                if (idField == null || parentField == null) {
                    throw new BusinessException(SystemCodeEnum.SYSTEM_NO_SUCH_PARAMENT_ERROR);
                }
                //如果数据符合要求，继续往下递归
                if (Objects.equals(parentId, ReflectUtil.getFieldValue(r, parentField))) {
                    if (returnName == null) {
                        arrList.add((T) r);
                    } else {
                        arrList.add((T) ReflectUtil.getFieldValue(r, returnName));
                    }
                    arrList.addAll(getChildList(allList, parentName, ReflectUtil.getFieldValue(r, idField), idName, returnName, depth));
                }
            }

        }
        return arrList;
    }

    /**
     * 递归查询列表数
     *
     * @param allList    总数据
     * @param parentName 上级的key
     * @param parentId   上级的值
     * @param idName     本级ID的key
     * @param treeName   子级树名称
     * @param <R>        list类型
     * @return
     */
    public static <R, T> List<T> getChildListTree(List<R> allList, String parentName, Object parentId, String idName, String treeName, Class<T> clazz) {
        return getChildListTree(allList, parentName, parentId, idName, treeName, clazz, Const.AUTH_DATA_RECURSION_NUM);
    }

    /**
     * 递归查询列表树
     *
     * @param allList    总数据
     * @param parentName 上级的key
     * @param parentId   上级的值
     * @param idName     本级ID的key
     * @param treeName   子级树名称
     * @param depth      最大递归层级
     * @return data
     */

    public static <R, T> List<T> getChildListTree(List<R> allList, String parentName, Object parentId, String idName, String treeName, Class<T> clazz, Integer depth) {
        depth--;
        List<T> arrList = new ArrayList<>();
        if (depth < 0) {
            return arrList;
        }
        for (R data : allList) {
            //获取ID字段和上级字段
            Field idField = ReflectUtil.getField(data.getClass(), idName), parentField = ReflectUtil.getField(data.getClass(), parentName);
            //字段不存在直接抛异常
            if (idField == null || parentField == null) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_SUCH_PARAMENT_ERROR);
            }
            if (Objects.equals(parentId, ReflectUtil.getFieldValue(data, parentField))) {
                T properties = BeanUtil.copyProperties(data, clazz);
                ReflectUtil.setFieldValue(properties,treeName,getChildListTree(allList, parentName, ReflectUtil.getFieldValue(data,idField), idName, treeName, clazz, depth));
                arrList.add(properties);
            }
        }
        return arrList;
    }

}
