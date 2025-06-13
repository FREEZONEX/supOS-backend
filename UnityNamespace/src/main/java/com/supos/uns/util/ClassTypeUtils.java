package com.supos.uns.util;

import java.lang.reflect.*;
import java.util.HashMap;

public class ClassTypeUtils {

    public static Type[] getParameterizedType(Class clazz) {
        // 获取当前类的实际泛型类型
        Type superClass = clazz.getGenericSuperclass();
        while (superClass != null && !(superClass instanceof ParameterizedType)) {
            // 处理多层继承或代理类的情况
            if (superClass instanceof Class<?>) {
                superClass = ((Class<?>) superClass).getGenericSuperclass();
            } else {
                throw new IllegalArgumentException("无法解析泛型类型: 未找到具体的参数化类型");
            }
        }

        if (superClass == null) {
            throw new IllegalArgumentException("必须指定具体的泛型类型");
        }

        ParameterizedType parameterizedType = (ParameterizedType) superClass;
        Type[] typeArgs = parameterizedType.getActualTypeArguments();
        if (typeArgs.length == 0) {
            throw new IllegalArgumentException("泛型类型参数未指定");
        }
        return typeArgs;
    }

    /**
     * 获取字段真实类型，考虑泛型
     *
     * @param field           - 字段
     * @param containingClass - 当前类
     * @return
     */
    public static Class getFieldClass(Field field, Class containingClass) {
        Class realClass = field.getType();
        if (field.getGenericType() instanceof TypeVariable typeVariable
                && containingClass.getGenericSuperclass() instanceof ParameterizedType) {
            Class<?> declaringClass = field.getDeclaringClass();
            GenericDeclaration d = typeVariable.getGenericDeclaration();
            String typeName = typeVariable.getTypeName();
            TypeVariable<?>[] ts = d.getTypeParameters();

            int index = -1;
            for (int i = 0; i < ts.length; i++) {
                TypeVariable variable = ts[i];
                if (typeName.equals(variable.getTypeName())) {
                    index = i;
                    break;
                }
            }
            Type[] prevTypes = null;
            while (containingClass != declaringClass) {
                if (containingClass.getGenericSuperclass() instanceof ParameterizedType pzt) {
                    Type[] types = pzt.getActualTypeArguments();
                    if (prevTypes != null) {
                        HashMap<String, Type> nameTypes = new HashMap<>(prevTypes.length);
                        TypeVariable<?>[] vars = containingClass.getTypeParameters();
                        for (int i = 0; i < vars.length; i++) {
                            Type type = vars[i];
                            nameTypes.put(type.getTypeName(), prevTypes[i]);
                        }
                        for (int i = 0; i < types.length; i++) {
                            types[i] = nameTypes.get(types[i].getTypeName());
                        }
                    }
                    prevTypes = types;
                } else {
                    break;
                }
                containingClass = containingClass.getSuperclass();
            }
            if (index >= 0 && prevTypes != null && prevTypes.length > 0 && index < prevTypes.length) {
                realClass = (Class) prevTypes[index];
            }
        }
        return realClass;
    }
}
