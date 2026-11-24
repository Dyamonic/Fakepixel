package club.abelix.core.velocity.filter.data;

import club.abelix.core.velocity.StarshipVelocity;
import club.abelix.core.velocity.filter.ChatFilterManager;
import club.abelix.core.velocity.filter.violation.ChatViolationEvent;
import club.abelix.core.velocity.filter.violation.Violation;
import club.abelix.core.velocity.filter.violation.ViolationType;
import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlayerData {

    @Getter
    private final List<Message> messages = new ArrayList<>();

    @Getter
    private final List<Violation> violations = new ArrayList<>();

    public void addMessage(@NonNull ChatFilterManager chatFilterManager, @NonNull Message message) {
        this.messages.add(message);

        while (this.messages.size() > chatFilterManager.getCapacity()) {
            this.messages.remove(0);
        }
    }

    public int messageCount() {
        return this.messages.size();
    }

    public double getAvgDelay() {
        if (messageCount() < 2) {
            throw new IllegalStateException();
        }

        List<Message> list = this.messages;

        // посчитаем задержку между всеми сохранёнными сообщениями
        double avg = list.get(1).time() - list.get(0).time();

        for (int i = 1; i < list.size() - 1; i++) {
            avg = (avg + (list.get(i + 1).time() - list.get(i).time())) / 2;
        }

        // прибавим задержку с последнего сообщения до текущего момента
        avg = (avg + (System.currentTimeMillis() - list.get(list.size() - 1).time())) / 2;

        return avg;
    }

    public long getLastTime() {
        return this.messages.isEmpty() ? 0 : this.messages.get(this.messages.size() - 1).time();
    }

    public void addVL(@NonNull Player player, @NonNull ViolationType type) {
        this.violations.add(new Violation(type));

        StarshipVelocity.getInstance().getProxy().getEventManager()
                .fireAndForget(new ChatViolationEvent(player, this, type));
    }

    public double getTotalViolations() {
        return getTotalViolations(TimeUnit.MINUTES.toMillis(10));
    }

    public double getTotalViolations(long threshold) {
        double vl = 0;

        for (Violation violation : this.violations) {
            if (System.currentTimeMillis() - violation.getTimestamp() < threshold) {
                vl += violation.getType().levelDelta;
            }
        }

        return vl;
    }

}
