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
package org.apache.ibatis.reflection;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.ibatis.util.MapUtil;

public class DefaultReflectorFactory implements ReflectorFactory {
  private boolean classCacheEnabled = true;
  // 缓存其创建的所有 Reflector 对象
  private final ConcurrentMap<Class<?>, Reflector> reflectorMap = new ConcurrentHashMap<>();

  public DefaultReflectorFactory() {
  }

  @Override
  public boolean isClassCacheEnabled() {
    return classCacheEnabled;
  }

  @Override
  public void setClassCacheEnabled(boolean classCacheEnabled) {
    this.classCacheEnabled = classCacheEnabled;
  }

  @Override
  public Reflector findForClass(Class<?> type) {
    if (classCacheEnabled) {
      // 首先会根据传入的 Class 类查询 reflectorMap 缓存，
      // 如果查找到对应的 `Reflector` 对象，则直接返回；
      // 否则创建相应的 Reflector 对象，并记录到 reflectorMap 中缓存，等待下次使用
      return MapUtil.computeIfAbsent(reflectorMap, type, Reflector::new);
    } else {
      // 如果禁用缓存，则直接创建返回，也不缓存
      return new Reflector(type);
    }
  }

}
