package org.example.entity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.function.Consumer;

/**
 * 对象转换接口
 */
public interface BaseData {

    /**
     * 将当前对象转换为指定类型对象, 然后传给`Consumer`进行处理, 然后染回该视图的对象
     *
     * @param clazz    指定类型对象
     * @param consumer 对应的函数, 这里是一个函数式接口
     * @param <V>      对应类型
     * @return 对应的视图对象
     */
    default <V> V asViewObject(Class<V> clazz, Consumer<V> consumer) {
        V v = this.asViewObject(clazz);
        consumer.accept(v);
        return v;
    }

    /**
     * 将当前对象转换为指定类型V的视图对象
     *
     * @param clazz 指定类型
     * @param <V>   对应类型
     * @return 指定类型的视图对象
     */
    default <V> V asViewObject(Class<V> clazz) {
        try {
            // 获取该类声明的所有字段
            Field[] declaredFields = clazz.getDeclaredFields();
            // 获取该类的无参构造函数
            Constructor<V> constructor = clazz.getConstructor();
            // 用获取的构造函数创建一个新的视图对象, 一般是无参构造
            V v = constructor.newInstance();
            // 遍历所有字段, 然后将对应的值复制到新创建的视图对象中
            for (Field declaredField : declaredFields) {
                convert(declaredField, v);
            }
            return v;
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception.getMessage());
        }

    }

    // 转换复制
    private void convert(Field field, Object vo) {
        try {
            // 先获取当前对象(调用本方法的对象)中的与field对应的字段
            Field source = this.getClass().getDeclaredField(field.getName());
            // 将这两个字段设置为可访问状态
            field.setAccessible(true);
            source.setAccessible(true);
            // 先去获取source在调用者中的值, 然后赋值给vo中的对应字段, field指定了需要赋值的字段对象
            field.set(vo, source.get(this));
        } catch (IllegalAccessException | NoSuchFieldException e) {

        }
    }

}
