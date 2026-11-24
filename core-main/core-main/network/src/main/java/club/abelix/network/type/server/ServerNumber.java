package club.abelix.network.type.server;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ServerNumber {

    private final int number;

    public boolean hasNumber() {
        return number != -1;
    }

}
