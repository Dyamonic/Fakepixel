package club.abelix.api.base.rank;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.data.rank.service.RanksService;
import club.abelix.commons.data.rank.service.UserRanksService;
import club.abelix.core.bukkit.event.ConnectionEstablishedEvent;
import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.play.processor.PlayServerProcessor;
import club.abelix.network.packet.play.server.SRefreshRanksPacket;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class RankRefreshListener implements Listener, PlayServerProcessor {

    RanksService ranksService;
    UserRanksService userRanksService;

    @EventHandler
    public void onConnected(ConnectionEstablishedEvent event) {
        event.getProcessor().registerProcessor(this);
    }

    @Override
    public void process(SRefreshRanksPacket packet, @NotNull ChannelProcessorContext ctx) {
        CompletableFuture.runAsync(() -> {
            val startTime = System.currentTimeMillis();

            ranksService.load();

            userRanksService.refreshUsersRanks(Gamer.getGamers());

            log.info("Refresh ranks completed successfully in {}ms", System.currentTimeMillis() - startTime);
        });
    }

}
