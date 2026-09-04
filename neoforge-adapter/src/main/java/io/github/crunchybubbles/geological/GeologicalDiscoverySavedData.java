package io.github.crunchybubbles.geological;

import io.github.crunchybubbles.geological.determinism.StableId;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfile;
import io.github.crunchybubbles.geological.worldgen.DimensionGeologyProfiles;
import io.github.crunchybubbles.geological.worldgen.ExplorationNotebook;
import io.github.crunchybubbles.geological.worldgen.ExplorationNotebookEntry;
import io.github.crunchybubbles.geological.worldgen.NotebookEvidenceKind;
import io.github.crunchybubbles.geological.worldgen.WorldgenSnapshot;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * World-persistent bridge for player notebooks.
 *
 * <p>Only compact player-visible evidence is written. Natural geology remains reconstructible from
 * the frozen worldgen snapshot and is never copied into this saved data.
 */
public final class GeologicalDiscoverySavedData extends SavedData {
  public static final String DATA_ID = "geological_discovery";
  public static final int SCHEMA_VERSION = 1;

  private static final String KEY_SCHEMA_VERSION = "schema_version";
  private static final String KEY_MODEL_VERSION = "model_version";
  private static final String KEY_SCIENTIFIC_DIGEST = "scientific_digest";
  private static final String KEY_CONFIGURATION_DIGEST = "configuration_digest";
  private static final String KEY_PRESENTATION_DIGEST = "presentation_digest";
  private static final String KEY_SCALE_PROFILE = "scale_profile";
  private static final String KEY_PLAYERS = "players";
  private static final String KEY_PLAYER_ID = "player_id";
  private static final String KEY_ENTRIES = "entries";
  private static final String KEY_ENTRY_ID = "entry_id";
  private static final String KEY_EVIDENCE_KIND = "evidence_kind";
  private static final String KEY_EVIDENCE_ID = "evidence_id";
  private static final String KEY_DIMENSION = "dimension";
  private static final String KEY_BLOCK_X = "block_x";
  private static final String KEY_BLOCK_Y = "block_y";
  private static final String KEY_BLOCK_Z = "block_z";
  private static final String KEY_SUMMARY = "summary";
  private static final String KEY_NOTE = "note";
  private static final String KEY_PROVENANCE = "provenance";
  private static final String KEY_CONFIDENCE_PPM = "confidence_ppm";
  private static final String DIMENSION_PROFILE_ID = "minecraft:overworld";
  private static final DimensionGeologyProfile OVERWORLD =
      DimensionGeologyProfiles.require(DIMENSION_PROFILE_ID);
  private static final WorldgenSnapshot CURRENT_SNAPSHOT = WorldgenSnapshot.forProfile(OVERWORLD);
  private static final Comparator<UUID> UUID_ORDER = Comparator.comparing(UUID::toString);

  public static final SavedData.Factory<GeologicalDiscoverySavedData> FACTORY =
      new SavedData.Factory<>(
          GeologicalDiscoverySavedData::new, GeologicalDiscoverySavedData::load);

  private final WorldgenSnapshot snapshot;
  private final Map<UUID, ExplorationNotebook> notebooks;

  private GeologicalDiscoverySavedData() {
    this(CURRENT_SNAPSHOT, new TreeMap<>(UUID_ORDER));
  }

  private GeologicalDiscoverySavedData(
      WorldgenSnapshot snapshot, Map<UUID, ExplorationNotebook> notebooks) {
    this.snapshot = Objects.requireNonNull(snapshot, "worldgen snapshot");
    this.notebooks = new TreeMap<>(UUID_ORDER);
    this.notebooks.putAll(notebooks);
  }

