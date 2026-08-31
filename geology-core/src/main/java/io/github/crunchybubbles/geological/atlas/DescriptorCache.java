package io.github.crunchybubbles.geological.atlas;

import java.util.function.Function;

/** Pure descriptor cache; removing or bypassing it must never change generated values. */
public interface DescriptorCache<K, V> {
  V get(K key, Function<K, V> constructor);

  void clear();

  int size();

  static <K, V> DescriptorCache<K, V> none() {
    return new DescriptorCache<>() {
      @Override
      public V get(K key, Function<K, V> constructor) {
        return constructor.apply(key);
      }

      @Override
      public void clear() {}

      @Override
      public int size() {
        return 0;
      }
    };
  }
}
