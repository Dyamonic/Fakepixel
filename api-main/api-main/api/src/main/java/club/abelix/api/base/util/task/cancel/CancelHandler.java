package club.abelix.api.base.util.task.cancel;

import lombok.NonNull;

public interface CancelHandler {

    void handleCancel(@NonNull CancelReason reason);

    // чтобы лист не создавать, вряд ли вообще будет больше одного хандлера
    default CancelHandler with(@NonNull CancelHandler another) {
        return reason -> {
            handleCancel(reason);
            another.handleCancel(reason);
        };
    }

}
