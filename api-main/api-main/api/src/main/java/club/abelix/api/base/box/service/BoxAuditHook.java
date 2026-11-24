package club.abelix.api.base.box.service;

import club.abelix.api.base.box.Box;
import club.abelix.api.base.box.reward.BoxReward;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.integration.discord.DiscordHook;
import club.abelix.api.integration.discord.DiscordHook.EmbedObject;
import club.abelix.commons.data.currency.GameCurrency;
import club.abelix.network.type.mode.ServerMode;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import lombok.val;

import java.awt.Color;
import java.time.Instant;

@Log4j2
@UtilityClass
public class BoxAuditHook {

    private static final String WEBHOOK_URL =
            "https://discord.com/api/webhooks/1553435940561621138/7YIQmFNYgsmLAZuLE8WgsYg4ySueYfzI4F-Pa4dt0SmeZG9ksLha13_c2udwRRaHVSny";

    private static final String USERNAME = "Box Audit";

    private static final int COLOR_PURCHASE = 0x3498DB;
    private static final int COLOR_OPEN = 0x2ECC71;
    private static final int COLOR_FAILED = 0xE74C3C;

    public void logPurchase(
            Gamer gamer,
            Box box,
            GameCurrency currency,
            int amount,
            int amountPaid,
            ServerMode mode
    ) {
        try {
            val hook = new DiscordHook(WEBHOOK_URL);
            hook.setUsername(USERNAME);

            val embed = new EmbedObject()
                    .setTitle("Box purchased")
                    .setColor(new Color(COLOR_PURCHASE))
                    .setTimestamp(Instant.now())
                    .addField("Player", gamer.getName() + " (`" + gamer.getPlayerId() + "`)", true)
                    .addField("Box", box.getNameKey(), true)
                    .addField("Rarity", box.getRarity().getName(gamer.getLanguage()), true)
                    .addField("Type", String.valueOf(box.getType().getId()), true)
                    .addField("Amount", String.valueOf(amount), true)
                    .addField("Paid", amountPaid + " " + currency.name(), true)
                    .addField("Mode", mode == null ? "none" : mode.getName(), true);

            hook.addEmbed(embed);
            hook.execute();
        } catch (Throwable t) {
            log.error("Failed to send box purchase audit to Discord", t);
        }
    }

    public void logOpen(
            Gamer gamer,
            Box box,
            BoxReward reward,
            boolean applied,
            String mode
    ) {
        try {
            val hook = new DiscordHook(WEBHOOK_URL);
            hook.setUsername(USERNAME);

            val embed = new EmbedObject()
                    .setTitle(applied ? "Box opened" : "Box opened (duplicate)")
                    .setColor(new Color(applied ? COLOR_OPEN : COLOR_FAILED))
                    .setTimestamp(Instant.now())
                    .addField("Player", gamer.getName() + " (`" + gamer.getPlayerId() + "`)", true)
                    .addField("Box", box.getNameKey(), true)
                    .addField("Rarity", box.getRarity().getName(gamer.getLanguage()), true)
                    .addField("Reward rarity", reward.getRarity().getName(gamer.getLanguage()), true)
                    .addField("Reward", reward.getName(gamer.getLanguage()), false)
                    .addField("Granted", applied ? "yes" : "no, duplicate equivalent issued", true)
                    .addField("Mode", mode == null ? "none" : mode, true);

            hook.addEmbed(embed);
            hook.execute();
        } catch (Throwable t) {
            log.error("Failed to send box open audit to Discord", t);
        }
    }
}
