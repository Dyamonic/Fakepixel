package club.abelix.antivpn.commands;

import club.abelix.antivpn.data.WhitelistedASNResult;
import club.abelix.antivpn.manager.AntiVPNManager;
import club.abelix.antivpn.sql.ASNWhitelistSqlHandler;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.core.app.api.command.CoreExecutionSession;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@RequiredArgsConstructor
@NamedCommand("whitelistasn")
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public final class WhitelistASNCommand {

    AntiVPNManager antiVPNManager;

    Pattern ASN_PATTERN = Pattern.compile("AS\\d+");

    @Executor
    @WithCondition("minArgs: 1")
    @WithPermission("command.whitelistasn")
    public void execute(CoreExecutionSession session) throws CommandTranslationException {
        String asn = session.getArgument(0);
        if (!ASN_PATTERN.matcher(asn).matches()) {
            throw new CommandTranslationException("antivpn_whitelist_asn_invalid");
        }

        ASNWhitelistSqlHandler.INSTANCE.insert(
                new WhitelistedASNResult(asn, session.getGamer().getName(), LocalDateTime.now())
        );

        antiVPNManager.getAsnWhitelist().add(asn);

        session.sendLocalizedMessage("antivpn_whitelist_asn_success", "%asn%", asn);
    }

}
