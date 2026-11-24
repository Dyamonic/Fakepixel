package club.abelix.api.server.lobby;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.FalconAPI;
import club.abelix.api.base.bestplayer.BestPlayerManager;
import club.abelix.api.base.box.npc.BoxContrabandNpc;
import club.abelix.api.base.command.BukkitCommandManager;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.multiplier.booster.Booster;
import club.abelix.api.base.parkour.LobbyParkour;
import club.abelix.api.base.parkour.ParkourListener;
import club.abelix.api.base.roleplay.SitManager;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.base.util.imagemap.ImageMap;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.base.util.location.LocationUtil;
import club.abelix.api.base.util.location.RawLocation;
import club.abelix.api.base.util.rotatinghead.HypixelHead;
import club.abelix.api.base.util.slime.SlimeWorldUtil;
import club.abelix.api.base.util.worldtime.WorldRealTimeSync;
import club.abelix.api.base.util.worldtime.WorldSetTime;
import club.abelix.api.base.util.worldtime.WorldTimeState;
import club.abelix.api.server.lobby.actionnpc.ActionNpc;
import club.abelix.api.server.lobby.bossbar.LobbyBossbar;
import club.abelix.api.server.lobby.command.FlyCommand;
import club.abelix.api.server.lobby.command.GiftCommand;
import club.abelix.api.server.lobby.command.GuideCommand;
import club.abelix.api.server.lobby.command.KaboomCommand;
import club.abelix.api.server.lobby.command.OpenVanityMenuCommand;
import club.abelix.api.server.lobby.command.RefreshLobbyLookupsCommand;
import club.abelix.api.server.lobby.command.ResetParkourTImeCommand;
import club.abelix.api.server.lobby.command.SpawnCommand;
import club.abelix.api.server.lobby.command.TournamentCommand;
import club.abelix.api.server.lobby.donate.DonateHologram;
import club.abelix.api.server.lobby.gamehistory.GameHistoryCommand;
import club.abelix.api.server.lobby.guild.GuildManager;
import club.abelix.api.server.lobby.information.InformationNpc;
import club.abelix.api.server.lobby.linkage.LinkageRewardManager;
import club.abelix.api.server.lobby.listener.LobbyHiderListener;
import club.abelix.api.server.lobby.listener.LobbyJumpPadListener;
import club.abelix.api.server.lobby.listener.LobbyPlayerListener;
import club.abelix.api.server.lobby.listener.LobbyPortalListener;
import club.abelix.api.server.lobby.listener.LobbyProtectionListener;
import club.abelix.api.server.lobby.listener.LobbyVoteListener;
import club.abelix.api.server.lobby.mode.npc.ModeNpc;
import club.abelix.api.server.lobby.mysterybox.MysteryBox;
import club.abelix.api.server.lobby.mysterybox.MysteryBoxStorage;
import club.abelix.api.server.lobby.profile.command.ProfileCommand;
import club.abelix.api.server.lobby.reward.DailyRewardListener;
import club.abelix.api.server.lobby.reward.DailyRewardNotifyService;
import club.abelix.api.server.lobby.reward.DailyRewardNpc;
import club.abelix.api.server.lobby.reward.RewardCommand;
import club.abelix.api.server.lobby.shop.impl.ShopRankNpc;
import club.abelix.api.server.lobby.shopkeeper.ShopKeeperNpc;
import club.abelix.api.server.lobby.vote.VoteNpc;
import club.abelix.api.server.lobby.waiting.WaitNpc;
import club.abelix.api.server.lobby.warmode.WarModeManager;
import club.abelix.api.tournament.command.TournamentCreateCommand;
import club.abelix.api.tournament.command.TournamentResetRatingCommand;
import club.abelix.api.tournament.npc.TournamentNpcTask;
import club.abelix.api.tournament.telegram.TelegramChannelSubscriptionClient;
import club.abelix.commons.data.skin.Skin;
import club.abelix.commons.language.Lang;
import club.abelix.commons.language.Language;
import club.abelix.commons.project.Project;
import club.abelix.commons.util.exclusive.ExclusivePeriod;
import club.abelix.commons.util.time.Duration;
import club.abelix.commons.util.time.DurationField;
import club.abelix.commons.util.time.DurationFormatter;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.type.mode.ServerMode;
import club.abelix.network.type.mode.Unknown;
import club.abelix.network.util.ModeUtil;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@Log4j2
@Getter
public abstract class BaseLobbyServer {

