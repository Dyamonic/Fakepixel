package club.abelix.core.app.command;

import club.abelix.commons.command.Async;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.commons.data.auth.BaseAuthPlayerData;
import club.abelix.commons.data.nickname.FakeNickname;
import club.abelix.commons.data.nickname.FakePlayerNickname;
import club.abelix.commons.data.nickname.NickSqlHandler;
import club.abelix.commons.data.punishment.meta.BanMeta;
import club.abelix.commons.data.rank.UserRank;
import club.abelix.commons.data.rank.type.UserRankType;
import club.abelix.commons.data.session.Session;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.json.JSON;
import club.abelix.commons.util.time.Duration;
import club.abelix.commons.util.time.TimeUtil;
import club.abelix.commons.version.GameVersion;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.party.Party;
import club.abelix.core.app.data.punishment.PunishmentManager;
import club.abelix.core.app.util.IpViewUtil;
import lombok.NonNull;
import lombok.val;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NamedCommand("whois")
public final class WhoIsCommand {

    private static final String RANK_FORMAT = "  - §r(%d:%d) %s §r[%s]";
    private static final DateTimeFormatter RANK_EXPIRE_FORMATTER =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);

    @Async
    @Executor
    @Tabcomplete("[playerName]")
    @WithCondition("minArgs: 1")
    @WithPermission("command.whois")
    public static void execute(CoreExecutionSession session) throws CommandTranslationException {
        Gamer target = loadOfflineTarget(session);

        BaseAuthPlayerData authData = loadAuth(target.getName());
        boolean personalInfo = hasPersonalInfoAccess(session);

        sendBaseInfo(session, target);

        if (personalInfo) {
            sendRegistrationInfo(session, target, authData);
        }

        sendPunishments(session, target);
        sendDomainIfOnline(session, target);
        sendNicknameIfPresent(session, target);

        sendOnlineOrLastSession(session, target, authData, personalInfo);
    }

    private static Gamer loadOfflineTarget(CoreExecutionSession session) throws CommandTranslationException {
        return session.getOfflineGamer(0, offlineGamer -> {
            offlineGamer.loadRanks();
            offlineGamer.loadPrefix();
            offlineGamer.loadLanguage();
            offlineGamer.loadFakeName();
            offlineGamer.loadFriends();
            offlineGamer.updateDisplayName();
        });
    }

    private static BaseAuthPlayerData loadAuth(String playerName) {
        BaseAuthPlayerData authData = new BaseAuthPlayerData(playerName);
        authData.load();
        return authData;
    }

    private static boolean hasPersonalInfoAccess(CoreExecutionSession session) {
        return session.getSender().hasPermission("command.whois.personalinfo");
    }

    private static void sendBaseInfo(CoreExecutionSession session, Gamer target) {
        Language lang = session.getLanguage();

        String ranks = target.getRanks().stream()
                .map(rank -> formatRankLine(rank, lang))
                .collect(Collectors.joining("\n"));

        target.loadAuthData();
        session.sendLocalizedMessage("core_whois_result_base",
                "%name%", target.getDisplayName(),
                "%ranks%", "\n" + ranks,
                "%id%", target.getPlayerId(),
                "%language%", target.getLanguage().getLanguageName(),
                "%friends%", target.getFriends().size(),
                "%online_friends%", target.getOnlineFriends().size(),
                "%friends_limit%", target.getFriendLimit(),
                "%protocol%", protocolMostRecentNameOrUnknown(target.getProtocol()),
                "%has2fa%", target.has2FA()
                        ? lang.getMessage("has2fa_enabled")
                        : lang.getMessage("has2fa_disabled")
        );
    }

    private static void sendRegistrationInfo(CoreExecutionSession session, Gamer target, BaseAuthPlayerData authData) {
        Language lang = session.getLanguage();

        Instant registrationTime = Instant.ofEpochMilli(authData.getRegDate());
        LocalDateTime registrationDate = LocalDateTime.ofInstant(registrationTime, ZoneId.systemDefault());

        session.sendLocalizedMessage("core_whois_result_registration_data",
                "%reg_ip%", IpViewUtil.of(authData.getRegIp(), session),
                "%reg_date%", TimeUtil.getDateTimeString(lang, registrationDate),
                "%protocol%", protocolMostRecentNameOrUnknown(target.getProtocol())
        );
    }

    private static void sendPunishments(CoreExecutionSession session, Gamer target) {
        sendBans(session, target);
        sendMute(session, target);
    }

    private static void sendBans(CoreExecutionSession session, Gamer target) {
        val banMetas = PunishmentManager.INSTANCE.getActiveNameBans(target.getName());
        if (banMetas.isEmpty()) return;

        session.sendLocalizedMessage("core_whois_result_bans");

        banMetas.stream()
                .sorted(Comparator.comparingLong(BanMeta::getIssueDate).reversed())
                .limit(3)
                .forEach(ban -> {
                    val hover = PunishmentManager.INSTANCE.getBanHoverText(ban, session.getLanguage(), true, true);
                    session.getSender().sendRawMessage(
                            JSON.HOVER_TEXT_LAYOUT.formatted("  - §c" + ban.getReason(session.getLanguage()), hover)
                    );
                });
    }

    private static void sendMute(CoreExecutionSession session, Gamer target) {
        val mute = PunishmentManager.INSTANCE.getActiveIdMute(target.getPlayerId());
        if (mute == null) return;

        session.sendLocalizedMessage("core_whois_result_mutes");

        val hover = PunishmentManager.INSTANCE.getMuteHoverText(mute, session.getLanguage(), true);
        session.getSender().sendRawMessage(
                JSON.HOVER_TEXT_LAYOUT.formatted("  - §c" + mute.getReason(session.getLanguage()), hover)
        );
    }

    private static void sendDomainIfOnline(CoreExecutionSession session, Gamer target) {
        if (!target.isOnline()) return;

        session.sendLocalizedMessage("core_whois_result_domain",
                "%domain%", target.getDomain()
        );
    }

    private static void sendNicknameIfPresent(CoreExecutionSession session, Gamer target) {
        FakePlayerNickname nickname = target.getFakeNickname();
        if (nickname == null) return;

        FakeNickname nick = nickname.getNickname();
        Language lang = session.getLanguage();

        NickSqlHandler.INSTANCE.getNicknameHistory(nick.getName())
                .thenAccept(historyEntries -> {
                    if (historyEntries.isEmpty()) return;

                    FakePlayerNickname.HistoryEntry entry = historyEntries.getFirst();

                    session.sendLocalizedMessage("core_whois_result_nickname",
                            "%fake_nickname%", nick.getName(),
                            "%fake_group%", nickname.getGroup().getDisplayName(lang),
                            "%date_activated%", TimeUtil.getDateTimeString(lang, entry.getIssueDate())
                    );
                })
                .join();
    }

    private static void sendOnlineOrLastSession(CoreExecutionSession session,
                                                Gamer target,
                                                BaseAuthPlayerData authData,
                                                boolean personalInfo) {
        if (target.isOnline()) {
            sendOnlineStatus(session, target, personalInfo);
            sendPartyIfPresent(session, target);
            return;
        }

        Session lastSession = authData.getLastSession();
        if (lastSession != null) {
            sendLastSession(session, lastSession, personalInfo);
        }
    }

    private static void sendOnlineStatus(CoreExecutionSession session, Gamer target, boolean personalInfo) {
        Language lang = session.getLanguage();
        GameVersion version = protocolVersionOrNull(target.getProtocol());

        session.sendLocalizedMessage("core_whois_result_status",
                "%server_name%", target.getServer(),
                "%proxy_name%", target.getProxy(),
                "%online_time%", Duration.durationShortSec(lang, target.getPlayingSeconds()),
                "%network%", version != null ? version.getName() : "Unknown",
                "%protocol%", protocolNameOrUnknown(target.getProtocol())
        );

        if (personalInfo) {
            session.sendLocalizedMessage("core_whois_result_status_ip",
                    "%ip%", IpViewUtil.of(target.getAddress(), session)
            );
        }
    }

    private static void sendPartyIfPresent(CoreExecutionSession session, Gamer target) {
        Party party = target.getParty();
        if (party == null) return;

        Language lang = session.getLanguage();

        List<String> members = Stream.concat(
                        party.getGamers().stream()
                                .map(g -> "§a" + partyMemberName(g, party)),
                        party.getOfflineMembers().stream()
                                .map(offline -> "§c" + partyMemberName(offline.getGamer(), party))
                )
                .collect(Collectors.toList());

        session.sendLocalizedMessage("core_whois_result_party",
                "%id%", party.getId(),
                "%players%", String.join("§7, ", members),
                "%public%", booleanString(lang, party.isPublicStatus())
        );
    }

    private static void sendLastSession(CoreExecutionSession session, Session lastSession, boolean personalInfo) {
        Language lang = session.getLanguage();

        session.sendLocalizedMessage("core_whois_result_session",
                "%last_login%", TimeUtil.getDateTimeString(lang, lastSession.getLeftTimestamp()),
                "%last_server%", lastSession.getLastServer().getName(),
                "%duration%", Duration.durationShortSec(lang, lastSession.getElapsedTime())
        );

        if (personalInfo) {
            session.sendLocalizedMessage("core_whois_result_session_ip",
                    "%last_ip%", IpViewUtil.of(lastSession.getAddress(), session)
            );
        }
    }

    private static String formatRankLine(UserRank rank, Language language) {
        return RANK_FORMAT.formatted(
                rank.id(),
                rank.rank().getId(),
                rank.rank().getDisplayName(language),
                formatRankExpire(rank, language)
        );
    }

    private static String formatRankExpire(UserRank rank, Language language) {
        if (rank.type() == UserRankType.PERMANENT) {
            return "∞";
        }

        Locale locale = toLocale(language);

        return Instant.ofEpochMilli(rank.getExpirationTime(TimeUnit.MILLISECONDS))
                .atZone(ZoneId.systemDefault())
                .format(RANK_EXPIRE_FORMATTER.withLocale(locale));
    }

    private static Locale toLocale(Language language) {
        return switch (language) {
            case RUSSIAN -> Locale.forLanguageTag("ru-RU");
            case ENGLISH -> Locale.forLanguageTag("en-US");
        };
    }

    private static String booleanString(@NonNull Language language, boolean status) {
        return status ? language.getMessage("enabled") : language.getMessage("disabled");
    }

    private static String partyMemberName(@NonNull Gamer gamer, @NonNull Party party) {
        String name = gamer.getName();
        return party.isLeader(gamer) ? "*" + name : name;
    }

    private static GameVersion protocolVersionOrNull(int protocol) {
        return protocol != 0 ? GameVersion.getByProtocolNumber(protocol) : null;
    }

    private static String protocolNameOrUnknown(int protocol) {
        GameVersion v = protocolVersionOrNull(protocol);
        return v != null ? v.getName() : "Unknown";
    }

    private static String protocolMostRecentNameOrUnknown(int protocol) {
        GameVersion v = protocolVersionOrNull(protocol);
        return v != null ? v.getMostRecentSupportedVersionName() : "Unknown";
    }
}