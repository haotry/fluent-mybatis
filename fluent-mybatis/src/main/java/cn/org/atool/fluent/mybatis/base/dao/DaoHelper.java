package cn.org.atool.fluent.mybatis.base.dao;

import cn.org.atool.fluent.mybatis.base.IEntity;
import cn.org.atool.fluent.mybatis.base.crud.IUpdate;
import cn.org.atool.fluent.mybatis.base.model.SqlOp;
import cn.org.atool.fluent.mybatis.segment.BaseWrapper;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static cn.org.atool.fluent.mybatis.If.notNull;

public class DaoHelper {

    public static IUpdate buildUpdateEntityById(Supplier<IUpdate> supplier, IEntity entity) {
        IUpdate update = supplier.get();
        String primary = ((BaseWrapper) update).primary();
        Map<String, Object> map = entity.toColumnMap();
        boolean hasPrimaryId = false;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String column = entry.getKey();
            Object value = entry.getValue();
            if (Objects.equals(column, primary)) {
                if (notNull(value)) {
                    update.where().apply(primary, SqlOp.EQ, value);
                    hasPrimaryId = true;
                }
            } else {
                update.updateSet(column, value);
            }
        }
        if (!hasPrimaryId) {
            throw new RuntimeException("no primary value found.");
        } else {
            return update;
        }
    }

    /**
     * 根据entity非空字段构建update和where条件
     *
     * @param supplier
     * @param update
     * @param where
     * @return
     */
    public static IUpdate buildUpdateByEntityNoN(Supplier<IUpdate> supplier, IEntity update, IEntity where) {
        IUpdate updater = supplier.get();
        boolean hasUpdate = false;
        Map<String, Object> updateMap = update.toColumnMap();
        for (Map.Entry<String, Object> entry : updateMap.entrySet()) {
            Object value = entry.getValue();
            if (notNull(value)) {
                updater.updateSet(entry.getKey(), value);
                hasUpdate = true;
            }
        }
        if (!hasUpdate) {
            throw new RuntimeException("no update value found.");
        }
        boolean hasWhere = false;
        Map<String, Object> whereMap = where.toColumnMap();
        for (Map.Entry<String, Object> entry : whereMap.entrySet()) {
            Object value = entry.getValue();
            if (notNull(value)) {
                updater.where().apply(entry.getKey(), SqlOp.EQ, value);
                hasWhere = true;
            }
        }
        if (!hasWhere) {
            throw new RuntimeException("no where condition found.");
        }
        return updater;
    }
}