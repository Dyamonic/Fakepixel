package club.abelix.api.server.lobby.mysterybox.content.equivalent;

import club.abelix.api.base.gamer.Gamer;
import lombok.NonNull;

public interface EquivalentType {

    /**
     * Выдать игроку эквивалентную валюту в случае дубликата
     *
     * @param gamer      - игрок
     * @param equivalent - количество
     * @return true - если все норм выдано,
     * false - если необходимо просто вернуть кейс игроку
     */
    boolean giveDuplicateEquivalent(@NonNull Gamer gamer, int equivalent);

    /**
     * Отправить сообщение о дубликате предмета
     *
     * @param gamer      - игрок
     * @param equivalent - кол-во выдаваемой валюты, если предмет уже имеется
     */
    void sendDuplicateEquivalentMessage(@NonNull Gamer gamer, int equivalent);

}
