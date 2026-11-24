package club.abelix.commons.data.session;

import java.net.InetAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class Session {

    private final int sessionId;
    private final int playerId;

    private final LocalDateTime joinedTimestamp;
    private final LocalDateTime leftTimestamp;

    private final String domain;
    private final InetAddress address;

    private final List<SessionServer> servers;

    public Session(int sessionId, int playerId, LocalDateTime joinedTimestamp, LocalDateTime leftTimestamp,
                   String domain, InetAddress address, List<SessionServer> servers) {
        this.sessionId = sessionId;
        this.playerId = playerId;
        this.joinedTimestamp = joinedTimestamp;
        this.leftTimestamp = leftTimestamp;
        this.domain = domain;
        this.address = address;
        this.servers = servers;
    }

    public int getSessionId() {
        return sessionId;
    }

    public int getPlayerId() {
        return playerId;
    }

    public long getElapsedTime() {
        if (leftTimestamp == null) {
            return 0;
        }

        return Duration.between(joinedTimestamp, leftTimestamp).getSeconds();
    }

    public LocalDateTime getJoinedTimestamp() {
        return joinedTimestamp;
    }

    public LocalDateTime getLeftTimestamp() {
        return leftTimestamp;
    }

    public String getDomain() {
        return domain;
    }

    public InetAddress getAddress() {
        return address;
    }

    public List<SessionServer> getServers() {
        return servers;
    }

    public boolean isEnded() {
        return leftTimestamp != null;
    }

    public SessionServer getLastServer() {
        return servers.isEmpty() ? null : servers.get(0);
    }

}
