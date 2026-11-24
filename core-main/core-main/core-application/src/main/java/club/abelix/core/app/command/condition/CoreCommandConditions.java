package club.abelix.core.app.command.condition;

import club.abelix.commons.command.condition.ConditionProvider;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.data.punishment.meta.MuteMeta;
import club.abelix.commons.language.Language;
import club.abelix.commons.project.Project;
import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.party.Party;
import club.abelix.core.app.data.punishment.PunishmentManager;
import lombok.val;

/**
 * @author WhileInside
 */
public final class CoreCommandConditions {

    private CoreCommandConditions() {
        throw new UnsupportedOperationException();
    }

    @ConditionProvider(value = "notBanned", priority = -100)
    public static void notBanned(CoreExecutionSession session, String meta) throws CommandTranslationException {
        Gamer gamer = session.getGamer();

        if (gamer != null
                && gamer.isOnline()
                && PunishmentManager.INSTANCE.getActiveBan(gamer.getName(), gamer.getAddress()) != null) {

            throw new CommandTranslationException("ban_disabled_command");
        }
    }

    @ConditionProvider(value = "safeProduction", priority = -100)
    public static void notEnabledOnProduction(CoreExecutionSession session, String meta) throws CommandTranslationException {
        val core = CoreApplication.getInstance();

        if (Project.CURRENT != Project.ABELIX_MC) {
            throw new CommandTranslationException("command_unavailable_on_production");
        }

        if (!core.isTestModeEnabled() && !session.getSender().hasPermission("command.bypasson.production")) {
            throw new CommandTranslationException("command_unavailable_on_production");
        }
    }

    @ConditionProvider(value = "notMuted", priority = -100)
    public static void notMuted(CoreExecutionSession session, String meta) throws CommandTranslationException {
        Gamer gamer = session.getGamer();

        if (gamer == null) {
            return;
        }

        MuteMeta active = PunishmentManager.INSTANCE.getActiveIdMute(gamer.getPlayerId());

        if (active == null) {
            return;
        }

        Language language = gamer.getLanguage();

        throw new CommandTranslationException("muted_message_format",
                "%time_left%", active.getExpirationString(language),
                "%meta_id%", active.getId(),
                "%reason%", active.getReason(language)
        );
    }

    @ConditionProvider(value = "player_not_vanished", priority = 1)
    public static void playerNotVanished(CoreExecutionSession session, String meta) throws CommandTranslationException {
        Gamer gamer = session.getGamer();

        if (gamer.isVanished()) {
            throw new CommandTranslationException("core_vanish_function_disabled");
        }
    }

    @ConditionProvider(value = "partyLeaderOnly", priority = 15)
    public static void partyLeaderOnly(CoreExecutionSession session, String meta) throws CommandTranslationException {
        Party senderParty = session.getProvidedObject(0);

        if (senderParty == null || !senderParty.isLeader(session.getGamer())) {
            throw new CommandTranslationException("party_not_leader");
        }
    }

    @ConditionProvider(value = "withoutPartyOrLeader", priority = 10)
    public static void withoutPartyOrLeader(CoreExecutionSession session, String meta) throws CommandTranslationException {
        Party senderParty = session.getProvidedObject(0);

        if (senderParty != null && !senderParty.isLeader(session.getGamer())) {
            throw new CommandTranslationException("party_not_leader");
        }
    }

    @ConditionProvider(value = "withPartyOnly", priority = 10)
    public static void withPartyOnly(CoreExecutionSession session, String meta) throws CommandTranslationException {
        Party senderParty = session.getProvidedObject(0);

        if (senderParty == null) {
            throw new CommandTranslationException("party_without_party");
        }
    }

    @ConditionProvider(value = "withoutPartyOnly", priority = 10)
    public static void withoutPartyOnly(CoreExecutionSession session, String meta) throws CommandTranslationException {
        Party senderParty = session.getProvidedObject(0);

        if (senderParty != null) {
            throw new CommandTranslationException("party_with_party");
        }
    }

}