  /** Returns the singleton world-level saved data store used by the Overworld. */
  public static GeologicalDiscoverySavedData get(ServerLevel level) {
    Objects.requireNonNull(level, "server level");
    if (!level.dimension().equals(Level.OVERWORLD)) {
      throw new IllegalArgumentException("discovery notebooks currently support Overworld only");
    }
    return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_ID);
  }

  public static GeologicalDiscoverySavedData load(
      CompoundTag tag, HolderLookup.Provider ignoredRegistries) {
    Objects.requireNonNull(tag, "saved notebook tag");
    WorldgenSnapshot snapshot = readSnapshot(tag);
    Map<UUID, ExplorationNotebook> notebooks = new TreeMap<>(UUID_ORDER);
    if (tag.contains(KEY_PLAYERS, Tag.TAG_LIST)) {
      ListTag players = tag.getList(KEY_PLAYERS, Tag.TAG_COMPOUND);
      for (int index = 0; index < players.size(); index++) {
        CompoundTag playerTag = players.getCompound(index);
        UUID playerId = readUuid(playerTag, KEY_PLAYER_ID);
        List<ExplorationNotebookEntry> entries = readEntries(playerTag);
        ExplorationNotebook notebook =
            new ExplorationNotebook(
                snapshot.modelVersion(),
                snapshot.scientificDigest(),
                DIMENSION_PROFILE_ID,
                entries);
        if (notebooks.put(playerId, notebook) != null) {
          throw new IllegalArgumentException(
              "saved discovery notebook contains duplicate player IDs");
        }
      }
    }
    return new GeologicalDiscoverySavedData(snapshot, notebooks);
  }

  public WorldgenSnapshot snapshot() {
    return snapshot;
  }

  /** Whether new records may be added without silently reinterpreting an old world. */
  public boolean isCompatibleWithCurrentSnapshot() {
    return snapshot.equals(CURRENT_SNAPSHOT);
  }

  public ExplorationNotebook notebook(UUID playerId) {
    Objects.requireNonNull(playerId, "player ID");
    return notebooks.getOrDefault(
        playerId,
        ExplorationNotebook.empty(
            snapshot.modelVersion(), snapshot.scientificDigest(), DIMENSION_PROFILE_ID));
  }

  /** Atomically records a bounded batch and marks the world save dirty when it changes. */
  public ExplorationNotebook record(UUID playerId, List<ExplorationNotebookEntry> additions) {
    Objects.requireNonNull(playerId, "player ID");
    Objects.requireNonNull(additions, "notebook additions");
    if (!isCompatibleWithCurrentSnapshot()) {
      throw new IllegalStateException(
          "notebook world identity is stale; restore the matching Geological data pack before recording");
    }
    ExplorationNotebook before = notebook(playerId);
    ExplorationNotebook after = before.recordAll(additions);
    if (!after.equals(before)) {
      notebooks.put(playerId, after);
      setDirty();
    }
    return after;
  }

  public boolean forget(UUID playerId, StableId entryId) {
    Objects.requireNonNull(playerId, "player ID");
    Objects.requireNonNull(entryId, "entry ID");
    if (!isCompatibleWithCurrentSnapshot()) {
      throw new IllegalStateException(
          "notebook world identity is stale; restore the matching Geological data pack before editing");
    }
    ExplorationNotebook before = notebook(playerId);
    ExplorationNotebook after = before.forget(entryId);
    if (after.equals(before)) {
      return false;
    }
    if (after.entries().isEmpty()) {
      notebooks.remove(playerId);
    } else {
      notebooks.put(playerId, after);
    }
    setDirty();
    return true;
  }

  public int playerCount() {
    return notebooks.size();
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider ignoredRegistries) {
    tag.putInt(KEY_SCHEMA_VERSION, SCHEMA_VERSION);
    tag.putString(KEY_MODEL_VERSION, snapshot.modelVersion());
    tag.putString(KEY_SCIENTIFIC_DIGEST, snapshot.scientificDigest());
    tag.putString(KEY_CONFIGURATION_DIGEST, snapshot.configurationDigest());
    tag.putString(KEY_PRESENTATION_DIGEST, snapshot.presentationDigest());
    tag.putString(KEY_SCALE_PROFILE, snapshot.scaleProfileId());
    ListTag players = new ListTag();
    notebooks.entrySet().stream()
        .sorted(Map.Entry.comparingByKey(UUID_ORDER))
        .forEach(
            player -> {
              CompoundTag playerTag = new CompoundTag();
              playerTag.putUUID(KEY_PLAYER_ID, player.getKey());
              ListTag entries = new ListTag();
              player.getValue().entries().stream()
                  .map(GeologicalDiscoverySavedData::writeEntry)
                  .forEach(entries::add);
              playerTag.put(KEY_ENTRIES, entries);
              players.add(playerTag);
            });
    tag.put(KEY_PLAYERS, players);
    return tag;
  }

  private static CompoundTag writeEntry(ExplorationNotebookEntry entry) {
    CompoundTag tag = new CompoundTag();
    tag.putString(KEY_ENTRY_ID, entry.entryId().toString());
    tag.putString(KEY_EVIDENCE_KIND, entry.evidenceKind().name());
    tag.putString(KEY_EVIDENCE_ID, entry.evidenceId().toString());
    tag.putString(KEY_DIMENSION, entry.dimensionProfileId());
    tag.putLong(KEY_BLOCK_X, entry.blockX());
    tag.putInt(KEY_BLOCK_Y, entry.blockY());
    tag.putLong(KEY_BLOCK_Z, entry.blockZ());
    tag.putString(KEY_SUMMARY, entry.evidenceSummary());
    tag.putString(KEY_NOTE, entry.note());
    tag.putInt(KEY_CONFIDENCE_PPM, entry.confidencePpm());
    ListTag provenance = new ListTag();
    entry.provenanceBodyIds().stream()
        .map(StableId::toString)
        .map(StringTag::valueOf)
        .forEach(provenance::add);
    tag.put(KEY_PROVENANCE, provenance);
    return tag;
  }

  private static List<ExplorationNotebookEntry> readEntries(CompoundTag playerTag) {
    if (!playerTag.contains(KEY_ENTRIES, Tag.TAG_LIST)) {
      return List.of();
    }
    ListTag entries = playerTag.getList(KEY_ENTRIES, Tag.TAG_COMPOUND);
    if (entries.size() > ExplorationNotebook.MAX_ENTRIES) {
      throw new IllegalArgumentException(
          "saved discovery notebook exceeds " + ExplorationNotebook.MAX_ENTRIES + " entries");
    }
    java.util.ArrayList<ExplorationNotebookEntry> result =
        new java.util.ArrayList<>(entries.size());
    for (int index = 0; index < entries.size(); index++) {
      CompoundTag tag = entries.getCompound(index);
      ListTag provenance = tag.getList(KEY_PROVENANCE, Tag.TAG_STRING);
      List<StableId> bodyIds =
          java.util.stream.IntStream.range(0, provenance.size())
              .mapToObj(provenance::getString)
              .map(StableId::parse)
              .toList();
      result.add(
          new ExplorationNotebookEntry(
              StableId.parse(tag.getString(KEY_ENTRY_ID)),
              NotebookEvidenceKind.valueOf(tag.getString(KEY_EVIDENCE_KIND)),
              StableId.parse(tag.getString(KEY_EVIDENCE_ID)),
              tag.getString(KEY_DIMENSION),
              tag.getLong(KEY_BLOCK_X),
              tag.getInt(KEY_BLOCK_Y),
              tag.getLong(KEY_BLOCK_Z),
              tag.getString(KEY_SUMMARY),
              tag.getString(KEY_NOTE),
              bodyIds,
              tag.getInt(KEY_CONFIDENCE_PPM)));
    }
    return List.copyOf(result);
  }

  private static UUID readUuid(CompoundTag tag, String key) {
    if (!tag.hasUUID(key)) {
      throw new IllegalArgumentException("saved notebook player ID is missing");
    }
    return tag.getUUID(key);
  }

  private static WorldgenSnapshot readSnapshot(CompoundTag tag) {
    if (!tag.contains(KEY_MODEL_VERSION, Tag.TAG_STRING)) {
      return CURRENT_SNAPSHOT;
    }
    int schema = tag.getInt(KEY_SCHEMA_VERSION);
    if (schema != SCHEMA_VERSION) {
      throw new IllegalArgumentException("unsupported discovery notebook schema: " + schema);
    }
    return new WorldgenSnapshot(
        tag.getString(KEY_MODEL_VERSION),
        tag.getString(KEY_SCIENTIFIC_DIGEST),
        tag.getString(KEY_CONFIGURATION_DIGEST),
        tag.getString(KEY_PRESENTATION_DIGEST),
        tag.getString(KEY_SCALE_PROFILE));
  }
}
