package club.abelix.commons.data.nickname;

import club.abelix.commons.data.rank.Rank;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Getter
public class FakePlayerNickname {

    private final int playerId;

    private final FakeNickname nickname;

    private final Rank group;

    public String formatDisplayName() {
        return group.getPrefix() + nickname.getName();
    }

    public boolean equals(@NonNull String nick) {
        return nickname.getName().equalsIgnoreCase(nick);
    }

    @RequiredArgsConstructor
    @Getter
    public static class HistoryEntry {

        private final FakePlayerNickname nickname;

        private final LocalDateTime issueDate, endDate;

        public HistoryEntry(@NonNull FakePlayerNickname nickname, @NonNull LocalDateTime issueDate) {
            this(nickname, issueDate, null);
        }

        public boolean isActive() {
            return endDate == null;
        }

    }

}
