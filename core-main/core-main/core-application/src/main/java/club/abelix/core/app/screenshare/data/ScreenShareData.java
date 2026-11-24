package club.abelix.core.app.screenshare.data;

import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(exclude = "startTime")
@RequiredArgsConstructor
public final class ScreenShareData {

    final Gamer target;
    final Gamer admin;
    final LocalDateTime startTime = LocalDateTime.now();

    @Setter
    AbstractServer server;

}
