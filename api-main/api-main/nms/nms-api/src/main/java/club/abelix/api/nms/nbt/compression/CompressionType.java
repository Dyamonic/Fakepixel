package club.abelix.api.nms.nbt.compression;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public enum CompressionType {

    NONE {
        @Override
        public InputStream create(InputStream is) {
            return new BufferedInputStream(is);
        }

        @Override
        public OutputStream create(OutputStream os) {
            return new BufferedOutputStream(os);
        }
    },
    GZ {
        @Override
        public InputStream create(InputStream is) throws IOException {
            return new BufferedInputStream(new GZIPInputStream(is));
        }

        @Override
        public OutputStream create(OutputStream os) throws IOException {
            return new BufferedOutputStream(new GZIPOutputStream(os));
        }
    };

    public abstract InputStream create(InputStream is) throws IOException;

    public abstract OutputStream create(OutputStream os) throws IOException;

}
