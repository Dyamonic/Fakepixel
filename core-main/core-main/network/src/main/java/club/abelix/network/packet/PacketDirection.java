package club.abelix.network.packet;

public enum PacketDirection {

    TO_SERVER {
        @Override
        public PacketMapper getMapper(PacketRegistry protocol) {
            return protocol.TO_SERVER;
        }
    },
    TO_CLIENT {
        @Override
        public PacketMapper getMapper(PacketRegistry protocol) {
            return protocol.TO_CLIENT;
        }
    };

    public abstract PacketMapper getMapper(PacketRegistry protocol);
}
