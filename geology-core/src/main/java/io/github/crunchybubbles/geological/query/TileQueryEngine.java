package io.github.crunchybubbles.geological.query;

import io.github.crunchybubbles.geological.model.Point2;
import io.github.crunchybubbles.geological.surface.SurfaceSample;
import java.util.ArrayList;
import java.util.List;

public final class TileQueryEngine {
  private final GeologyQueryEngine queryEngine;

  public TileQueryEngine(GeologyQueryEngine queryEngine) {
    this.queryEngine = queryEngine;
  }

  public AtlasTile query(TileKey key) {
    int side = key.samplesPerSide();
    List<SurfaceSample> samples = new ArrayList<>(Math.multiplyExact(side, side));
    for (int z = 0; z < side; z++) {
      long worldZ = Math.addExact(key.originZ(), Math.multiplyExact((long) z, key.spacing()));
      for (int x = 0; x < side; x++) {
        long worldX = Math.addExact(key.originX(), Math.multiplyExact((long) x, key.spacing()));
        samples.add(queryEngine.surface(new Point2(worldX, worldZ)));
      }
    }
    return new AtlasTile(key, samples);
  }
}
