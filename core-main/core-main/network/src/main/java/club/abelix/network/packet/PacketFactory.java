package club.abelix.network.packet;

/**
 * @author Unidentified Person
 */
public interface PacketFactory {

    Packet<?> newInstance();

}
