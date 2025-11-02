package com.tbw.cut.bilibili;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class BilibiliApiResponse {
    private int code;
    private String message;
    private int ttl;
    private JSONObject data;
    
    /**
     * 检查请求是否成功
     * @return 是否成功
     */
    public boolean isSuccess() {
        return code == 0;
    }
    
    /**
     * 获取数据对象
     * @return 数据对象
     */
    public JSONObject getData() {
        return data;
    }
    
    /**
     * 根据键获取数据值
     * @param key 键
     * @return 值
     */
    public Object getDataValue(String key) {
        if (data != null) {
            return data.get(key);
        }
        return null;
    }
    
    /**
     * 根据键获取字符串值
     * @param key 键
     * @return 字符串值
     */
    public String getDataString(String key) {
        Object value = getDataValue(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * 根据键获取整数值
     * @param key 键
     * @return 整数值
     */
    public Integer getDataInteger(String key) {
        Object value = getDataValue(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    /**
     * 根据键获取长整数值
     * @param key 键
     * @return 长整数值
     */
    public Long getDataLong(String key) {
        Object value = getDataValue(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}