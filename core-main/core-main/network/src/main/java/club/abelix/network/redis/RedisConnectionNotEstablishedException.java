package club.abelix.network.redis;

public class RedisConnectionNotEstablishedException extends RuntimeException {
    public RedisConnectionNotEstablishedException(String message) {
        super(message);
    }

    public RedisConnectionNotEstablishedException(String message, Throwable cause) {
        super(message, cause);
    }
}
