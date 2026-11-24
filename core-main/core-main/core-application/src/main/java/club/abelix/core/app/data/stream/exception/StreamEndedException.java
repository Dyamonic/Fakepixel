package club.abelix.core.app.data.stream.exception;

public class StreamEndedException extends StreamException {

    public StreamEndedException() {
        super("stream_ended");
    }
}
