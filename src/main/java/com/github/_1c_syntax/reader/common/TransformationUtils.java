/*
 * This file is a part of MDClasses.
 *
 * Copyright © 2019 - 2022
 * Tymko Oleg <olegtymko@yandex.ru>, Maximov Valery <maximovvalery@gmail.com> and contributors
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 *
 * MDClasses is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * MDClasses is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with MDClasses.
 */
package com.github._1c_syntax.reader.common;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.map.CaseInsensitiveMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Вспомогательный класс для конвертирования значений между моделями
 */
@UtilityClass
@Slf4j
public class TransformationUtils {

  private static final Map<Class<?>, Map<String, Method>> methods = new ConcurrentHashMap<>();
  private static final String BUILD_METHOD_NAME = "build";
  private static final String BUILDER_METHOD_NAME = "builder";
  private static final String LOGGER_MESSAGE_PREF = "Class {}, method {}";

  public void setValue(Object source, String methodName, Object value) {
    var method = getMethod(source.getClass(), methodName);
    if (method != null && value != null) {
      try {
        method.invoke(source, value);
      } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
        LOGGER.error(LOGGER_MESSAGE_PREF, source.getClass(), methodName, e);
      }
    }
  }

  public Type fieldType(Object source, String methodName) {
    var method = getMethod(source.getClass(), methodName);
    if (method != null) {
      return method.getGenericParameterTypes()[0];
    }
    return null;
  }

  public Object builder(Class<?> clazz) {
    var method = getMethod(clazz, BUILDER_METHOD_NAME);
    if (method != null) {
      try {
        return method.invoke(clazz);
      } catch (IllegalAccessException | InvocationTargetException e) {
        LOGGER.error(LOGGER_MESSAGE_PREF, clazz, BUILDER_METHOD_NAME, e);
      }
    }
    return null;
  }

  public Object build(Object builder) {
    var method = getMethod(builder.getClass(), BUILD_METHOD_NAME);
    if (method != null) {
      try {
        return method.invoke(builder);
      } catch (IllegalAccessException | InvocationTargetException e) {
        LOGGER.error(LOGGER_MESSAGE_PREF, builder.getClass(), BUILD_METHOD_NAME, e);
      }
    }
    return null;
  }

  private Method getMethod(Class<?> clazz, String methodName) {
    var classMethods = methods.get(clazz);
    if (classMethods == null) {
      classMethods = new CaseInsensitiveMap<>();
    }

    var method = classMethods.get(methodName);
    // ключ метода в кэше есть, но метода нет
    if (method == null) {
      // ключ метода в кэше есть, но метода нет
      if (classMethods.containsKey(methodName)) {
        return null;
      }
      method = Arrays.stream(clazz.getDeclaredMethods())
        .filter(classMethod -> methodName.equalsIgnoreCase(classMethod.getName()))
        .findFirst()
        .orElse(null);
      if (method != null) {
        saveMethod(clazz, classMethods, method, methodName);
      }
    }
    return method;
  }

  private static void saveMethod(Class<?> builderClass,
                                 Map<String, Method> classMethods,
                                 Method method,
                                 String builderMethodName) {
    classMethods.put(builderMethodName, method);
    methods.put(builderClass, classMethods);
  }
}