    public static final String LOBBY_HOTBAR_ID = "lobby";

    protected final AbstractPlugin plugin;

    private final Map<Language, LobbyBossbar> bossbarMap = new HashMap<>();

    protected FileConfiguration configuration;

    private Location spawnLocation;

    private List<LobbyParkour> lobbyParkourList;

    @Nullable
    private final ExclusivePeriod exclusivePeriod;

    @Getter
    private DailyRewardNotifyService dailyRewardNotifyService;

    private final LinkageRewardManager linkageRewardManager
            = new LinkageRewardManager();
    private final TelegramChannelSubscriptionClient tournamentTelegramSubscriptionClient
            = TelegramChannelSubscriptionClient.createDefault();

    public BaseLobbyServer(@NonNull AbstractPlugin plugin) {
        this.plugin = plugin;
        this.exclusivePeriod = ExclusivePeriod.getCurrentExclusivePeriod();
    }

    private static void updateLobbyStatuses() {
        for (Gamer gamer : Gamer.getGamers()) {
            if (SitManager.INSTANCE.isSitting(gamer)) {
                continue;
            }

            Language language = gamer.getLanguage();

            List<String> statuses = new ArrayList<>(3);

            if (gamer.getFakeName() != null) {
                statuses.add(language.getMessage("lobby_nick_active_bar"));
            }

            if (gamer.isVanished()) {
                statuses.add(language.getMessage("lobby_vanish_active_bar"));
            }

            Booster booster = gamer.getBooster();
            if (booster != null) {
                Duration.FormatParams params = new Duration.FormatParams();
                params.sourceUnit = TimeUnit.MILLISECONDS;
                params.min = DurationField.SEC;
                params.max = DurationField.HOUR;
                params.locale = language == Language.RUSSIAN ?
                        DurationFormatter.RU_SHORT :
                        DurationFormatter.EN_SHORT;

                double multiplier = booster.getBoosterType().getMultiplier() + 1;
                long duration = java.time.Duration
                        .between(LocalDateTime.now(), booster.getExpire()).toMillis();

                statuses.add(language.getMessage("lobby_booster_active_bar",
                        "%multiplier%", multiplier,
                        "%expires%", Duration.format(duration, params)
                ));
            }

            if (!statuses.isEmpty()) {
                gamer.sendActionBar(String.join(", ", statuses));
            }
        }
    }


