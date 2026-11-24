package club.abelix.core.app.data.stream.exception;

public class StreamNotFoundException extends StreamException {

    public StreamNotFoundException() {
        super("stream_not_found");
    }
}
