package club.abelix.api.base.box.service;

import club.abelix.api.base.box.entity.BoxDto;
import club.abelix.api.base.box.repo.PlayerBoxesRepository;
import club.abelix.api.base.box.type.BoxType;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.data.currency.GameCurrency;
import club.abelix.commons.gamer.IGamer;
import club.abelix.network.type.mode.ServerMode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Log4j2
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class BoxesService {

    BoxesStorageService storage;

    public void attemptPurchase(
            Gamer gamer,
            GameCurrency gameCurrency,
            ServerMode mode,
            BoxType boxType,
            Rarity rarity
    ) {
        attemptPurchase(gamer, gameCurrency, mode, boxType, rarity, 1);
    }

    public void attemptPurchase(
            Gamer gamer,
            GameCurrency gameCurrency,
            ServerMode mode,
            BoxType boxType,
            Rarity rarity,
            int amount
    ) {
        val optionalBox = storage.findByTypeAndRarity(boxType, rarity);

        if (optionalBox.isEmpty())
            throw new IllegalArgumentException("Box with rarity " + boxType.getName(gamer.getLanguage()) + " not found.");

        val box = optionalBox.get();
        val modeBound = mode != null && box.getMode() != null;

        if (!modeBound && gameCurrency == GameCurrency.COINS) {
            gamer.sendLocalizedMessage("you_cant_buy_box_without_mode");
            return;
        }

        int amountToPay = box.getCost(gameCurrency) * amount;

        if (gameCurrency == GameCurrency.COINS) {
            if (gamer.getCoins() < amountToPay) {
                gamer.sendLocalizedMessage("not_enough_coins");
                return;
            }

            gamer.takeCoins(mode, amountToPay);
        } else if (gameCurrency == GameCurrency.GOLD) {
            if (gamer.getGold() < amountToPay) {
                gamer.sendLocalizedMessage("not_enough_gold");
                return;
            }

            gamer.takeGold(amountToPay);
        } else {
            throw new IllegalArgumentException("Unsupported currency: " + gameCurrency);
        }

        if (amount == 1) {
            giveBox(gamer, box.getRarity(), modeBound ? mode.getName() : null, boxType.getId(), box.getType());
        } else {
            giveBoxes(gamer, box.getRarity(), modeBound ? mode.getName() : null, boxType.getId(), box.getType(), amount);
        }

        BoxAuditHook.logPurchase(gamer, box, gameCurrency, amount, amountToPay, mode);
    }

    public void giveBox(
            final Gamer gamer,
            final BoxDto boxDto
    ) {
        giveBox(
                gamer,
                boxDto.getRarity(),
                boxDto.getServerMode(),
                boxDto.getBoxTypeId(),
                null
        );
    }

    public void giveBox(
            Gamer gamer,
            Rarity rarity,
            String mode,
            int boxType,
            BoxType type
    ) {
        val dtoOptional = PlayerBoxesRepository.insertPlayerBox(
                gamer.getPlayerId(),
                rarity,
                boxType,
                mode
        );

        if (dtoOptional.isEmpty()) {
            gamer.sendLocalizedMessage("box_error_occurred");
            return;
        }

        gamer.getBoxes().add(dtoOptional.get());

        if (type != null) {
            gamer.sendLocalizedMessage("box_received",
                    "%rarity%", rarity.getName(gamer.getLanguage()),
                    "%boxType%", type.getName(gamer.getLanguage())
            );
        }

        log.info("Player {} received box with rarity {}", gamer.getName(), rarity);
    }

    public void giveBoxes(
            Gamer gamer,
            Rarity rarity,
            String mode,
            int boxType,
            BoxType type,
            int amount
    ) {
        val dtoOptional = PlayerBoxesRepository.insertPlayerBoxes(
                gamer.getPlayerId(),
                rarity,
                boxType,
                mode,
                amount
        );

        gamer.getBoxes().addAll(dtoOptional);

        if (type != null) {
            gamer.sendLocalizedMessage("boxes_received",
                    "%rarity%", rarity.getName(gamer.getLanguage()),
                    "%boxType%", type.getName(gamer.getLanguage()),
                    "%amount%", amount
            );
        }

        log.info("Player {} received {} boxes with rarity {}", gamer.getName(), amount, rarity);
    }
}
