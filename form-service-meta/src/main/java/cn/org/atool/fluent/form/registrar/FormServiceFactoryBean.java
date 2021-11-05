package cn.org.atool.fluent.form.registrar;

import cn.org.atool.fluent.form.IMethodAround;
import cn.org.atool.fluent.form.annotation.FormMethod;
import cn.org.atool.fluent.form.annotation.FormService;
import cn.org.atool.fluent.form.annotation.MethodType;
import cn.org.atool.fluent.form.meta.EntryMetas;
import cn.org.atool.fluent.form.meta.MethodMeta;
import cn.org.atool.fluent.form.meta.NoMethodAround;
import cn.org.atool.fluent.form.setter.FormHelper;
import cn.org.atool.fluent.mybatis.If;
import cn.org.atool.fluent.mybatis.base.IEntity;
import cn.org.atool.fluent.mybatis.base.crud.IQuery;
import cn.org.atool.fluent.mybatis.base.crud.IUpdate;
import cn.org.atool.fluent.mybatis.base.entity.AMapping;
import cn.org.atool.fluent.mybatis.base.model.KeyMap;
import cn.org.atool.fluent.mybatis.model.StdPagedList;
import cn.org.atool.fluent.mybatis.model.TagPagedList;
import cn.org.atool.fluent.mybatis.utility.RefKit;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.cglib.proxy.InvocationHandler;

import java.lang.reflect.Method;
import java.util.List;

import static org.springframework.cglib.proxy.Proxy.newProxyInstance;

/**
 * FormServiceFactoryBean: FormService bean封装工厂
 *
 * @author darui.wu
 */
@SuppressWarnings({"unused", "unchecked", "rawtypes"})
public class FormServiceFactoryBean implements FactoryBean {
    public static final KeyMap<Class> TableEntityClass = new KeyMap();

    private final Class serviceClass;

    private final IMethodAround methodAround;

    private final FormService api;

    private Class entityClass;

    public FormServiceFactoryBean(Class serviceClass, Class aroundClass) {
        this.serviceClass = serviceClass;
        this.api = (FormService) serviceClass.getDeclaredAnnotation(FormService.class);
        this.methodAround = this.aroundInstance(aroundClass);
    }

