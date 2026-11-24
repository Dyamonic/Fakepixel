package club.abelix.api.base.parkour;

import club.abelix.api.base.camera.state.PlayerState;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ParkourState {

    private final LobbyParkour parkour;

    private PlayerState playerState;

    private long start;
    private long startCheckpoint;

    private int checkpoint;

    public ParkourCheckpoint getParkourCheckpoint() {
        return parkour.getCheckpoint(checkpoint);
    }

    public void runCheckpointTimer(int checkpoint) {
        this.startCheckpoint = System.currentTimeMillis();
        this.checkpoint = checkpoint;
    }

    public void runStartTimer() {
        long now = System.currentTimeMillis();

        this.checkpoint = 0;
        this.start = now;
        this.startCheckpoint = now;
    }

}
