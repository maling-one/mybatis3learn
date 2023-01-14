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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheException;

/**
 * <p>Simple blocking decorator
 *
 * <p>Simple and inefficient version of EhCache's BlockingCache decorator.
 * It sets a lock over a cache key when the element is not found in cache.
 * This way, other threads will wait until this element is filled instead of hitting the database.
 *
 * <p>By its nature, this implementation can cause deadlock when used incorrectly.
 *
 * @author Eduardo Macarron
 *
 */
public class BlockingCache implements Cache {

  private long timeout;
  private final Cache delegate;
  private final ConcurrentHashMap<Object, CountDownLatch> locks;

  public BlockingCache(Cache delegate) {
    this.delegate = delegate;
    this.locks = new ConcurrentHashMap<>();
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public int getSize() {
    return delegate.getSize();
  }

  @Override
  public void putObject(Object key, Object value) {
    try {
      delegate.putObject(key, value);
    } finally {
      releaseLock(key);
    }
  }

  @Override
  public Object getObject(Object key) {
    acquireLock(key); // 获取锁
    // 查询缓存
    Object value = delegate.getObject(key);
    if (value != null) {
      // 查询到了立刻释放锁
      releaseLock(key);
    }
    // 返回结果
    return value;
  }

  @Override
  public Object removeObject(Object key) {
    // despite its name, this method is called only to release locks
    releaseLock(key);
    return null;
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  private void acquireLock(Object key) {
    // 初始化一个全新的 CountDownLatch 对象
    CountDownLatch newLatch = new CountDownLatch(1);
    while (true) {
      // 尝试将 key 与 newLatch 这个 CountDownLatch 对象关联起来
      // 如果没有其他线程并发，则返回的 latch 为 null
      CountDownLatch latch = locks.putIfAbsent(key, newLatch);
      if (latch == null) {
        // 如果当前 key 未关联 CountDownLatch，
        // 则无其他线程并发，当前线程获取锁成功
        break;
      }
      try {
        // 当前 key 已关联 CountDownLatch 对象，则表示有其他线程并发操作当前 key，
        // 当前线程需要阻塞在并发线程留下的 CountDownLatch 对象 (旧 latch) 之上，
        // 直至并发线程调用 latch.countDown() 唤醒该线程
        if (timeout > 0) {// 根据 timeout 的值，决定阻塞超时时间
          boolean acquired = latch.await(timeout, TimeUnit.MILLISECONDS);
          // 超时未获取到锁，则抛出异常
          if (!acquired) {
            throw new CacheException(
              "Couldn't get a lock in " + timeout + " for the key " + key + " at the cache " + delegate.getId());
          }
        } else {// 死等
          latch.await();
        }
      } catch (InterruptedException e) {
        throw new CacheException("Got interrupted while trying to acquire lock for key " + key, e);
      }
    }
  }

  private void releaseLock(Object key) {
    CountDownLatch latch = locks.remove(key);
    if (latch == null) {
      throw new IllegalStateException("Detected an attempt at releasing unacquired lock. This should never happen.");
    }
    latch.countDown();
  }

  public long getTimeout() {
    return timeout;
  }

  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }
}
