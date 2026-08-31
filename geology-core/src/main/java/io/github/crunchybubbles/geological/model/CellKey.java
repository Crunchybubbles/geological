package io.github.crunchybubbles.geological.model;

import java.util.Comparator;

/** Integer identity for one owned cell in the spatial hierarchy. */
public record CellKey(String level, long x, long z) implements Comparable<CellKey> {
  public static final Comparator<CellKey> ORDER =
      Comparator.comparing(CellKey::level)
          .thenComparingLong(CellKey::x)
          .thenComparingLong(CellKey::z);

  public CellKey {
    if (level == null || level.isBlank()) {
      throw new IllegalArgumentException("level must be present");
    }
  }

  public static CellKey containing(String level, long worldX, long worldZ, long cellSize) {
    if (cellSize <= 0) {
      throw new IllegalArgumentException("cellSize must be positive");
    }
    return new CellKey(level, Math.floorDiv(worldX, cellSize), Math.floorDiv(worldZ, cellSize));
  }

  @Override
  public int compareTo(CellKey other) {
    return ORDER.compare(this, other);
  }
}
