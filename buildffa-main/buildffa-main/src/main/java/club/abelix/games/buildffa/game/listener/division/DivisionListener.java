package club.abelix.games.buildffa.game.listener.division;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.division.AbstractDivision;
import club.abelix.games.buildffa.game.event.GameDeathEvent;
import lombok.val;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Optional;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class DivisionListener extends YListener implements Listener {
    private final BuildFFAGame game;
    private final Set<GameDeathEvent> processedDeaths = Collections.newSetFromMap(new WeakHashMap<>());

    public DivisionListener(BuildFFAGame game) {
        super(game.getPlugin());
        this.game = game;
    }

    @EventHandler
    public void onEntityDeath(GameDeathEvent event) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Death ratings must be applied on the server thread");
        if (!processedDeaths.add(event)) return;
        val victim = Gamer.getGamer(event.getVictim());
        val killer = Optional.ofNullable(event.getKiller())
                .map(Gamer::getGamer)
                .orElse(null);

        if (killer != null) {
            if (event.isProgressionEligible()) handleKillerKill(killer, victim, event);
        }

        handleVictimDeath(victim, killer, event);
    }

    private void handleKillerKill(Gamer killer, Gamer victim, GameDeathEvent event) {
        updateRating(killer, event.getFinalRatingAwarded());
        game.getQuestManager().handleRating(killer, event.getFinalRatingAwarded());
        updateDivisionStatus(killer);

        val language = killer.getLanguage();

        val killMessage = language.getMessage("kill_message_new",
                "%victim%", victim.getDisplayName(),
                "%amount%", String.valueOf(event.getFinalCoinsAwarded()),
                "%coins%", String.valueOf(event.getFinalCoinsAwarded()),
                "%rating%", String.valueOf(event.getFinalRatingAwarded()));

        killer.sendMessage(game.getChatMessageManager().formatKillMessage(
                killer,
                victim,
                event.getFinalCoinsAwarded(),
                event.getFinalRatingAwarded(),
                ChatColor.translateAlternateColorCodes('&', killMessage)
        ));
    }

    private void handleVictimDeath(Gamer victim, Gamer killer, GameDeathEvent event) {
        int victimRatingLost = event.getVictimRatingLost();
        if (victimRatingLost != 0) updateRating(victim, victimRatingLost);

        val language = victim.getLanguage();
        String attacker = killer == null ? language.getMessage("buildffa_air") : killer.getDisplayName();

        victim.sendMessage(ChatColor.translateAlternateColorCodes('&', language.getMessage("death_message",
                "%killer%", attacker,
                "%victim%", attacker,
                "%rating%", victimRatingLost)));

        if (event.isProgressionEligible()) updateDivisionStatus(victim);
    }

    private void updateRating(Gamer gamer, int value) {
        int currentRating = gamer.getLifetimeStat("rating");
        AbstractDivision currentDivision = AbstractDivision.getRankForRating(currentRating);

        int newRating = currentRating + value;
        AbstractDivision newDivision = AbstractDivision.getRankForRating(newRating);

        if (currentDivision != newDivision) {
            broadcastDivisionUpdate(gamer, newDivision);
        }

        gamer.addStat("rating", value);
    }

    public void awardEventRating(Gamer gamer, int rating) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Event ratings must be applied on the server thread");
        if (rating <= 0) return;
        updateRating(gamer, rating);
        game.getQuestManager().handleRating(gamer, rating);
        updateDivisionStatus(gamer);
    }

    private void updateDivisionStatus(Gamer gamer) {
        int rating = gamer.getLifetimeStat("rating");
        AbstractDivision newDivision = AbstractDivision.getRankForRating(rating);

        int currentDivisionIndex = gamer.getLifetimeStat("division");
        int newDivisionIndex = newDivision.ordinal();

        int divisionDifference = newDivisionIndex - currentDivisionIndex;
        gamer.addStat("division", divisionDifference);
    }

    private void broadcastDivisionUpdate(Gamer gamer, AbstractDivision newDivision) {
        game.broadcastLocalized(
                "division_update_message",
                "%player%", gamer.getDisplayName(),
                "%division%", newDivision.getName(gamer.getLanguage())
        );
    }
}
