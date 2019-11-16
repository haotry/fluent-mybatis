package cn.org.atool.fluent.mybatis.base;

import java.io.Serializable;
import java.util.Map;

/**
 * @author darui.wu
 */
public interface IEntity extends Serializable {
    /**
     * 返回实体主键
     *
     * @return 主键
     */
    Serializable findPk();

    /**
     * 将实体对象转换为map对象
     *
     * @return map对象
     */
    Map<String, Object> toMap();
}
