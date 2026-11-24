package club.abelix.commons.data.vote;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@Getter
public class PlayerVoteData {

    private final Map<VotePlatform, List<PlayerVote>> playerVoteMap = new HashMap<>();

    public void addVote(@NonNull VotePlatform platform, LocalDateTime timestamp) {
        List<PlayerVote> votes = playerVoteMap.computeIfAbsent(platform, k -> new ArrayList<>());
        votes.add(new PlayerVote(platform, timestamp));
    }

    public int getVoteCount(@NonNull VotePlatform platform) {
        List<PlayerVote> votes = playerVoteMap.get(platform);

        return votes == null ? 0 : votes.size();
    }

    public boolean isVotedOnAllPlatforms() {
        for (VotePlatform platform : VoteSqlHandler.INSTANCE.getActiveVotePlatforms()) {
            if (!isVotedToday(platform)) {
                return false;
            }
        }

        return true;
    }

    public boolean isVotedToday(@NonNull VotePlatform platform) {
        long lastVoteTime = getLastVoteTime(platform);

        return lastVoteTime != 0 && lastVoteTime < VoteSqlHandler.VOTE_INTERVAL;
    }

    public long getLastVoteTime(@NonNull VotePlatform platform) {
        List<PlayerVote> votes = playerVoteMap.get(platform);

        if (votes == null || votes.isEmpty()) {
            return 0;
        }

        LocalDateTime voteTime = votes.get(votes.size() - 1).timestamp;
        ZonedDateTime zonedVoteTime = voteTime.atZone(ZoneId.systemDefault());

        return System.currentTimeMillis() - zonedVoteTime.toInstant().toEpochMilli();
    }

    @RequiredArgsConstructor
    @Getter
    public static class PlayerVote {

        private final VotePlatform platform;
        private final LocalDateTime timestamp;

    }

}
