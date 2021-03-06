package cn.chenny3.secondHand.common.bean.vo;

import java.util.HashMap;
import java.util.Map;

/**
 * VO
 * 用来包装视图层需渲染的数据源
 */
public class ViewObject {
    private Map<String,Object> map=new HashMap<String,Object>();


    public ViewObject put(String key, Object value){
        map.put(key,value);
        return this;
    }

    public Object get(String key){
        return map.get(key);
    }

    public static ViewObject build(int code,Object value){
        ViewObject viewObject = new ViewObject();
        viewObject.put("code",code);
        viewObject.put("value",value);
        return viewObject;
    }

}
