package club.abelix.antivpn.commands;

import club.abelix.antivpn.data.WhitelistedASNResult;
import club.abelix.antivpn.manager.AntiVPNManager;
import club.abelix.antivpn.sql.ASNWhitelistSqlHandler;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.core.app.api.command.CoreExecutionSession;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@NamedCommand("asnwhitelist")
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public final class ASNWhitelistCommand {

    AntiVPNManager antiVPNManager;

    Pattern ASN_PATTERN = Pattern.compile("as\\d+");

    @SubcommandExecutor("add")
    @WithCondition("minArgs: 1")
    @WithPermission("command.asnwhitelist.add")
    public void execute(CoreExecutionSession session) throws CommandTranslationException {
        String asn = session.getArgument(0).toLowerCase();
        if (!ASN_PATTERN.matcher(asn).matches()) {
            throw new CommandTranslationException("antivpn_whitelist_asn_invalid");
        }

        if (antiVPNManager.getAsnWhitelist().contains(asn)) {
            session.sendLocalizedMessage("antivpn_whitelist_asn_already_whitelisted", "%asn%", asn);
            return;
        }

        ASNWhitelistSqlHandler.INSTANCE.insert(
                new WhitelistedASNResult(asn, session.getGamer().getName(), LocalDateTime.now())
        );

        antiVPNManager.getAsnWhitelist().add(asn);

        session.sendLocalizedMessage("antivpn_whitelist_asn_success", "%asn%", asn);
    }

    @SubcommandExecutor("remove")
    @WithCondition("minArgs: 1")
    @WithPermission("command.asnwhitelist.remove")
    public void remove(CoreExecutionSession session) throws CommandTranslationException {
        String asn = session.getArgument(0).toLowerCase();
        if (!ASN_PATTERN.matcher(asn).matches()) {
            throw new CommandTranslationException("antivpn_whitelist_asn_invalid");
        }

        if (!antiVPNManager.getAsnWhitelist().contains(asn)) {
            session.sendLocalizedMessage("antivpn_whitelist_asn_not_whitelisted", "%asn%", asn);
            return;
        }

        ASNWhitelistSqlHandler.INSTANCE.delete(asn);
        antiVPNManager.getAsnWhitelist().remove(asn);

        session.sendLocalizedMessage("antivpn_whitelist_asn_removed", "%asn%", asn);
    }

    @SubcommandExecutor("reload")
    @WithPermission("command.asnwhitelist.reload")
    public void reload(CoreExecutionSession session) {
        antiVPNManager.loadWhitelist();
        session.sendLocalizedMessage("antivpn_whitelist_asn_reloaded");
    }

    @SubcommandExecutor("list")
    @WithPermission("command.asnwhitelist.list")
    public void list(CoreExecutionSession session) {
        session.sendLocalizedMessage("antivpn_whitelist_asn_list", "%list%", String.join(", ", antiVPNManager.getAsnWhitelist()));
    }

}