    public final void loadLobby() {
        val configDirectory = plugin.getDataFolder()
                .toPath();

        File file = configDirectory
                .resolve("lobby.yml").toFile();

        Logging.IMP.info("[Exclusive] Current period: {}", exclusivePeriod);
        // Если сейчас идет эксклюзивный период, пробуем его загрузить
        if (exclusivePeriod != null) {
            Logging.IMP.info("[Exclusive] Try find exclusive config!");
            File exclusiveFile = new File(plugin.getDataFolder(),
                    "exclusive/" + exclusivePeriod.getExclusiveIdent() + "_lobby.yml");

            if (exclusiveFile.exists()) {
                Logging.IMP.info("[Exclusive] Use {} config!", exclusivePeriod.getExclusiveIdent());
                file = exclusiveFile;
            }
        }

        plugin.getServer().getLogger().info("Load configuration from " + file.getAbsolutePath());

        if (!file.exists()) {
            throw new IllegalStateException("Lobby configuration not found!");
        }

        configuration = YamlConfiguration.loadConfiguration(file);

        RawLocation spawnRawLocation = LocationUtil.stringToRawLocation(configuration.getString("Spawn"));

        if (Bukkit.getWorld(spawnRawLocation.world()) == null) {
            val propertyMap = SlimeWorldUtil.getDefaultProperty();

            propertyMap.setInt(SlimeProperties.SPAWN_X, (int) spawnRawLocation.x());
            propertyMap.setInt(SlimeProperties.SPAWN_Y, (int) spawnRawLocation.y());
            propertyMap.setInt(SlimeProperties.SPAWN_Z, (int) spawnRawLocation.z());

            SlimeWorldUtil.loadSLimeWorld(spawnRawLocation.world(), propertyMap);
        }

        spawnLocation = LocationUtil.stringToLocation(configuration.getString("Spawn"));

        World lobbyWorld = spawnLocation.getWorld();

        lobbyWorld.setDifficulty(Difficulty.PEACEFUL);
        lobbyWorld.setGameRuleValue("doEntityDrops", "false");
        lobbyWorld.setGameRuleValue("randomTickSpeed", "0");

        String rawDailyRewardNpcLocation = configuration.getString("DailyReward");
        String rawVoteNpcLocation = configuration.getString("VoteNpc");

        new LobbyProtectionListener(plugin);
        new LobbyPlayerListener(this);
//        new LobbySheepListener(this);
        new LobbyHiderListener(plugin);
        new LobbyPortalListener(this);

        BukkitCommandManager commandManager = plugin.getCommandManager();

        commandManager.addCommand(GuideCommand.class);
        commandManager.addCommand(RewardCommand.class);
        commandManager.addCommand(KaboomCommand.class);
        commandManager.addCommand(new ProfileCommand());
        commandManager.addCommand(SpawnCommand.create(this));
        commandManager.addCommand(OpenVanityMenuCommand.class);
        commandManager.addCommand(FlyCommand.class);
        commandManager.addCommand(GameHistoryCommand.class);
        commandManager.addCommand(TournamentCommand.class);
        commandManager.addCommand(new RefreshLobbyLookupsCommand(FalconAPI.getInstance().getModeNpcService()));

        if (Project.CURRENT == Project.FAKEPIXEL) {
            commandManager.addCommand(GiftCommand.class);
        }

//        ProtocolLibrary.getProtocolManager()
//                .addPacketListener(new BiomeChangePacketListener(plugin));

        GuildManager.INSTANCE.register(this);

        WarModeManager.INSTANCE.register(this);

        boolean rainEnable = configuration.getBoolean("Rain");

        String rawBestPlayerLocation = configuration.getString("BestPlayer");

        List<String> mysteryBoxes = configuration.getStringList("MysteryBoxes");
        List<String> waitNpcList = configuration.getStringList("Wait");

        ConfigurationSection lobbyNpcSection = configuration.getConfigurationSection("NPC");

        ConfigurationSection bannerSection = configuration.getConfigurationSection("Banner");
        ConfigurationSection lobbyTimeSection = configuration.getConfigurationSection("Time");
        ConfigurationSection jumpPadSection = configuration.getConfigurationSection("JumpPad");
        ConfigurationSection rotatingHeadSection = configuration.getConfigurationSection("RotatingHead");
        ConfigurationSection hologramsSection = configuration.getConfigurationSection("Holograms");
        ConfigurationSection parkourSection = configuration.getConfigurationSection("Parkour");
        ConfigurationSection shopSection = configuration.getConfigurationSection("Shop");
        ConfigurationSection informationNpcSection = configuration.getConfigurationSection("InformationNpc");
        ConfigurationSection donateHologramSection = configuration.getConfigurationSection("DonateHologram");
        ConfigurationSection actionNpcSection = configuration.getConfigurationSection("ActionNpc");
        ConfigurationSection shopKeeperNpcSection = configuration.getConfigurationSection("ShopKeeperNpc");
        ConfigurationSection linkageRewardNpcSection = configuration.getConfigurationSection("LinkageNpc");
        ConfigurationSection tournamentTopSection = configuration.getConfigurationSection("TournamentTop");

        if (linkageRewardNpcSection != null) {
            linkageRewardManager.registerManager(linkageRewardNpcSection);
        }

        if (rawDailyRewardNpcLocation != null) {
            val dailyRewardLocation = LocationUtil.stringToLocation(rawDailyRewardNpcLocation);
            dailyRewardNotifyService = new DailyRewardNotifyService(dailyRewardLocation);

            new DailyRewardListener(dailyRewardNotifyService);

            DailyRewardNpc dailyRewardNPC = new DailyRewardNpc(LocationUtil.stringToLocation(rawDailyRewardNpcLocation));
            dailyRewardNPC.create();
        } else {
            Logging.IMP.warn("[DAILY NPC] Failed spawn, because npc location is null!");
        }

        if (rawVoteNpcLocation != null) {
            new LobbyVoteListener(plugin);

            VoteNpc voteNPC = new VoteNpc(LocationUtil.stringToLocation(rawVoteNpcLocation));
            voteNPC.create();
        } else {
            Logging.IMP.warn("[VOTE NPC] Failed spawn, because npc location is null!");
        }

        if (lobbyNpcSection != null) {
            for (String gameModeNpc : lobbyNpcSection.getKeys(false)) {
                ServerMode mode = ModeUtil.getServerMode(gameModeNpc);

                if (mode instanceof Unknown) {
                    plugin.log.warn("Unknown server mode: {}", gameModeNpc);
                    continue;
                }

                ConfigurationSection npcSection = configuration.getConfigurationSection("NPC.".concat(gameModeNpc));

                String value = npcSection.getString("Skin.Value");
                String signature = npcSection.getString("Skin.Signature");

                Location location = LocationUtil.stringToLocation(npcSection.getString("Location"));

                Skin skin = new Skin(mode.toString(), "", value, signature);

                ModeNpc npc = FalconAPI.getInstance()
                        .getModeNpcService()
                        .spawn(
                                location,
                                skin,
                                mode
                        );

                if (npcSection.contains("Integration.Laby")) {
                    int labyEmote = npcSection.getInt("Integration.Laby.Emote");
                    npc.addLabyEmote(labyEmote);
                }
            }

            FalconAPI.getInstance().getModeNpcService().triggerOnlineUpdate();
        }

        if (shopSection != null) {
            ConfigurationSection storeNpcSection = shopSection.getConfigurationSection("StoreNpc");
            if (storeNpcSection != null) {
                new ShopRankNpc().create(storeNpcSection);
            }

            ConfigurationSection exchangerNpcSection = shopSection.getConfigurationSection("ExchangerNpc");
            if (exchangerNpcSection != null) {
                log.warn("Exchanger NPC wont be loaded, because it's disabled");
//                new ShopExchangerNpc().create(exchangerNpcSection);
            }

        }

        if (rawBestPlayerLocation != null) {
            BestPlayerManager bestPlayerManager = new BestPlayerManager(plugin,
                    LocationUtil.stringToLocation(rawBestPlayerLocation));
            bestPlayerManager.load();
        }

        if (bannerSection != null) {
            bannerSection.getKeys(false).forEach(k -> {
                val section = bannerSection.getConfigurationSection(k);
                File imageFile = getPlugin().getDataFolder().toPath().resolve(section.getString("ImagePath")).toFile();
                Logging.IMP.info("Loading for {}", section.getName());
                if (imageFile.exists()) {
                    Logging.IMP.info("Loading image from: {}", imageFile.getPath());
                    //локация верхнего левого угла картинки
                    Location location = LocationUtil.stringToLocation(section.getString("Location"));

                    //в эту сторону будут смотреть рамки
                    double yaw = section.getDouble("Yaw");

                    try {
                        BufferedImage image = ImageIO.read(imageFile);

                        new ImageMap(location, LocationUtil.getFacingDirection(yaw, true), image);
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                }
            });
        }

        switch (lobbyTimeSection.getString("Mode")) {
            case "SYNCHRONIZE" -> {
                String timeZone = lobbyTimeSection.getString("Zone");
                WorldRealTimeSync.sync(lobbyWorld, TimeZone.getTimeZone(timeZone).toZoneId());
            }
            case "SET" -> {
                String timeState = lobbyTimeSection.getString("State");
                WorldSetTime.setAndFreezeTime(lobbyWorld, WorldTimeState.valueOf(timeState));
            }
        }

        if (rainEnable) {
            lobbyWorld.setWeatherDuration(Integer.MAX_VALUE);
            lobbyWorld.setStorm(true);
            lobbyWorld.setThundering(false);
        }

        if (jumpPadSection != null) {
            double jumpPadMultiply = jumpPadSection.getDouble("Multiply");
            double jumpPadY = jumpPadSection.getDouble("Y");

            Material jumpPadMaterial = Material.getMaterial(jumpPadSection.getString("Material"));

            new LobbyJumpPadListener(jumpPadMultiply, jumpPadY, jumpPadMaterial);
        }

        if (rotatingHeadSection != null) {
            Set<String> heads = rotatingHeadSection.getKeys(false);

            for (String headName : heads) {
                ConfigurationSection headSettings = rotatingHeadSection.getConfigurationSection(headName);

                Location location = LocationUtil.stringToLocation(headSettings.getString("Location"));
                String texture = headSettings.getString("Texture");
                List<String> textLines = headSettings.getStringList("Lines");
                String clickMessage = headSettings.getString("ClickMessage");
                String clickCommand = headSettings.getString("ClickCommand");
                String block = headSettings.getString("Block");

                ConfigurationSection colorSection = headSettings.getConfigurationSection("Color");

                Color color = Color.fromRGB(0, 255, 50);

                if (colorSection != null) {
                    color = Color.fromRGB(colorSection.getInt("Red"), colorSection.getInt("Green"), colorSection.getInt("Blue"));
                }

                HypixelHead head = texture != null ?
                        new HypixelHead(location, ItemUtil.getBuilder(Material.SKULL_ITEM).setSkullValue(texture).setDurability(3).build(), color)
                        : new HypixelHead(location, ItemUtil.getBuilder(Material.getMaterial(block)).build(), color);


                if (clickCommand != null) {
                    head.setClickAction(player -> player.chat(clickCommand));
                }

                if (clickMessage != null) {
                    head.setClickAction(player -> Gamer.getGamer(player).sendLocalizedMessage(clickMessage));
                }

                for (String line : textLines) {
                    head.addTextLine(Lang.of(line).compile());
                }

                head.register();
                Logging.IMP.info("[Rotating Head] Success spawn head {}...", headName);
            }
        }

        if (hologramsSection != null) {
            Set<String> holograms = hologramsSection.getKeys(false);

            for (String hologramName : holograms) {
                ConfigurationSection hologramSettings = hologramsSection.getConfigurationSection(hologramName);
                Location location = LocationUtil.stringToLocation(hologramSettings.getString("Location"));
                List<String> lines = hologramSettings.getStringList("Lines");

                Hologram hologram = new Hologram(location);

                for (String line : lines) {
                    if (line.contains("Material:")) {
                        Material material = Material.getMaterial(line.replace("Material:", ""));
                        hologram.addItemLine(new ItemStack(material));
                        continue;
                    }
                    hologram.addTextLine(line);
                }

                hologram.setPublicStatus(true);
                Logging.IMP.info("[Hologram] Success spawn hologram {}...", hologramName);
            }
        }

        for (
                String mysteryBoxRawLocation : mysteryBoxes) {
            MysteryBox mysteryBox = MysteryBoxStorage.registerMysteryBox(
                    LocationUtil.stringToLocation(mysteryBoxRawLocation)
            );
            mysteryBox.setMysteryBoxCraft(true);
        }

        if (parkourSection != null) {
            lobbyParkourList = new ArrayList<>();

            for (String key : parkourSection.getKeys(false)) {
                ConfigurationSection singleSection = parkourSection.getConfigurationSection(key);

                lobbyParkourList.add(new LobbyParkour(plugin, singleSection));
            }

            plugin.getCommandManager().addCommand(new ResetParkourTImeCommand(this));
            new ParkourListener(plugin);
        }

        if (informationNpcSection != null) {
            Set<String> infoKeys = informationNpcSection.getKeys(false);

            for (String key : infoKeys) {
                ConfigurationSection npcInfo = informationNpcSection.getConfigurationSection(key);
                ConfigurationSection skinSection = npcInfo.getConfigurationSection("Skin");

                String message = npcInfo.getString("Message");
                List<String> hologram = npcInfo.getStringList("Lines");
                Location location = LocationUtil.stringToLocation(npcInfo.getString("Location"));

                Skin skin = new Skin("", "",
                        skinSection.getString("Value"),
                        skinSection.getString("Signature"));

                InformationNpc informationNpc = new InformationNpc(location,
                        hologram, message, skin);
                informationNpc.create();

            }
        }

        if (donateHologramSection != null) {
            new DonateHologram(LocationUtil.stringToLocation(donateHologramSection.getString("Location")));
        }

        if (actionNpcSection != null) {
            for (String key : actionNpcSection.getKeys(false)) {
                ConfigurationSection npcInfo = actionNpcSection.getConfigurationSection(key);
                ActionNpc.of(npcInfo).spawn();
            }
        }

        if (shopKeeperNpcSection != null) {
            ShopKeeperNpc.of(shopKeeperNpcSection).spawn();
        }

        plugin.getServer().
                getScheduler().runTaskTimerAsynchronously(
                        plugin,
                        BaseLobbyServer::updateLobbyStatuses,
                        30L,
                        30L
                );

        if (configuration.contains("Boxes") && FalconAPI.BOXES_ENABLED) {
            FalconAPI.getInstance().getAnimationService().init(configuration);

            val boxNpcSection = configuration.getConfigurationSection("Boxes.BoxNpc");

            if (boxNpcSection != null) {
                val npc = new BoxContrabandNpc();
                npc.create(boxNpcSection);
            }
        }

        try {
            Skin skin = new Skin("waiter", "",
                    "ewogICJ0aW1lc3RhbXAiIDogMTYzNjEzNTYwNzkwOCwKICAicHJvZmlsZUlkIiA6ICJkZTE0MGFmM2NmMjM0ZmM0OTJiZTE3M2Y2NjA3MzViYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJTUlRlYW0iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2NkOGYwMjUwMTc4NWQxNmIwOTNmNGQ2NTk2OThhNzlmY2Q1MGEwMDk0ZjUzMzQ5ZDQ4ZDY2MzJmYzZlMTMyZCIKICAgIH0KICB9Cn0=",
                    "RPxcif+B1vYlZgxQKcZaMdJMlDfOLuHMsG+bHLEUulkOVMZIRXcEIp/YGtgPHcEjZ8tqVDPE6E+mFqnpY03JPsz1aOrWvr3D4dx7ZEMtQE/drjD6c5tFdkPt8xJi4BP79xQsQJtSKZsCn0xRUfDKugmn0i0Waxaw5UFvGDkQbhYWC9lGwNcblIYqube/L/XAtnOiDKQ8zT0hdqsuJ5Ny5AveW3hxBv221/gmwrrPwdgYX5egApxirooaoB1vVULCfd7opBpSs7IGyp4jM5PlrU8TGUMDX7/oHh4GRG9f3NG2ccbftcg05eXxok7QVBHXMnP5o3r0V71l0tvj5VyqN/uiJDWqcmaLsCt2Qeu/3hMclTxGEm3IhWWXFWcWfkATfZ6hg+Kuk7bLBNcTExwj+veu5MIm8vTLp2MXkYCZ3zv6RzLolSyM7jGG3wAczGpY01HpaIMgkOA1SUMlmu1+pRJkTHDXPTC3XGyNaY4WzgkbBgvPOD0Z1052dtG9enbRLzhCpGNxV5N1nMiod6objHdXL4MUtiuT29ANcyHm7WHSm/f5cJ8qtrF9e3VYOnXECBGBym6DiTHY1V4nSnkCtXv8rQyrpo5XJdEzCDMyLkvxqbUj9+YTz9MkAqEngqZkpCzukXbEn3pIveXMD+oMV6ncRg27qrDPKphHuMFk5TM=");
            for (String s : waitNpcList) {
                new WaitNpc().createEntities(skin, LocationUtil.stringToLocation(s));
            }
        } catch (Exception exception) {
            Logging.IMP.info("[WaitersNpc] Cant load Wait NPCs locations!");

        }

        if (tournamentTopSection != null) {
            val client = FalconAPI.getInstance().getTournamentClient();

            new TournamentNpcTask(
                    plugin,
                    client,
                    FalconAPI.getInstance().getTournamentService(),
                    tournamentTelegramSubscriptionClient,
                    tournamentTopSection
            ).startChecking();

            getPlugin().getCommandManager().addCommand(new TournamentCreateCommand(client));
            getPlugin().getCommandManager().addCommand(new TournamentResetRatingCommand(client));
        }

        onLobbyLoad();

        if (exclusivePeriod != null) {
            onExclusivePeriod();
        }

    }

    public void onExclusivePeriod() {

    }

    public void onLobbyLoad() {

    }


    public void onPlayerJoin(@NonNull Gamer gamer) {
        sendBoard(gamer);

        getBossbar(gamer.getLanguage()).send(gamer);

        gamer.getPlayer()
                .addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
        gamer.getPlayer()
                .addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
    }

    public void onPlayerQuit(@NonNull Gamer gamer) {

    }

    public void onPlayerLanguageChanged(@NonNull Gamer gamer, @NonNull Language oldLanguage) {
        sendBoard(gamer);

        getBossbar(oldLanguage).hide(gamer);
        getBossbar(gamer.getLanguage()).send(gamer);
    }

    public void sendBoard(@NonNull Gamer gamer) {
    }

    public LobbyBossbar getBossbar(@NonNull Language language) {
        return bossbarMap.computeIfAbsent(language, k -> new LobbyBossbar(plugin, language).start());
    }

}
