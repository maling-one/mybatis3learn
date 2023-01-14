/*
 *    Copyright 2009-2022 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.reflection.property;

import java.util.Locale;

import org.apache.ibatis.reflection.ReflectionException;

/**
 * @author Clinton Begin
 */
public final class PropertyNamer {

  private PropertyNamer() {
    // Prevent Instantiation of Static Class
  }

  public static String methodToProperty(String name) {
    if (name.startsWith("is")) {
      // 如果是 is 开头，返回 is 后的作为属性名
      name = name.substring(2);
    } else if (name.startsWith("get") || name.startsWith("set")) {
      // 如果是 get set 方法，则返回后边的属性名
      name = name.substring(3);
    } else {
      // 如果不是 get、set、is 开头的，抛出异常
      throw new ReflectionException("Error parsing property name '" + name + "'.  Didn't start with 'is', 'get' or 'set'.");
    }
    // 如果属性名长度等于1，则直接转换成小写返回；
    // 如果大于 1 并且第二个字符是小写，则把第一个字符转换为小写，然后与剩余部分拼接后返回
    if (name.length() == 1 || (name.length() > 1 && !Character.isUpperCase(name.charAt(1)))) {
      name = name.substring(0, 1).toLowerCase(Locale.ENGLISH) + name.substring(1);
    }

    return name;
  }

  public static boolean isProperty(String name) {
    return isGetter(name) || isSetter(name);
  }

  public static boolean isGetter(String name) {
    return (name.startsWith("get") && name.length() > 3) || (name.startsWith("is") && name.length() > 2);
  }

  public static boolean isSetter(String name) {
    return name.startsWith("set") && name.length() > 3;
  }

}
