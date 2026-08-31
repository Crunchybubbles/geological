package io.github.crunchybubbles.geological;

import io.github.crunchybubbles.geological.atlas.Province;
import io.github.crunchybubbles.geological.atlas.ProvinceGrammar;
import io.github.crunchybubbles.geological.model.CellKey;
import io.github.crunchybubbles.geological.query.GeologyQueryEngine;

final class Phase1TestSupport {
  private Phase1TestSupport() {}

  static Province provinceWithGrammar(GeologyQueryEngine query, ProvinceGrammar grammar) {
    for (long radius = 0; radius <= 12; radius++) {
      for (long x = -radius; x <= radius; x++) {
        for (long z = -radius; z <= radius; z++) {
          if (StrictMath.max(StrictMath.abs(x), StrictMath.abs(z)) != radius) {
            continue;
          }
          Province province = query.atlas().province(new CellKey("province", x, z));
          if (province.grammar() == grammar) {
            return province;
          }
        }
      }
    }
    throw new AssertionError("test seed did not produce grammar " + grammar);
  }
}
