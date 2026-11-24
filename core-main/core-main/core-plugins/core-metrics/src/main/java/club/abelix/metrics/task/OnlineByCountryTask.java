package club.abelix.metrics.task;

import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.commons.metric.base.MetricGaugeTask;
import lombok.val;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>OnlineByCountryTask creation on 20.02.2024 at 01:34</p>
 */

public class OnlineByCountryTask extends MetricGaugeTask {
    public OnlineByCountryTask() {
        super("online_distribute_by_country",
                "Players by country", 10,
                TimeUnit.SECONDS,
                "country");
    }

    @Override
    public void update() {
        Map<String, Integer> onlineByCountry = new HashMap<>();
        val core = CoreApplication.getInstance();

        for (val gamer : Gamer.getGamers()) {
            val country = core.getGeoLocationManager().lookup(gamer.getAddress())
                    .getCountry();

            if (country == null) {
                continue;
            }

            val iso = country.getIsoCode();
            onlineByCountry.put(iso, onlineByCountry.getOrDefault(iso, 0) + 1);
        }

        onlineByCountry.forEach(
                (country, online) ->
                        getCollector().labels(country).set(online));
    }

    @Override
    protected Double getValue() {
        return null;
    }
}
