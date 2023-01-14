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
package org.apache.ibatis.cache.decorators;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Deque;
import java.util.LinkedList;

import org.apache.ibatis.cache.Cache;

/**
 * Soft Reference cache decorator
 * Thanks to Dr. Heinz Kabutz for his guidance here.
 *
 * @author Clinton Begin
 */
public class SoftCache implements Cache {
  // SoftCache 装饰的底层 Cache 对象
  private final Cache delegate;
  // 该引用队列会与每个 SoftEntry 对象关联，用于记录已经被回收的缓存条目，即 SoftEntry 对象，
  // SoftEntry 又通过 key 这个强引用指向缓存的 Key 值，这样我们就可以知道哪个 Key 被回收了。
  private final ReferenceQueue<Object> queueOfGarbageCollectedEntries;
  // 在 SoftCache 中，最近经常使用的一部分缓存条目（也就是热点数据）会被添加到这个集合中，
  // 正如其名称的含义，该集合会使用强引用指向其中的每个缓存 Value，防止它被 GC 回收。
  private final Deque<Object> hardLinksToAvoidGarbageCollection;
  // 指定了强连接的个数，默认值是 256，也就是最近访问的 256 个 Value 无法直接被 GC 回收。
  private int numberOfHardLinks;

  public SoftCache(Cache delegate) {
    this.delegate = delegate;
    this.numberOfHardLinks = 256;
    this.hardLinksToAvoidGarbageCollection = new LinkedList<>();
    this.queueOfGarbageCollectedEntries = new ReferenceQueue<>();
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public int getSize() {
    removeGarbageCollectedItems();
    return delegate.getSize();
  }

  public void setSize(int size) {
    this.numberOfHardLinks = size;
  }

  @Override
  public void putObject(Object key, Object value) {
    removeGarbageCollectedItems();
    delegate.putObject(key, new SoftEntry(key, value, queueOfGarbageCollectedEntries));
  }

  @Override
  public Object getObject(Object key) {
    Object result = null;
    // 从底层被装饰的缓存中查找数据
    @SuppressWarnings("unchecked")
    SoftReference<Object> softReference = (SoftReference<Object>) delegate.getObject(key);
    if (softReference != null) {
      result = softReference.get();
      if (result == null) {
        // Value 为 null，则已被 GC 回收，直接从缓存删除该 Key
        delegate.removeObject(key);
      } else { // 未被 GC 回收
        // 将 Value 添加到 hardLinksToAvoidGarbageCollection 集合中，防止被 GC 回收
        synchronized (hardLinksToAvoidGarbageCollection) {
          hardLinksToAvoidGarbageCollection.addFirst(result);
          // 检查 hardLinksToAvoidGarbageCollection 长度，超过上限，则清理最早添加的 Value
          if (hardLinksToAvoidGarbageCollection.size() > numberOfHardLinks) {
            hardLinksToAvoidGarbageCollection.removeLast();
          }
        }
      }
    }
    return result;
  }

  @Override
  public Object removeObject(Object key) {
    removeGarbageCollectedItems();
    @SuppressWarnings("unchecked")
    SoftReference<Object> softReference = (SoftReference<Object>) delegate.removeObject(key);
    return softReference == null ? null : softReference.get();
  }

  @Override
  public void clear() {
    synchronized (hardLinksToAvoidGarbageCollection) {
      hardLinksToAvoidGarbageCollection.clear();
    }
    removeGarbageCollectedItems();
    delegate.clear();
  }

  private void removeGarbageCollectedItems() {
    SoftEntry sv;
    while ((sv = (SoftEntry) queueOfGarbageCollectedEntries.poll()) != null) {
      delegate.removeObject(sv.key);
    }
  }

  private static class SoftEntry extends SoftReference<Object> {
    private final Object key;

    SoftEntry(Object key, Object value, ReferenceQueue<Object> garbageCollectionQueue) {
      // 指向 value 的是软引用，并且关联了引用队列
      super(value, garbageCollectionQueue);
      // 指向 key 的是强引用
      this.key = key;
    }
  }

}
