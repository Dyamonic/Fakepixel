package club.abelix.commons.util.request;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class RequestData {

    private final RequestType type;
    private final int senderId;
    private final int receiverId;

    public RequestData reverse() {
        return new RequestData(type, receiverId, senderId);
    }
}

