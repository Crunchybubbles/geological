package io.github.crunchybubbles.geological.atlas;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/** Small synchronized LRU used only as an acceleration layer for immutable descriptors. */
public final class BoundedDescriptorCache<K, V> implements DescriptorCache<K, V> {
  private final Map<K, V> values;

  public BoundedDescriptorCache(int maximumSize) {
    if (maximumSize <= 0) {
      throw new IllegalArgumentException("maximumSize must be positive");
    }
    values =
        new LinkedHashMap<>(maximumSize, 0.75f, true) {
          @Override
          protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maximumSize;
          }
        };
  }

  @Override
  public synchronized V get(K key, Function<K, V> constructor) {
    V existing = values.get(key);
    if (existing != null) {
      return existing;
    }
    V created = constructor.apply(key);
    values.put(key, created);
    return created;
  }

  @Override
  public synchronized void clear() {
    values.clear();
  }

  @Override
  public synchronized int size() {
    return values.size();
  }
}