    @Override
    public Class<?> getObjectType() {
        return this.serviceClass;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public Object getObject() {
        ClassLoader classLoader = this.serviceClass.getClassLoader();
        return newProxyInstance(classLoader, new Class[]{this.serviceClass}, this::invoke);
    }

    /**
     * FactoryBean的 {@link InvocationHandler#invoke(Object, Method, Object[])} 实现
     */
    private Object invoke(Object target, Method method, Object[] args) throws Throwable {
        if (Object.class.equals(method.getDeclaringClass()) || method.isDefault()) {
            return method.invoke(this, args);
        }
        Class eClass = this.getEntityClass(method);
        MethodMeta aMeta = this.methodAround.before(eClass, method, args);
        try {
            Object result = this.doInvoke(aMeta);
            return this.methodAround.after(eClass, method, result);
        } catch (RuntimeException e) {
            return this.methodAround.after(eClass, method, e);
        }
    }

    /**
     * 执行Form操作
     *
     * @param method 方法执行行为
     * @return 执行结果
     */
    private Object doInvoke(MethodMeta method) {
        EntryMetas metas = method.metas();
        if (method.methodType == MethodType.Save) {
            return save(method, metas);
        } else if (method.methodType == MethodType.Update) {
            return update(method, metas);
        } else {
            return query(method, metas);
        }
    }

    /**
     * 返回要操作的表EntityClass
     *
     * @param method 方法
     * @return EntityClass
     */
    private Class<? extends IEntity> getEntityClass(Method method) {
        FormMethod aMethod = method.getDeclaredAnnotation(FormMethod.class);
        if (aMethod == null) {
            return this.getEntityClass();
        }
        Class entity = this.getEntityClass(aMethod.entity(), aMethod.table());
        if (entity == null || entity == Object.class) {
            entity = this.getEntityClass();
        }
        if (entity == null || entity == Object.class) {
            throw new RuntimeException("The entityClass value of @MethodService of Method[" + method.getName() + "] must be a subclass of IEntity.");
        }
        return entity;
    }

    private Class<? extends IEntity> getEntityClass() {
        if (this.entityClass == null) {
            this.entityClass = this.getEntityClass(api.entity(), api.table());
        }
        return this.entityClass;
    }

    /**
     * 根据{@link FormMethod}或{@link FormService}注解上声明的entityClass和entityTable
     * 值解析实际的EntityClass值
     *
     * @param entityClass Entity类
     * @param entityTable 表名称
     * @return 有效的Entity Class
     */
    private Class getEntityClass(Class entityClass, String entityTable) {
        if (If.notBlank(entityTable)) {
            return this.getEntityClass(entityTable);
        } else if (Object.class.equals(entityClass)) {
            return Object.class;
        } else if (IEntity.class.isAssignableFrom(entityClass)) {
            return entityClass;
        } else {
            throw new RuntimeException("The value of entity() of @Action(@FormService) must be a subclass of IEntity.");
        }
    }

    /**
     * 根据表名称获取实例类型
     *
     * @param table 表名称
     * @return 实例类型
     */
    public Class<? extends IEntity> getEntityClass(String table) {
        if (If.isBlank(table)) {
            return null;
        }
        if (TableEntityClass.containsKey(table)) {
            return TableEntityClass.get(table);
        }
        AMapping mapping = RefKit.byTable(table);
        if (mapping == null) {
            throw new RuntimeException("The table[" + table + "] not found.");
        } else {
            return mapping.entityClass();
        }
    }

    private static final KeyMap<IMethodAround> instances = new KeyMap<IMethodAround>().put(NoMethodAround.class, NoMethodAround.instance);

    private IMethodAround aroundInstance(Class<? extends IMethodAround> aClass) {
        if (instances.containsKey(aClass)) {
            return instances.get(aClass);
        }
        synchronized (instances) {
            if (instances.containsKey(aClass)) {
                return instances.get(aClass);
            }
            IMethodAround aop = NoMethodAround.instance;
            try {
                aop = aClass.getDeclaredConstructor().newInstance();
            } catch (Exception ignored) {
            }
            instances.put(aClass, aop);
            return aop;
        }
    }

    /**
     * 构造eClass实体实例
     *
     * @param method 操作定义
     * @param metas  入参元数据
     * @return entity实例
     */
    public static <R> R save(MethodMeta method, EntryMetas metas) {
        IEntity entity = FormHelper.newEntity(method, metas);
        Object pk = RefKit.mapper(method.entityClass).save(entity);
        if (method.returnType == void.class || method.returnType == Void.class) {
            return null;
        } else if (method.returnType == Boolean.class || method.returnType == boolean.class) {
            return (R) (Boolean) (pk != null);
        } else if (method.returnType.isAssignableFrom(method.entityClass)) {
            return (R) entity;
        } else {
            return (R) FormHelper.entity2result(entity, method.returnType);
        }
    }

    /**
     * 更新操作
     *
     * @param method 操作定义
     * @param metas  入参元数据
     * @return ignore
     */
    public static int update(MethodMeta method, EntryMetas metas) {
        IUpdate update = FormHelper.newUpdate(method, metas);
        return RefKit.mapper(method.entityClass).updateBy(update);
    }

    /**
     * 构造查询条件实例
     *
     * @param method 操作定义
     * @param metas  入参元数据
     * @return 查询实例
     */
    public static Object query(MethodMeta method, EntryMetas metas) {
        IQuery query = FormHelper.newQuery(method, metas);
        if (method.isCount()) {
            int count = query.to().count();
            return method.isReturnLong() ? (long) count : count;
        } else if (method.isStdPage()) {
            /* 标准分页 */
            StdPagedList paged = query.to().stdPagedEntity();
            List data = FormHelper.entities2result(paged.getData(), method.returnParameterType);
            return paged.setData(data);
        } else if (method.isTagPage()) {
            /* Tag分页 */
            TagPagedList paged = query.to().tagPagedEntity();
            List data = FormHelper.entities2result(paged.getData(), method.returnParameterType);
            IEntity next = (IEntity) paged.getNext();
            return new TagPagedList(data, next == null ? null : next.findPk());
        } else if (method.isList()) {
            /* 返回List */
            List<IEntity> list = query.to().listEntity();
            return FormHelper.entities2result(list, method.returnParameterType);
        } else {
            /* 查找单条数据 */
            query.limit(1);
            IEntity entity = (IEntity) query.to().findOne().orElse(null);
            return FormHelper.entity2result(entity, method.returnType);
        }
    }
}