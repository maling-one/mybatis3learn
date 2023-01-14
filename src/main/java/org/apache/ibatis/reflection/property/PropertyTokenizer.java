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

import java.util.Iterator;

/**
 * @author Clinton Begin
 */
public class PropertyTokenizer implements Iterator<PropertyTokenizer> {
  // 当前 property 名
  private String name;
  /*
   * 当前完整分词
   * 要么是 fullname
   * 如果存在分隔符 “.”，则表示 “.” 前的所有字符
   */
  private final String indexedName;
  /*
   * 如果是数组，则表示下标，0、1、2...
   * 如果是 Map，则表示 key
   */
  private String index;
  //如果有分隔符 “.”  则表示 “.” 后边剩余的字符串
  private final String children;

  public PropertyTokenizer(String fullname) {
    int delim = fullname.indexOf('.');
    if (delim > -1) {
      // 如果存在分隔符 “.”，则用 name 存储 "." 前的所有字符
      name = fullname.substring(0, delim);
      // 用 children 存储第一个 “.” 后的剩余字符
      children = fullname.substring(delim + 1);
    } else {
      // 否则直接用 fullname 作为属性名
      name = fullname;
      children = null;
    }
    // 存储当前完整分词
    indexedName = name;
    delim = name.indexOf('[');
    if (delim > -1) {
      //如果存在数组 []，则解析下标，将 name 更新为去掉 [] 后的名称
      index = name.substring(delim + 1, name.length() - 1);
      name = name.substring(0, delim);
    }
  }

  public String getName() {
    return name;
  }

  public String getIndex() {
    return index;
  }

  public String getIndexedName() {
    return indexedName;
  }

  public String getChildren() {
    return children;
  }

  @Override
  public boolean hasNext() {
    return children != null;
  }

  @Override
  public PropertyTokenizer next() {
    return new PropertyTokenizer(children);
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("Remove is not supported, as it has no meaning in the context of properties.");
  }
}
