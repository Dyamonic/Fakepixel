package club.abelix.api.server.lobby.nick;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.book.PagedBook;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.base.util.sign.SignInput;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.ConditionProvider;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.FallbackExecutor;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.commons.data.nickname.FakeNickname;
import club.abelix.commons.data.nickname.FakePlayerNickname;
import club.abelix.commons.data.rank.Rank;
import club.abelix.commons.data.rank.service.RanksService;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.loading.caching.CachingManager;
import club.abelix.commons.util.loading.caching.guava.GuavaCachingManager;
import club.abelix.commons.util.loading.caching.guava.strategy.ExpireAfterWrite;
import club.abelix.commons.util.regex.Patterns;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.core.bukkit.StarshipBukkit;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.network.packet.play.client.CChangePlayerNick;
import club.abelix.network.packet.play.client.CPlayerNickGenerate;
import club.abelix.network.packet.play.server.SChangePlayerNickResponse;
import club.abelix.network.packet.play.server.SPlayerNickGenerateResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.val;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@NamedCommand("nick")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class NickCommand {

    AbstractPlugin plugin;
    RanksService ranksService;
    CachingManager<UUID, List<String>> generatedNickCache;

    @NonFinal
    Set<Rank> nickRanks;

    public static @NotNull NickCommand create(final AbstractPlugin plugin) {
        plugin.getCommandManager().addCondition(NickCommandConditions.class);

        NickCommand command = new NickCommand(
                plugin,
                StarshipBukkit.getInstance().getRanksService(),
                new GuavaCachingManager<>(ExpireAfterWrite.get(2, TimeUnit.MINUTES))
        );

        command.initializeNickRanks();

        return command;
    }

    @WithCondition("disableConsole")
    @WithCondition("onlyLobby")
    @WithPermission("command.nick.cachereset")
    @SubcommandExecutor("cachereset")
    public void cacheReset(@NotNull final BukkitExecutionSession session) {
        val gamer = session.getGamer();

        generatedNickCache.invalidate(gamer.getPlayer().getUniqueId());
        gamer.sendLocalizedMessage("nick_cache_reset");
    }

    @WithCondition("onlyLobby")
    @WithCondition("disableConsole")
    @SubcommandExecutor("reset")
    public void resetNickname(final BukkitExecutionSession session) throws CommandException {
        val gamer = session.getGamer();
        val nicked = gamer.getFakeName() != null;

        if (!nicked) {
            throw new CommandTranslationException("nick_not_nicked");
        }

        // сбрасываем ник
        gamer.setFakeName(null);

        // отправляем инфу на кор
        Connection.sendPacket(new CChangePlayerNick(gamer.getPlayerId()));

        gamer.sendLocalizedMessage("nick_reset");
    }

    @FallbackExecutor
    @SubcommandExecutor("infobook")
    @WithCondition("onlyLobby")
    @WithPermission("command.nick.infobook")
    public void nick(@NotNull final BukkitExecutionSession session) throws CommandException {
        val gamer = session.getGamer();
        val language = gamer.getLanguage();

        val book = new PagedBook();

        val component = new TextComponent(language.getMessage("nick_book_info"));

        val generateComponent = new TextComponent(language.getMessage("nick_book_info_understand"));
        generateComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nick action"));
        generateComponent.setHoverEvent(
                new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new BaseComponent[]{
                                new TextComponent(language.getMessage("nick_book_info_understand_lore"))
                        }
                )
        );

        val page = book.addPage();
        page.addComponents(component, generateComponent);

        book.openBook(gamer.getPlayer());
    }

    @SubcommandExecutor("action")
    @WithCondition("onlyLobby")
    @WithPermission("command.nick.action")
    public void nickAction(@NotNull final BukkitExecutionSession session) throws CommandException {
        val gamer = session.getGamer();
        val language = gamer.getLanguage();

        val book = new PagedBook();

        val component = new TextComponent(language.getMessage("nick_book_action"));

        val generateComponent = new TextComponent(
                language.getMessage("nick_book_action_random") + "\n"
        );
        generateComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nick generate"));
        generateComponent.setHoverEvent(
                new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new BaseComponent[]{
                                new TextComponent(language.getMessage("nick_book_action_random_lore"))
                        }
                )
        );

        val page = book.addPage();
        page.addComponents(component, generateComponent);

        if (!canOnlyUseRandomNicks(gamer)) {
            val customNickComponent = new TextComponent(
                    language.getMessage("nick_book_action_custom") + "\n"
            );
            customNickComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nick custom"));
            customNickComponent.setHoverEvent(
                    new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new BaseComponent[]{
                                    new TextComponent(language.getMessage("nick_book_action_custom_lore"))
                            }
                    )
            );

            page.addComponents(customNickComponent);
        }

        if (gamer.getFakeName() != null) {
            val resetNickComponent = new TextComponent(language.getMessage("nick_book_action_reset"));
            resetNickComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nick reset"));
            resetNickComponent.setHoverEvent(
                    new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new BaseComponent[]{
                                    new TextComponent(language.getMessage("nick_book_action_reset_lore"))
                            }
                    )
            );

            page.addComponents(resetNickComponent);
        }

        book.openBook(gamer.getPlayer());
    }

    @WithCondition("onlyLobby")
    @WithPermission("command.nick.generate")
    @SubcommandExecutor("generate")
    public void generate(final BukkitExecutionSession session) throws CommandException {
        val gamer = session.getGamer();
        val length = session.getArgumentCount();

        suggestNick(gamer, length >= 1 ? session.getIntArgument(0, "nick") : 0);
    }

    @WithCondition("onlyLobby")
    @WithPermission("command.nick.random")
    @SubcommandExecutor("random")
    public void random(final BukkitExecutionSession session) throws CommandException {
        val gamer = session.getGamer();
        val length = session.getArgumentCount();

        if (length >= 2) {
            processRandomNickRequest(gamer, session.getArgument(0), session.getArgument(1));
        } else if (length == 1) {
            processRankRequest(gamer, session.getArgument(0), true);
        } else {
            suggestNick(gamer, 0);
        }
    }

    @WithCondition("onlyLobby")
    @WithPermission("command.nick.custom")
    @SubcommandExecutor("custom")
    public void custom(final BukkitExecutionSession session) throws CommandException {
        val gamer = session.getGamer();
        val length = session.getArgumentCount();

        if (canOnlyUseRandomNicks(gamer)) {
            suggestNick(gamer, 0);
            return;
        }

        if (length > 1) {
            processCustomNickRequest(gamer, session.getArgument(0), session.getArgument(1));
        } else if (length == 1) {
            processRankRequest(gamer, session.getArgument(0), false);
        } else {
            processCustomNickInput(gamer);
        }
    }

    public void initializeNickRanks() {
        if (nickRanks == null) {
            nickRanks = ranksService.findAllRanks().stream()
                    .filter(rank -> rank.getPermissions().asSet().stream()
                            .map(p -> p.getName().toLowerCase())
                            .noneMatch(name -> name.equals("nick.disallowed")))
                    .sorted(Comparator.comparingInt(Rank::getPriority))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    private boolean canOnlyUseRandomNicks(final Gamer gamer) {
        val ranks = gamer.getRanks();

        return !ranks.hasPermission("nick.custom.use");
    }

    private Rank getRankFromArgument(final Gamer gamer, final String rankName) {
        val ranks = gamer.getRanks();
        val rankOptional = StarshipBukkit.getInstance().getRanksService().findByName(rankName.toLowerCase());
        val defaultRank = StarshipBukkit.getInstance().getRanksService().defaultRank();

        if (rankOptional.isEmpty()) return null;

        val rank = rankOptional.get();

        if (ranks.hasPermission("nick." + rank.getName().toLowerCase() + ".use")) {
            return rank;
        }

        if (ranks.hasPermission("nick.bypass")) {
            return rank;
        }

        return gamer.getPriorityGroup().rank().getPriority() >= rank.getPriority() ? rank : defaultRank;
    }

    private void suggestNick(final Gamer gamer, int index) {
        val player = gamer.getPlayer();

        val availableNicks = generatedNickCache.get(player.getUniqueId());

        // запрашиваем ники у кора и еще раз выполняем этот метод
        if (availableNicks == null) {
            int finalIndex = index;
            requestNicks(gamer).thenAccept(__ -> suggestNick(gamer, finalIndex));
            return;
        }

        // если ников нет, то не предлагаем ничего игроку
        if (availableNicks.isEmpty()) {
            gamer.sendLocalizedMessage("nick_error_no_nicks_found");
            return;
        }

        val language = gamer.getLanguage();

        val size = availableNicks.size();

        if (index >= size || index < 0) {
            index = 0;
        }

        val nick = availableNicks.get(index);

        val book = new PagedBook();

        val component = new TextComponent(language.getMessage("nick_book_generator",
                "%name%", nick));

        val nickComponent = new TextComponent(
                language.getMessage("nick_book_action_select", "%name%", nick) + "\n"
        );
        nickComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nick random " + nick));
        nickComponent.setHoverEvent(
                new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new BaseComponent[]{
                                new TextComponent(language.getMessage("nick_book_action_select_lore"))
                        }
                )
        );

        val regenComponent = new TextComponent(language.getMessage("nick_book_action_generate"));
        regenComponent.setClickEvent(
                new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nick generate " + (index + 1))
        );
        regenComponent.setHoverEvent(
                new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new BaseComponent[]{
                                new TextComponent(language.getMessage("nick_book_action_generate_lore"))
                        }
                )
        );

        book.addPage()
                .addComponents(component, nickComponent, regenComponent);

        book.openBook(player);
    }

    private void processCustomNickInput(final Gamer gamer) {
        SignInput.openInput(gamer).thenAccept(strings -> {
            val name = strings[0].trim();

            if (name.isEmpty()) {
                gamer.sendLocalizedMessage("nick_error_invalid_name");
            } else {
                HeartBeat.runSync(() -> processRankRequest(gamer, name, false));
            }
        });
    }

    private void processCustomNickRequest(final Gamer gamer, final String customNick, final String rankName) {
        if (!Patterns.isValidNick(customNick)) {
            gamer.sendLocalizedMessage("nick_error_invalid_name");
            return;
        }

        finallyProcessNickRequest(gamer, customNick, rankName);
    }

    private void processRandomNickRequest(final Gamer gamer, final String randomNick, final String rankName) {
        val availableNicks = generatedNickCache.get(gamer.getPlayer().getUniqueId());

        if (availableNicks == null || !availableNicks.contains(randomNick)) {
            gamer.sendLocalizedMessage("nick_error_random_name");
            return;
        }

        finallyProcessNickRequest(gamer, randomNick, rankName);
    }

    private void processRankRequest(final Gamer gamer, final String selectedNick, final boolean random) {
        val book = new PagedBook();

        val language = gamer.getLanguage();
        val component = new TextComponent(language.getMessage("nick_book_rank_selector"));

        val page = book.addPage();
        page.addComponents(component);

        nickRanks.stream()
                .map(rank -> createRankButton(language, rank, selectedNick, random))
                .forEach(page::addComponents);

        book.openBook(gamer.getPlayer());
    }

    private TextComponent createRankButton(
            final Language language,
            final Rank rank,
            final String selectedNick,
            final boolean random
    ) {
        val argument = random ? "random" : "custom";
        val command = "/nick " + argument + " " + selectedNick + " " + rank.getName();

        val component = new TextComponent("➤ " + rank.getDisplayName(language));

        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        component.setHoverEvent(
                new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new BaseComponent[]{
                                new TextComponent(
                                        language.getMessage(
                                                "nick_book_action_rank_select",
                                                "%rank%", rank.getDisplayName(language), "%name%", selectedNick
                                        )
                                )
                        }
                )
        );

        component.addExtra("\n");

        return component;
    }

    private void finallyProcessNickRequest(final Gamer gamer, final String nick, final String rankName) {
        val rank = getRankFromArgument(gamer, rankName);

        if (rank == null) {
            gamer.sendLocalizedMessage("nick_error_invalid_rank");
            return;
        }

        applyNick(gamer, nick, rank);
    }

    private CompletableFuture<Void> requestNicks(final Gamer gamer) {
        val uuid = gamer.getPlayer().getUniqueId();

        List<String> nicks = generatedNickCache.get(uuid);

        if (nicks == null) {
            generatedNickCache.put(uuid, nicks = new ArrayList<>());

            return Connection.awaitPacket(new CPlayerNickGenerate(gamer.getPlayerId()))
                    .thenApply(SPlayerNickGenerateResponse::getAvailableNicks)
                    .thenAccept(nicks::addAll);
        }

        return CompletableFuture.completedFuture(null);
    }

    private void applyNick(final Gamer gamer, final String nick, final Rank group) {
        Connection.awaitPacket(new CChangePlayerNick(gamer.getPlayerId(), nick, group.getId()))
                .whenComplete((response, cause) -> {
                    if (response.getStatus() != SChangePlayerNickResponse.ChangeNickStatus.SUCCESS) {
                        gamer.sendMessage(response.getStatus().getErrorText(gamer.getLanguage()));
                        return;
                    }

                    val nickname = new FakeNickname(response.getNickId(), response.getName());

                    val playerNickname = new FakePlayerNickname(gamer.getPlayerId(), nickname, group);
                    Bukkit.getScheduler().runTask(plugin, () -> gamer.setFakeName(playerNickname));

                    val book = new PagedBook();

                    val language = gamer.getLanguage();
                    val component = new TextComponent(language.getMessage("nick_book_success",
                            "%name%", playerNickname.formatDisplayName()));

                    val page = book.addPage();
                    page.addComponents(component);

                    book.openBook(gamer.getPlayer());

                    gamer.sendLocalizedMessage("nick_selected",
                            "%nick%", playerNickname.formatDisplayName());
                });
    }

    public static final class NickCommandConditions {

        @ConditionProvider(value = "onlyLobby", priority = -1)
        public static void onlyLobbyCondition(final BukkitExecutionSession session) throws CommandException {
            if (!CoreUtil.isLobby()) {
                throw new CommandTranslationException("command_available_only_in_lobby");
            }
        }
    }

}