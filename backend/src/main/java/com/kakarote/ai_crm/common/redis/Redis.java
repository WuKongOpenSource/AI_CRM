package com.kakarote.ai_crm.common.redis;

import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author zhangzhiwei
 * redis简易封装,如有需要的方法不存在可以在此类中添加
 * 也可以使用RedisTemplate
 */
public interface Redis {

    /**
     * 删除key 可传多个key
     *
     * @param keys keys
     */
    void del(Object... keys);

    /**
     * 实现命令：TTL key，以秒为单位，返回给定 key的剩余生存时间(TTL, time to live)。
     *
     * @param key key
     * @return time
     */
    Long ttl(String key);

    /**
     * 给指定的key设置过期时间  单位为秒
     *
     * @param key     key
     * @param timeout 过期时间 单位是秒
     */
    void expire(String key, Integer timeout);


    /**
     * 移除key的过期时间，将key永久保存
     *
     * @param key key
     */
    void persist(String key);

    /**
     * 检验该key是否存在 存在返回true
     *
     * @param key key
     * @return 是否存在
     */
    boolean exists(String key);

    /**
     * 返回 key 所储存的值的类型
     *
     * @param key key
     * @return DataType
     */
    DataType getType(String key);


    /**
     * set字符串
     *
     * @param key   key
     * @param value value
     */
    void set(String key, Object value);

    /**
     * set字符串 并加上失效时间  以豪秒为单位
     *
     * @param key    key
     * @param value  value
     * @param second 失效时间 单位为秒
     */
    void setex(Object key, Integer second, Object value);

    /**
     * 当key不存在时 设置key value
     *
     * @param key     key
     * @param timeout 过期时间，秒为单位
     * @param value   value
     * @return boolean true为锁定成功
     */
    boolean setNx(String key, Long timeout, Object value);


    /**
     * 根据key获取value
     *
     * @param key key
     * @return value
     */
    <T> T get(String key);

    /**
     * 将 key 中储存的数字值增加 value。
     * 如果 key 不存在，那么 key 的值会先被初始化为 0 ，然后再执行 INCR 操作。
     * 如果值包含错误的类型，或字符串类型的值不能表示为数字，那么返回一个错误。
     * 本操作的值限制在 64 位(bit)有符号数字表示之内。
     * todo 本方法没有对key做过期处理，注意
     */
    Long incr(String key, Long value);

    /**
     * 将 key 中储存的数字值增加 value。
     * 如果 key 不存在，那么 key 的值会先被初始化为 0 ，然后再执行 INCR 操作。
     * 如果值包含错误的类型，或字符串类型的值不能表示为数字，那么返回一个错误。
     * 本操作的值限制在 64 位(bit)有符号数字表示之内。
     */
    Long incr(String key, Long time, TimeUnit timeUnit);

    /**
     * 将 key 中储存的数字值增一。
     * 如果 key 不存在，那么 key 的值会先被初始化为 0 ，然后再执行 INCR 操作。
     * 如果值包含错误的类型，或字符串类型的值不能表示为数字，那么返回一个错误。
     * 本操作的值限制在 64 位(bit)有符号数字表示之内。
     * todo 本方法没有对key做过期处理，注意
     */
    Long incr(String key);

    /**
     * 将 key 中储存的数字值减少 value。
     * 如果 key 不存在，那么 key 的值会先被初始化为 0 ，然后再执行 DECR 操作。
     * 如果值包含错误的类型，或字符串类型的值不能表示为数字，那么返回一个错误。
     * 本操作的值限制在 64 位(bit)有符号数字表示之内。
     * todo 本方法没有对key做过期处理，注意
     */
    Long decr(String key, Long value);

    /**
     * 将 key 中储存的数字值减一。
     * 如果 key 不存在，那么 key 的值会先被初始化为 0 ，然后再执行 DECR 操作。
     * 如果值包含错误的类型，或字符串类型的值不能表示为数字，那么返回一个错误。
     * 本操作的值限制在 64 位(bit)有符号数字表示之内。
     * todo 本方法没有对key做过期处理，注意
     */
    Long decr(String key);

    /**
     * 获取所有(一个或多个)给定 key 的值
     *
     * @param keys keys
     * @return values
     */
    List<Object> mGet(Object... keys);

