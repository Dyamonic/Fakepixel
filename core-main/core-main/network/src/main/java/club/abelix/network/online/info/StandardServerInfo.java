package club.abelix.network.online.info;

import club.abelix.network.online.data.ServerKind;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class StandardServerInfo extends AbstractServerInfo {

    public  StandardServerInfo(
            final @NotNull String id,
            final @NotNull String serverName,
            final @NotNull String serverMode,
            final @NotNull ServerKind kind,
            final @NotNull List<@NotNull String> tags,
            final int slots,
            final int online,
            final int vanishedOnline
    ) {
        super(
                id,
                serverName,
                serverMode,
                kind,
                tags,
                slots,
                online,
                vanishedOnline
        );
    }
}
