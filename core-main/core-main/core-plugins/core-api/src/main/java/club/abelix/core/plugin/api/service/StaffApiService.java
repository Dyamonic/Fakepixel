package club.abelix.core.plugin.api.service;

import club.abelix.commons.data.punishment.BansSqlHandler;
import club.abelix.commons.data.punishment.MutesSqlHandler;
import club.abelix.core.api.proto.v1.GetOnlineStaffResponse;
import club.abelix.core.api.proto.v1.PunishmentStats;
import club.abelix.core.api.proto.v1.StaffEntryProto;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.punishment.PunishmentManager;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StaffApiService {

    public GetOnlineStaffResponse getOnlineStaff() {
        val result = new ArrayList<StaffEntryProto>();

        for (val gamer : Gamer.getGamers()) {
            if (!gamer.getRanks().isStaff()) {
                continue;
            }

            result.add(
                    StaffEntryProto.newBuilder()
                            .setUsername(gamer.getDisplayName())
                            .setUserId(gamer.getPlayerId())
                            .setVanished(gamer.isVanished())
                            .build()
            );
        }

        return GetOnlineStaffResponse.newBuilder().addAllStaff(result).build();
    }

    public CompletableFuture<PunishmentStats> buildStats() {
        val bansCount = PunishmentManager.INSTANCE.getBansCount();
        val mutesCount = PunishmentManager.INSTANCE.getMuteCount();

        val activeBansCountFuture = BansSqlHandler.INSTANCE.getActiveBansCount();
        val activeMutesCountFuture = MutesSqlHandler.INSTANCE.getActiveMutesCount();

        val bansThisWeekFuture = BansSqlHandler.INSTANCE.getBansFromDate(LocalDateTime.now().minusDays(7));
        val mutesThisWeekFuture = MutesSqlHandler.INSTANCE.getMutesFromDate(LocalDateTime.now().minusDays(7));

        return CompletableFuture
                .allOf(
                        activeBansCountFuture,
                        activeMutesCountFuture,
                        bansThisWeekFuture,
                        mutesThisWeekFuture
                )
                .thenApply(ignored -> PunishmentStats.newBuilder()
                        .setTotalBans(bansCount)
                        .setTotalMutes(mutesCount)
                        .setActiveBans(activeBansCountFuture.join())
                        .setActiveMutes(activeMutesCountFuture.join())
                        .setBansThisWeek(bansThisWeekFuture.join())
                        .setMutesThisWeek(mutesThisWeekFuture.join())
                        .build()
                );
    }

}
