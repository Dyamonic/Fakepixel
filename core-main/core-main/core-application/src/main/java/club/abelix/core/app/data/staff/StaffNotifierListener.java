package club.abelix.core.app.data.staff;

import club.abelix.commons.data.auth.AuthSqlHandler;
import club.abelix.commons.data.auth.BaseAuthPlayerData;
import club.abelix.commons.data.session.Session;
import club.abelix.commons.data.session.SessionSqlHandler;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.array.ObjectLinkedBuf;
import club.abelix.commons.util.time.Duration;
import club.abelix.commons.util.time.TimeUtil;
import club.abelix.core.app.Async;
import club.abelix.core.app.api.event.EventPriority;
import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.punishment.PunishmentManager;
import club.abelix.core.app.event.gamer.GamerJoinEvent;
import club.abelix.core.app.event.gamer.GamerLoadEvent;
import club.abelix.core.app.event.gamer.PlayerQuitEvent;
import lombok.RequiredArgsConstructor;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import w.geo.api.Country;
import w.geo.api.GeoLocationManager;

import java.net.InetAddress;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class StaffNotifierListener implements Listener {

    private final GeoLocationManager geoLocationManager;

    @Subscribe(priority = EventPriority.HIGHEST)
    public void onLoad(GamerLoadEvent event) {
        Gamer gamer = event.getGamer();

        if (gamer.hasPermission("core.staffnotify.changelocation")) {
            gamer.loadLastSession();
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onJoin(GamerJoinEvent event) {
        Gamer gamer = event.getGamer();

        Session lastSession = gamer.getLastSession();

        InetAddress oldAddress = lastSession != null ? lastSession.getAddress() : null;
        InetAddress newAddress = gamer.getAddress();

        Async.run(() -> {
            List<String> banned = new ArrayList<>();

            if (oldAddress != null) {
                banned.addAll(checkAlts(oldAddress.getHostAddress()));
            }

            banned.addAll(checkAlts(newAddress.getHostAddress()));

            BaseAuthPlayerData authData = new BaseAuthPlayerData(gamer.getName());
            authData.load();

            String regIp = authData.getRegIp();
            banned.addAll(checkAlts(regIp));

            //remove duplicates from list
            banned = banned.stream().distinct().collect(Collectors.toList());

            if (banned.size() <= 15) {
                StringBuilder sb = new StringBuilder();

                for (String account : banned) {
                    sb.append(account).append("§7, §c");
                }

                sb.delete(sb.length() - 4, sb.length());

                ServerManager.IMP.broadcastStaff("command.staffchat", "core_staff_banned_alt_notify",
                        "%name%", gamer.getDisplayName());
                ServerManager.IMP.broadcastStaff("command.staffchat", "core_staff_banned_alt_list",
                        "%names%", sb.toString());
            } else {
                ServerManager.IMP.broadcastStaff("command.staffchat", "core_staff_banned_alt_notify",
                        "%name%", gamer.getDisplayName());
                ServerManager.IMP.broadcastStaff("command.staffchat", "core_staff_banned_alt_list",
                        "%names%", "§c" + banned.size() + " §7(>15)");
            }
        });

        if (gamer.hasPermission("core.staffnotify.join")) {
            ServerManager.IMP.broadcastStaff("command.staffchat", "core_staff_join_notify",
                    "%name%", gamer.getDisplayName());

            if (newAddress.equals(oldAddress)) {
                return;
            }

            if (oldAddress != null) {
                Country oldCountry = geoLocationManager.lookup(oldAddress).getCountry();
                Country newCountry = geoLocationManager.lookup(newAddress).getCountry();

                if (oldCountry == null && newCountry != null) {
                    ServerManager.IMP.broadcastStaff("command.staffchat", "core_staff_join_new_country",
                            "%name%", gamer.getDisplayName(),
                            "%country%", newCountry.getName());
                    return;
                }

                if (oldCountry != null && newCountry == null) {
                    ServerManager.IMP.broadcastStaff("command.staffchat", "core_staff_join_unknown_new_country",
                            "%name%", gamer.getDisplayName(),
                            "%old_country%", oldCountry.getName());
                    return;
                }

                if (oldCountry == null) {
                    ServerManager.IMP.broadcastStaff("command.staffchat", "core_staff_join_unknown_country",
                            "%name%", gamer.getDisplayName());
                    return;
                }


                if (oldCountry.getIsoCode().equalsIgnoreCase(newCountry.getIsoCode())) {
                    return;
                }


                ServerManager.IMP.broadcastStaff("command.staffchat", "core_staff_join_change_country",
                        "%name%", gamer.getDisplayName(),
                        "%old_country%", oldCountry.getName(),
                        "%new_country%", newCountry.getName());
            }
        }
    }

    private List<String> checkAlts(String address) {
        List<String> banned = new ArrayList<>();

        List<String> accountsOnIP = AuthSqlHandler.INSTANCE.findAccounts(address)
                .join();


        if (accountsOnIP.size() > 0) {
            for (String account : accountsOnIP) {
                Gamer gamer1 = Gamer.getOfflineGamer(account, Gamer::loadLastSession);
                if (gamer1 != null && PunishmentManager.INSTANCE.getActiveBan(account, gamer1.getLastSession().getAddress()) != null) {
                    banned.add(account);
                }
            }
        }

        return banned;
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onQuit(PlayerQuitEvent event) {
        Gamer gamer = event.getGamer();

        if (gamer.isHelper()) {
            LocalDate today = LocalDate.now();
            LocalDateTime todayTime = today.atTime(LocalTime.MIDNIGHT);

            SessionSqlHandler.INSTANCE.loadSessions(gamer.getPlayerId(), todayTime).whenComplete((result, cause) -> {
                long totalTime = result.stream()
                        .mapToLong(Session::getElapsedTime)
                        .sum() + gamer.getPlayingSeconds();

                Function<Language, ObjectLinkedBuf> message = language -> {
                    ObjectLinkedBuf buf = new ObjectLinkedBuf();

                    ObjectLinkedBuf.Writer writer = buf.writer();
                    writer.writeNext("%name%", gamer.getDisplayName(), "%time_spent%");

                    if (totalTime > 0) {
                        writer.writeNext(language.getMessage(
                                "core_staff_leave_time_played",
                                "%date%", TimeUtil.getDateString(language, today),
                                "%time%", Duration.durationWordsSec(language, totalTime)
                        ));
                    } else {
                        writer.writeNext(language.getMessage("core_staff_leave_didnt_played"));
                    }

                    return buf;
                };


                ServerManager.IMP.broadcastStaff("command.staffchat", "core_staff_leave_notify", message);
            });
        }
    }


}
