package club.abelix.network.exception;

import club.abelix.network.channel.AbstractChannel;

public class BindException extends Exception {

    public BindException(AbstractChannel channel, Throwable cause) {
        super("Unable to bind server [" + channel.getConnectAddress() + "]", cause);
    }

}
