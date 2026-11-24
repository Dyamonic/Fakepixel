package club.abelix.commons.data.rank.service.remote;

import club.abelix.commons.data.rank.Rank;
import club.abelix.commons.data.rank.entity.RankEntity;
import club.abelix.commons.data.rank.permission.GroupPermission;
import club.abelix.commons.data.rank.permission.GroupPermissions;
import club.abelix.commons.data.rank.permission.ManagedGroupPermissions;
import club.abelix.commons.data.rank.permission.Permissions;
import club.abelix.commons.data.rank.service.RanksService;
import club.abelix.commons.data.rank.service.mapper.RanksMapper;
import club.abelix.commons.data.rank.service.remote.grpc.RankServiceGrpcIntegration;
import club.abelix.ranks.proto.PermissionUpdateRequest;
import club.abelix.ranks.proto.PermissionUpdateResponse;
import club.abelix.ranks.proto.RefreshRanksResponse;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class RemoteRankService implements RanksService {

    private static final Integer MAX_RETRIES = 5;

    @Getter
    final RankServiceGrpcIntegration rankServiceGrpcIntegration;

    volatile List<RankRecord> ranks = List.of();
    volatile Int2ObjectMap<RankRecord> idToRankMap = new Int2ObjectOpenHashMap<>();
    volatile Map<String, RankRecord> nameToRankMap = Map.of();
    final AtomicBoolean reloadInProgress = new AtomicBoolean(false);

    @NonFinal
    Rank defaultRank;

    @Override
    public void load() {
        if (!reloadInProgress.compareAndSet(false, true)) return;

        rankServiceGrpcIntegration.findAllRanks()
                .thenAccept(response -> {
                    if (response == null) return;

                    val newList = new ArrayList<RankRecord>(response.getRanksCount());
                    val newIdMap = new Int2ObjectOpenHashMap<RankRecord>(response.getRanksCount());
                    val newNameMap = new HashMap<String, RankRecord>(response.getRanksCount());

                    for (var proto : response.getRanksList()) {
                        var rank = RanksMapper.mapFromProto(proto);
                        var record = new RankRecord(rank, getMappedEntity(rank));
                        newList.add(record);
                        newIdMap.put(rank.getId(), record);
                        newNameMap.put(rank.getName().toLowerCase(Locale.ROOT), record);
                    }

                    this.ranks = List.copyOf(newList);
                    this.idToRankMap = newIdMap;
                    this.nameToRankMap = Map.copyOf(newNameMap);
                })
                .exceptionally(t -> {
                    log.error("Reloading ranks failed. Keeping previous snapshot.", t);
                    return null;
                })
                .whenComplete((r, t) -> reloadInProgress.set(false));

        rankServiceGrpcIntegration.defaultRank()
                .thenAccept(response -> {
                    if (response == null) return;
                    this.defaultRank = getMappedEntity(RanksMapper.mapFromProto(response.getRank()));
                })
                .exceptionally(t -> {
                    log.error("Reloading default rank failed. Keeping previous value.", t);
                    return null;
                });
    }

    @Override
    public @NotNull Set<Rank> findAllRanks() {
        return ranks.stream()
                .map(RankRecord::mappedEntity)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public @NotNull Rank defaultRank() {
        return defaultRank;
    }

    @Override
    public @NotNull Optional<Rank> findRankById(int id) {
        if (!idToRankMap.isEmpty()) return Optional.of(idToRankMap.get(id).mappedEntity());

        for (int i = 0; i < MAX_RETRIES; i++) {
            val rank = idToRankMap.get(id);

            if (rank != null) return Optional.of(rank.mappedEntity());

            refreshRanks();
        }

        return Optional.empty();
    }

    @Override
    public @NotNull Optional<Rank> findByName(@NotNull String name) {
        return Optional.ofNullable(nameToRankMap.get(name.toLowerCase()).mappedEntity());
    }

    @Override
    public @NotNull CompletableFuture<RefreshRanksResponse> refreshRanks() {
        return rankServiceGrpcIntegration.refreshRanks();
    }

    @Override
    public @NotNull CompletableFuture<PermissionUpdateResponse> updatePermission(int rankId, @NotNull String permission, PermissionUpdateRequest.@NotNull Action action) {
        return rankServiceGrpcIntegration.updatePermissions(rankId, permission, action);
    }

    @Override
    public @NotNull Optional<RankRecord> findRawById(int id) {
        return Optional.ofNullable(idToRankMap.get(id));
    }

    private GroupPermissions getRankPermissions(RankEntity rank) {
        Set<GroupPermission> rankPermissions = new HashSet<>();
        collectPermissionsRecursive(rank, rankPermissions);
        return ManagedGroupPermissions.create(rankPermissions);
    }

    private void collectPermissionsRecursive(RankEntity rank, Set<GroupPermission> collected) {
        collected.addAll(mapPermissions(rank.getPermissions()));

        for (RankEntity parent : rank.getInheritedRanks()) {
            collectPermissionsRecursive(parent, collected);
        }
    }

    private Set<GroupPermission> mapPermissions(Set<String> permissions) {
        return permissions.stream()
                .map(Permissions::from)
                .collect(Collectors.toSet());
    }

    private Rank getMappedEntity(RankEntity rankEntity) {
        val rankPermissions = getRankPermissions(rankEntity);
        log.debug("Mapped rank permissions for {} -> {}", rankEntity.getName(), rankPermissions);

        return Rank.builder()
                .id(rankEntity.getId())
                .priority(rankEntity.getPriority())
                .name(rankEntity.getName())
                .displayNameTranslationKey(rankEntity.getDisplayNameKey())
                .prefix(rankEntity.getPrefix())
                .suffix(rankEntity.getSuffix())
                .permissions(rankPermissions)
                .build();
    }

    public record RankRecord(RankEntity entity, Rank mappedEntity) {

    }

}
