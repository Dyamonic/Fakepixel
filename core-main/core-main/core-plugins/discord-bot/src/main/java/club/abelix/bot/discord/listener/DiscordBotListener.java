package club.abelix.bot.discord.listener;

import club.abelix.bot.discord.DiscordBot;
import club.abelix.bot.discord.handler.BotSqlHandler;
import club.abelix.bot.discord.user.DiscordBotUser;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateBoostTimeEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class DiscordBotListener extends ListenerAdapter {

    private final DiscordBot bot;

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (event.getChannelType() != ChannelType.TEXT) {
            return;
        }

        Message message = event.getMessage();
        Member member = event.getMember();

        if (member == null || member.getUser().isBot()) {
            return;
        }

        TextChannel channel = event.getChannel().asTextChannel();

        if (channel.getIdLong() != bot.getDiscordLinkChatId()) {
            return;
        }

        String confirmation = message.getContentRaw().trim();

        log.info("Message ID: {}", message.getId());
        log.info("Raw: '{}'", message.getContentRaw());
        log.info("Display: '{}'", message.getContentDisplay());
        log.info("Stripped: '{}'", message.getContentStripped());
        log.info("Type: {}", message.getType());

        log.info("Received link code '{}' from {} ({})",
                confirmation,
                member.getUser().getAsTag(),
                member.getIdLong());

        Gamer gamer = bot.getAwaitingConfirmationPlayer(confirmation);
        long memberId = member.getIdLong();

        EmbedBuilder response = new EmbedBuilder();

        if (gamer != null) {
            DiscordBotUser existingUser = BotSqlHandler.INSTANCE.getByDiscordId(memberId);

            if (existingUser != null) {
                log.info("Discord user {} ({}) attempted to link but is already linked.",
                        member.getUser().getAsTag(),
                        memberId);

                response.setColor(Color.RED)
                        .setTitle(":x: Account Already Linked")
                        .setDescription(
                                "Your Discord account is already linked.\n\n" +
                                        "To unlink it, use **/discord unlink** in-game."
                        );
            } else {
                log.info("Linking Discord account {} ({}) with Minecraft player {} ({})",
                        member.getUser().getAsTag(),
                        memberId,
                        gamer.getName(),
                        gamer.getPlayerId());

                DiscordBotUser botUser = BotSqlHandler.INSTANCE
                        .linkDiscordAccount(memberId, gamer.getPlayerId());

                if (gamer.isOnline()) {
                    gamer.sendLocalizedMessage(
                            "discord_link_success",
                            "%tag%",
                            member.getUser().getAsTag()
                    );
                }

                response.setColor(Color.GREEN)
                        .setTitle(":white_check_mark: Account Linked Successfully!")
                        .setDescription(
                                "Your Discord account has been successfully linked to **" +
                                        gamer.getName() +
                                        "**.\n\n" +
                                        "Your Discord roles have been synchronized with your in-game ranks."
                        );

                log.info("Successfully linked Discord account {} to {}",
                        memberId,
                        gamer.getName());

                log.info("Starting Discord synchronization for {}",
                        gamer.getName());

                bot.discordUserSync(botUser.getDiscordId(), gamer);

                log.info("Synchronization request queued for {}",
                        gamer.getName());
            }

            bot.cleanAwaitingConfirmation(confirmation);
        } else {
            log.warn("Invalid or expired confirmation code '{}' submitted by {} ({})",
                    confirmation,
                    member.getUser().getAsTag(),
                    memberId);

            response.setColor(Color.RED)
                    .setTitle(":x: Invalid Link Code")
                    .setDescription(
                            "The code you entered is invalid or has expired.\n\n" +
                                    "Use **/discord link** in-game to generate a new code."
                    );
        }

        channel.deleteMessageById(message.getId()).queue(
                success -> log.debug("Deleted confirmation message {}", message.getId()),
                failure -> log.warn("Failed to delete confirmation message {}", message.getId(), failure)
        );

        channel.sendMessageEmbeds(response.build()).queue(sentMessage -> {
            log.debug("Sent temporary response {}", sentMessage.getId());

            channel.deleteMessageById(sentMessage.getId()).queueAfter(
                    10,
                    TimeUnit.SECONDS,
                    success -> log.debug("Deleted temporary response {}", sentMessage.getId()),
                    failure -> log.warn("Failed to delete temporary response {}", sentMessage.getId(), failure)
            );
        });
    }

    @Override
    public void onGuildMemberJoin(@Nonnull GuildMemberJoinEvent event) {
        Member member = event.getMember();

        log.info("Guild member joined: {} ({})",
                member.getUser().getAsTag(),
                member.getIdLong());

        DiscordBotUser botUser = BotSqlHandler.INSTANCE.getByDiscordId(member.getIdLong());

        if (botUser == null) {
            log.info("Member {} is not linked to any Minecraft account.",
                    member.getUser().getAsTag());
            return;
        }

        Gamer gamer = botUser.getPlayer();

        log.info("Found linked Minecraft account '{}' for {}. Synchronizing Discord roles.",
                gamer.getName(),
                member.getUser().getAsTag());

        bot.discordUserSync(member.getIdLong(), gamer);
        bot.reconcileBoosterMember(member);

        log.info("Synchronization queued for {}",
                gamer.getName());
    }

    @Override
    public void onGuildMemberUpdateBoostTime(@Nonnull GuildMemberUpdateBoostTimeEvent event) {
        Member member = event.getMember();

        if (member.getUser().isBot()) {
            return;
        }

        boolean boostedNow = event.getNewTimeBoosted() != null;
        boolean boostedBefore = event.getOldTimeBoosted() != null;

        if (boostedNow == boostedBefore) {
            return;
        }

        log.info("Member {} ({}) {} the server. Reconciling booster rank.",
                member.getUser().getAsTag(),
                member.getIdLong(),
                boostedNow ? "boosted" : "unboosted");

        bot.reconcileBoosterMember(member);
    }
}