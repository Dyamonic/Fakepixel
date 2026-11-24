package club.abelix.network.exception;

import club.abelix.network.channel.AbstractChannel;

public class ConnectException extends Exception {

    public ConnectException(AbstractChannel channel, Throwable cause) {
        super("Unable to connect to server [" + channel.getConnectAddress() + "]", cause);
    }

}
