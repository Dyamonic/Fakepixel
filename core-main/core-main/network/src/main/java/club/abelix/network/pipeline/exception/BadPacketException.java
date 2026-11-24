package club.abelix.network.pipeline.exception;

/**
 * @author WhileInside
 */
public class BadPacketException extends InstantException {

    public BadPacketException(int packetId) {
        super("Bad Packet: [ID: " + packetId + "]");
    }
}
