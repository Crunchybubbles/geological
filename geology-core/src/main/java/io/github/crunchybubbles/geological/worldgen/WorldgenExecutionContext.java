package io.github.crunchybubbles.geological.worldgen;

import java.util.Objects;
import java.util.concurrent.Executor;

/** Immutable worker context for one authorized worldgen stage and target chunk. */
public record WorldgenExecutionContext(
    WorldgenChunkRequest request,
    WorldgenStage stage,
    WorldgenSnapshot snapshot,
    Executor executor) {
  public WorldgenExecutionContext {
    Objects.requireNonNull(request, "worldgen chunk request");
    Objects.requireNonNull(stage, "worldgen stage");
    Objects.requireNonNull(snapshot, "worldgen snapshot");
    Objects.requireNonNull(executor, "stage-supplied executor");
    request.requireStage(stage);
    if (!snapshot.matches(request.profile())) {
      throw new IllegalArgumentException(
          "worldgen snapshot does not match the requested profile identity");
    }
  }

  /** Executes work on the executor supplied by the platform generation stage. */
  public void execute(Runnable task) {
    executor.execute(Objects.requireNonNull(task, "worldgen task"));
  }

  public boolean canWriteTarget() {
    return request.canWrite(stage);
  }

  /** Requires a stage that is permitted to mutate the target chunk. */
  public void requireWritableTarget() {
    if (!canWriteTarget()) {
      throw new IllegalStateException("stage " + stage + " is not a writable worldgen stage");
    }
  }

  /** Checks both stage permission and the chunk-local write rule. */
  public void requireWritableTargetChunk(long targetChunkX, long targetChunkZ) {
    requireWritableTarget();
    request.requireTargetChunk(targetChunkX, targetChunkZ);
  }

  public ChunkBlockBounds targetBounds() {
    return request.targetBounds();
  }
}
