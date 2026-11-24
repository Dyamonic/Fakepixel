package club.abelix.bot.discord;

import club.abelix.bot.discord.config.DiscordSyncBotConfig;
import club.abelix.bot.discord.handler.BotSqlHandler;
import club.abelix.bot.discord.user.DiscordBotUser;
import club.abelix.bot.discord.listener.DiscordBotListener;
import club.abelix.bot.discord.roles.AdaptedRole;
import club.abelix.commons.data.rank.Rank;
import club.abelix.commons.data.rank.UserRanks;
import club.abelix.commons.data.rank.service.RanksService;
import club.abelix.commons.data.rank.service.UserRanksService;
import club.abelix.commons.util.EnvUtility;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.packet.play.both.BUserRankRefreshPacket;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class DiscordBot {

    private final DiscordSyncBotConfig config;
    private final RanksService ranksService;
    private final UserRanksService userRanksService;

    private final Cache<String, Gamer> confirmationCache = CacheBuilder.newBuilder()
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .build();

    @Getter
    private final ArrayList<TextChannel> staffInfoChannels = new ArrayList<>();
    private final ArrayList<AdaptedRole> adaptedRolesList = new ArrayList<>();
    private final Map<Rank, Long> rankToDiscordRoleIds = new HashMap<>();

    @Getter
    private long guildId, linkedRoleId, discordLinkChatId;

    @Getter
    private Role boosterRole;
    private Rank boosterRank;

    @Getter
    private JDA discordApi;


    public void start() {
        prepareJda();

        try {
            discordApi.awaitReady();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        loadConfiguration();
    }

    private void prepareJda() {
        stop();

        try {
            discordApi = JDABuilder.createDefault(EnvUtility.getEnvStrict("DISCORD_SYNC_BOT_TOKEN"))
                    .enableIntents(
                            GatewayIntent.GUILD_MEMBERS,
                            GatewayIntent.MESSAGE_CONTENT,
                            GatewayIntent.GUILD_MESSAGES
                    )
                    .setActivity(Activity.playing(EnvUtility.getEnvStrict("PROJECT_IP")))
                    .addEventListeners(new DiscordBotListener(this))
                    .build();
        } catch (Exception e) {
            log.error("Unable to create Discord JDA instance", e);
        }
    }

    public void loadConfiguration() {
        guildId = config.getGuildId();
        linkedRoleId = config.getLinkedRoleId();
        discordLinkChatId = config.getLinkChannelId();

        System.out.println("JDA Status: " + discordApi.getStatus());

        for (val staffChannel : config.getStaffChannels()) {
            val textChannel = discordApi.getTextChannelById(staffChannel);

            if (textChannel != null) {
                staffInfoChannels.add(textChannel);
            } else {
                log.error("Unable to find staff info channel with id {}", staffChannel);
            }
        }

        for (val role : config.getAdaptedRoles()) {
            val discordRole = discordApi.getRoleById(role.getDiscordId());
            val rank = ranksService.findRankById(role.getLocalRankId()).orElseThrow();

            if (discordRole != null) {
                adaptedRolesList.add(new AdaptedRole(discordRole.getIdLong(), rank));

                rankToDiscordRoleIds.put(rank, discordRole.getIdLong());
            } else {
                log.error("Unable to find adapted role with id {}", role.getDiscordId());
            }
        }

        if (config.getBoosterRoleId() > 0) {
            boosterRole = discordApi.getRoleById(config.getBoosterRoleId());

            if (boosterRole == null) {
                log.error("Unable to find booster role with id {}", config.getBoosterRoleId());
            } else {
                boosterRank = ranksService.findByName("BOOSTER").orElse(null);

                if (boosterRank == null) {
                    log.error("Unable to find the 'BOOSTER' rank. Check that it exists in the ranks database.");
                }
            }
        } else {
            log.info("Booster integration disabled (boosterRoleId <= 0).");
        }

    }

    public void reconcileBoosterMember(@NonNull Member member) {
        long discordId = member.getIdLong();

        if (boosterRole == null || boosterRank == null) {
            return;
        }

        DiscordBotUser user = BotSqlHandler.INSTANCE.getByDiscordId(discordId);

        if (user == null) {
            return;
        }

        boolean boosting = boosting(member);

        Gamer gamer = user.getPlayer();
        boolean hasBoosterRank = gamer.getRanks().hasRank(boosterRank.getId());

        if (boosting && !hasBoosterRank) {
            log.info("Member {} ({}) is boosting - granting booster rank to {}",
                    member.getUser().getAsTag(), discordId, gamer.getName());

            userRanksService.addUserRank(user.getPlayerId(), boosterRank, null)
                    .whenComplete((resp, err) -> {
                        if (err != null) {
                            log.error("Failed to grant booster rank to {} ({})",
                                    gamer.getName(), discordId, err);
                            return;
                        }

                        applyRankChange(gamer);
                    });
            return;
        }

        if (!boosting && hasBoosterRank) {
            removeBoosterRank(gamer);
        }
    }

    private void applyRankChange(Gamer gamer) {
        gamer.loadRanks();
        gamer.updateDisplayName();

        val packet = new BUserRankRefreshPacket(gamer.getPlayerId());
        gamer.sendToProxy(packet);
        gamer.sendToServer(packet);
    }

    public void reconcileBoosterOnJoin(@NonNull Gamer gamer) {
        if (boosterRole == null || boosterRank == null) {
            return;
        }

        DiscordBotUser botUser = BotSqlHandler.INSTANCE.getByPlayerId(gamer.getPlayerId());

        if (botUser == null) {
            if (gamer.getRanks().hasRank(boosterRank.getId())) {
                log.info("Player {} holds the booster rank but is not linked to Discord - removing",
                        gamer.getName());
                removeBoosterRank(gamer);
            }
            return;
        }

        Guild guild = discordApi.getGuildById(guildId);

        if (guild == null) {
            log.warn("Unable to reconcile booster rank for {}: guild {} not found.",
                    gamer.getName(), guildId);
            return;
        }

        guild.retrieveMemberById(botUser.getDiscordId())
                .onErrorMap(t -> {
                    log.warn("Failed to retrieve Discord member {} for {} - keeping booster rank as-is",
                            botUser.getDiscordId(), gamer.getName());
                    return null;
                })
                .queue(member -> {
                    if (member == null) {
                        if (gamer.getRanks().hasRank(boosterRank.getId())) {
                            log.info("Player {} is linked to Discord but is no longer a member - removing booster rank",
                                    gamer.getName());
                            removeBoosterRank(gamer);
                        }
                        return;
                    }

                    reconcileBoosterMember(member);
                });
    }

    private boolean boosting(@NonNull Member member) {
        long targetRoleId = boosterRole.getIdLong();

        log.debug("Checking boosting status for member: {} (ID: {})", member.getEffectiveName(), member.getIdLong());
        log.debug("Target booster role ID to match: {}", targetRoleId);

        boolean isBoosting = member.getRoles().stream()
                .peek(role -> log.trace("Evaluating member role: {} (ID: {})", role.getName(), role.getIdLong()))
                .anyMatch(role -> role.getIdLong() == targetRoleId);

        log.debug("Boosting check result for {}: {}", member.getEffectiveName(), isBoosting);

        return isBoosting;
    }

    private void removeBoosterRank(@NonNull Gamer gamer) {
        if (!gamer.getRanks().hasRank(boosterRank.getId())) {
            return;
        }

        val userRank = gamer.getRanks().findUserRankById(boosterRank.getId());

        if (userRank == null) {
            return;
        }

        userRanksService.removeUserRank(gamer.getPlayerId(), userRank.id())
                .whenComplete((resp, err) -> {
                    if (err != null) {
                        log.error("Failed to remove booster rank from {}", gamer.getName(), err);
                        return;
                    }

                    applyRankChange(gamer);
                });
    }

    public void stop() {
        if (discordApi != null) {
            discordApi.shutdown();
        }
    }

    public void discordUserSync(long discordId, Gamer gamer) {
        log.info("Synchronizing Discord user {}", discordId);

        Guild guild = discordApi.getGuildById(guildId);

        if (guild == null) {
            log.error("Guild {} not found.", guildId);
            return;
        }

        Member cachedMember = guild.getMemberById(discordId);

        if (cachedMember != null) {
            applySync(guild, cachedMember, gamer);
            return;
        }

        guild.retrieveMemberById(discordId)
                .onErrorMap(t -> {
                    log.error("Failed to retrieve guild member {}", discordId, t);
                    return null;
                })
                .queue(member -> {
                    if (member != null) {
                        applySync(guild, member, gamer);
                    }
                });
    }

    private void applySync(Guild guild, Member member, Gamer gamer) {
        long discordId = member.getIdLong();
        Role linkedRole = guild.getRoleById(linkedRoleId);

        Set<Role> currentRoles = new HashSet<>(member.getRoles());
        Set<Role> managedRoles = adaptedRolesList.stream()
                .map(adaptedRole -> guild.getRoleById(adaptedRole.getDiscordRoleId()))
                .filter(Objects::nonNull).collect(Collectors.toSet());

        if (linkedRole != null) {
            managedRoles.add(linkedRole);
        }

        if (gamer == null) {
            Set<Role> rolesToRemove = new HashSet<>(currentRoles);
            rolesToRemove.retainAll(managedRoles);

            if (rolesToRemove.isEmpty()) {
                log.info("No synchronized roles to remove from Discord user {}", discordId);
                return;
            }

            guild.modifyMemberRoles(member, Collections.emptySet(), rolesToRemove)
                    .queue(
                            success -> log.info("Removed {} synchronized roles from Discord user {}",
                                    rolesToRemove.size(),
                                    discordId),
                            failure -> log.error("Failed to remove synchronized roles from Discord user {}",
                                    discordId,
                                    failure)
                    );
            return;
        }

        Set<Role> desiredRoles = new HashSet<>();

        if (linkedRole != null) {
            desiredRoles.add(linkedRole);
        }

        UserRanks userRanks = gamer.getRanks();

        userRanks.stream().forEach(userRank -> {
            Rank rank = userRank.rank();

            Long discordRoleId = rankToDiscordRoleIds.get(rank);

            if (discordRoleId == null) {
                log.warn("No Discord role mapping found for rank '{}' (id={})",
                        rank.getName(),
                        rank.getId());
                return;
            }

            Role userGroupRole = guild.getRoleById(discordRoleId);

            if (userGroupRole == null) {
                log.warn("Discord role {} for rank '{}' does not exist",
                        discordRoleId,
                        rank.getName());
                return;
            }

            desiredRoles.add(userGroupRole);
        });

        Set<Role> rolesToAdd = new HashSet<>(desiredRoles);
        rolesToAdd.removeAll(currentRoles);

        Set<Role> rolesToRemove = new HashSet<>(managedRoles);
        rolesToRemove.retainAll(currentRoles);
        rolesToRemove.removeAll(desiredRoles);

        if (rolesToAdd.isEmpty() && rolesToRemove.isEmpty()) {
            log.info("Discord roles already synchronized for {} ({})",
                    gamer.getName(),
                    discordId);
            Logging.IMP.debug("Synchronized Discord of {}", gamer.getName());
            return;
        }

        log.info("Synchronizing {} role(s) for {} ({})",
                rolesToAdd.size() + rolesToRemove.size(),
                gamer.getName(),
                discordId);

        guild.modifyMemberRoles(member, rolesToAdd, rolesToRemove)
                .queue(
                        success -> {
                            log.info("Discord synchronization completed for {}", gamer.getName());
                            Logging.IMP.debug("Synchronized Discord of {}", gamer.getName());
                        },
                        failure -> log.error("Failed to synchronize Discord roles for {} ({})",
                                gamer.getName(),
                                discordId,
                                failure)
                );

        if (!gamer.getRanks().hasPermission("discord.sync.skip_nickname")) {
            guild.modifyNickname(member, gamer.getName())
                    .queue(
                            success -> log.debug("Updated nickname for {}", gamer.getName()),
                            failure -> log.warn("Unable to update nickname for {} ({})",
                                    gamer.getName(),
                                    discordId,
                                    failure)
                    );
        }
    }


    public void addAwaitingConfirmation(@NonNull Gamer gamer, @NonNull String code) {
        confirmationCache.cleanUp();
        confirmationCache.put(code, gamer);
    }

    public Gamer getAwaitingConfirmationPlayer(@NonNull String confirmationCode) {
        confirmationCache.cleanUp();

        return confirmationCache.getIfPresent(confirmationCode);
    }

    public boolean isAwaitingConfirmation(@NonNull Gamer gamer) {
        return confirmationCache.asMap().containsValue(gamer);
    }

    public void cleanAwaitingConfirmation(@NonNull String code) {
        confirmationCache.invalidate(code);
    }

}
