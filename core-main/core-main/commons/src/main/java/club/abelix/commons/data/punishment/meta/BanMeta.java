package club.abelix.commons.data.punishment.meta;

import club.abelix.commons.data.network.NetworkSqlHandler;
import lombok.Getter;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Getter
public class BanMeta extends PunishmentMeta {

    /**
     * Баны на указанные ники (по этому ID)
     */
    private final List<String> names = new ArrayList<>();

    /**
     * Баны на указанные IP (по этому ID)
     */
    private final List<String> addresses = new ArrayList<>();

    public BanMeta(int id, @NonNull String banBy, long time, long expire, @NonNull String reason) {
        this(id, banBy, null, time, expire, reason);
    }

    public BanMeta(int id, @NonNull String adminName, String unBanBy, long issueDate, long expirationDate, @NonNull String reason) {
        super(id, adminName, unBanBy, reason, issueDate, expirationDate);
    }

    /**
     * Добавить ники для которых этот бан будет выдан
     *
     * @param names - ники игроков
     */
    public void addNames(Collection<String> names) {
        this.names.addAll(names);
    }

    /**
     * Добавить ник для которого этот бан будет выдан
     *
     * @param name - ник игрока
     */
    public void addName(@NonNull String name) {
        names.add(name);
    }

    /**
     * Добавить IP адреса для которых этот бан будет выдан
     *
     * @param addresses - IP адреса
     */
    public void addAddresses(Collection<String> addresses) {
        this.addresses.addAll(addresses);
    }

    /**
     * Добавить IP адрес для которого этот бан будет выдан
     *
     * @param address - IP адрес
     */
    public void addAddress(@NonNull String address) {
        addresses.add(address);
    }

    /**
     * Получить список заблокированных ников этим баном
     *
     * @return - заблокированные ники
     */
    public List<String> getNames() {
        return Collections.unmodifiableList(names);
    }

    public boolean containsName(@NonNull String name) {
        return names.stream().anyMatch(target -> target.equalsIgnoreCase(name));
    }

    public boolean containsAddress(@NonNull String ip) {
        return addresses.contains(ip);
    }

    /**
     * Получить список заблокированных ip этим баном
     *
     * @return - заблокированные IP адреса
     */
    public List<String> getAddresses() {
        return Collections.unmodifiableList(addresses);
    }

    public int[] getBannedIds() {
        return getNames().stream()
                .mapToInt(NetworkSqlHandler.INSTANCE::getPlayerId)
                .filter(banId -> banId != -1)
                .toArray();
    }

}