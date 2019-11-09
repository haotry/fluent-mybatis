package cn.org.atool.fluent.mybatis.test.and;

import cn.org.atool.fluent.mybatis.demo.generate.mapper.UserMapper;
import cn.org.atool.fluent.mybatis.demo.generate.query.UserEntityQuery;
import cn.org.atool.fluent.mybatis.test.BaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.test4j.hamcrest.matcher.string.StringMode;

public class AndObjectTest_Ge extends BaseTest {

    @Autowired
    private UserMapper mapper;

    @Test
    public void ge() {
        UserEntityQuery query = new UserEntityQuery()
                .and.age.ge(34);
        mapper.selectCount(query);
        db.sqlList().wantFirstSql().eq("SELECT COUNT( 1 ) FROM t_user WHERE (age >= ?)");
        db.sqlList().wantFirstPara().eqReflect(new Object[]{34});
    }

    @Test
    public void ge_condition() {
        UserEntityQuery query = new UserEntityQuery()
                .and.age.ge(true, 34);
        mapper.selectCount(query);
        db.sqlList().wantFirstSql().eq("SELECT COUNT( 1 ) FROM t_user WHERE (age >= ?)");
        db.sqlList().wantFirstPara().eqReflect(new Object[]{34});
    }

    @Test
    public void ge_supplier() {
        UserEntityQuery query = new UserEntityQuery()
                .and.age.ge(true, () -> 34);
        mapper.selectCount(query);
        db.sqlList().wantFirstSql()
                .eq("SELECT COUNT( 1 ) FROM t_user WHERE (age >= ?)", StringMode.SameAsSpace);
        db.sqlList().wantFirstPara().eqReflect(new Object[]{34});
    }

    @Test
    public void ge_IfNotNull() {
        UserEntityQuery query = new UserEntityQuery()
                .and.age.ge_IfNotNull(34);
        mapper.selectCount(query);
        db.sqlList().wantFirstSql().eq("SELECT COUNT( 1 ) FROM t_user WHERE (age >= ?)");
        db.sqlList().wantFirstPara().eqReflect(new Object[]{34});
    }

    @Test
    public void ge_predicate() {
        UserEntityQuery query = new UserEntityQuery()
                .and.age.ge((age) -> true, 34);
        mapper.selectCount(query);
        db.sqlList().wantFirstSql().eq("SELECT COUNT( 1 ) FROM t_user WHERE (age >= ?)");
        db.sqlList().wantFirstPara().eqReflect(new Object[]{34});
    }

    @Test
    public void ge_predicate_supplier() {
        UserEntityQuery query = new UserEntityQuery()
                .and.age.ge((age) -> true, () -> 34);
        mapper.selectCount(query);
        db.sqlList().wantFirstSql().eq("SELECT COUNT( 1 ) FROM t_user WHERE (age >= ?)");
        db.sqlList().wantFirstPara().eqReflect(new Object[]{34});
    }
}