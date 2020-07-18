package cn.org.atool.fluent.mybatis.test.and;

import cn.org.atool.fluent.mybatis.demo.generate.helper.UserMapping;
import cn.org.atool.fluent.mybatis.demo.generate.mapper.UserMapper;
import cn.org.atool.fluent.mybatis.demo.generate.wrapper.UserQuery;
import cn.org.atool.fluent.mybatis.test.BaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class OrderTest extends BaseTest {
    @Autowired
    private UserMapper mapper;

    @Test
    public void order() {
        UserQuery query = new UserQuery()
            .where.userName().like("user").end()
            .orderBy.id().asc()
            .addressId().desc()
            .desc("user_name", "id+0").end();
        mapper.listEntity(query);
        db.sqlList().wantFirstSql().where().eq("user_name LIKE ?");
        db.sqlList().wantFirstSql().end("ORDER BY id ASC, address_id DESC, user_name DESC, id+0 DESC");
    }

    @Test
    public void order2() {
        UserQuery query = new UserQuery()
            .where.userName().like("user").end()
            .orderBy
            .id().asc()
            .asc("address_id")
            .userName().desc()
            .asc("id+0")
            .end();
        mapper.listEntity(query);
        db.sqlList().wantFirstSql().where().eq("user_name LIKE ?");
        db.sqlList().wantFirstSql().end("ORDER BY id ASC, address_id ASC, user_name DESC, id+0 ASC");
    }

    @Test
    public void orderBy_condition() {
        UserQuery query = new UserQuery()
            .where.userName().like("user").end()
            .orderBy
            .apply(true, false, UserMapping.id, UserMapping.addressId)
            .apply(false, true, UserMapping.userName)
            .asc("id+0")
            .end();
        mapper.listEntity(query);
        db.sqlList().wantFirstSql().end("ORDER BY id DESC, address_id DESC, id+0 ASC");
    }
}