    /**
     * 同时设置多个key，value
     *
     * @param map map
     */
    void mSet(Map<String, Object> map);

    /**
     * 所有给定的key都不存在时，设置多个key，value
     *
     * @param map map
     */
    void mSetNx(Map<String, Object> map);


    /**
     * 当前key存在时  向这个key对应value的默认追加上此字符
     *
     * @param key   key
     * @param value 要追加的字符
     */
    void appendStr(Object key, String value);


    /**
     * 删除key中指定的hashkey的值  相当于一个key中存储着map值 这个方法就是删除这个key中的map里面的一个或多个key
     *
     * @param key      key
     * @param hashKeys hashKeys
     */
    void hdel(Object key, Object... hashKeys);

    /**
     * 添加一个hash值
     *
     * @param key     key
     * @param hashKey 相当于 map中的key
     * @param value   存储的值 相当于map中的value
     */
    void put(Object key, Object hashKey, Object value);


    /**
     * 添加一个map
     *
     * @param key key
     * @param map map
     */
    void putAll(String key, Map<Object, Object> map);

    /**
     * 获取redis中的map
     *
     * @param key key
     * @return map
     */
    Map<Object, Object> getRedisMap(String key);

    /**
     * 返回这个key中的所有value
     *
     * @param key key
     * @return value
     */
    List<Object> getValues(Object key);

    /**
     * 判断key中的hashKey是否存在
     *
     * @param key     key
     * @param hashKey hashKey
     * @return exist
     */
    Boolean hashMapKey(Object key, String hashKey);

    /**
     * 从左入栈
     *
     * @param key   key
     * @param value value
     */
    void lpush(String key, Object value);

    /**
     * 从右入栈
     *
     * @param key   key
     * @param value value
     */
    void rpush(String key, Object value);


    /**
     * 从左出栈
     *
     * @param key key
     * @return value
     */
    @SuppressWarnings("unchecked")
    <T> T lPop(String key);

    /**
     * 从右出栈
     *
     * @param key key
     * @return value
     */
    @SuppressWarnings("unchecked")
    <T> T rPop(String key);


    /**
     * 获取该key index处的元素
     *
     * @param key   key
     * @param index index
     * @return value
     */

    <T> T getKeyIndex(String key, int index);

    /**
     * 获取列表的长度
     *
     * @param key key
     * @return length
     */
    Long getLength(String key);

    /**
     * 获取key中下标从start到end处的值
     *
     * @param key   key
     * @param start 开始下标
     * @param end   结束下标
     * @return values
     */
    List<Object> range(String key, int start, int end);


    //set操作  无序集合

    /**
     * 向集合中添加
     *
     * @param key    key
     * @param values values
     */
    void addSet(String key, Object... values);

    /**
     * 移除并取出第一个元素
     *
     * @param key key
     * @return value
     */

    <T> T getSet(String key);

    /**
     * 返回集合中所有的元素
     *
     * @param key key
     * @return values
     */
    Set<Object> getSets(String key);


    /**
     * 返回集合中的长度
     *
     * @param key key
     * @return length
     */
    Long getSetsNum(String key);

    /**
     * 返回集合中的所有元素
     *
     * @param key key
     * @return values
     */
    Set<Object> members(String key);


    //zSet操作 有序集合

    /**
     * 添加数据
     * <p>
     * 添加方式：
     * 1.创建一个set集合
     * Set<ZSetOperations.TypedTuple<Object>> sets=new HashSet<>();
     * 2.创建一个有序集合
     * ZSetOperations.TypedTuple<Object> objectTypedTuple1 = new DefaultTypedTuple<Object>(value,排序的数值，越小越在前);
     * 4.放入set集合
     * sets.add(objectTypedTuple1);
     * 5.放入缓存
     * reidsImpl.Zadd("zSet", list);
     *
     * @param key    key
     * @param tuples tuples
     */
    void zadd(String key, Set<ZSetOperations.TypedTuple<Object>> tuples);


    /**
     * 返回 分数在min至max之间的数据 按分数值递减(从大到小)的次序排列。
     *
     * @param key key
     * @param min min
     * @param max max
     * @return values
     */
    Set<Object> reverseRange(String key, Double min, Double max);

}
