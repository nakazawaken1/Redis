package jp.qpg;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Redis client
 */
public class Redis implements AutoCloseable {
    /**
     * Data type
     */
    public enum Type {
        /**
         * Single line string
         */
        STRING('+'),
        /**
         * Single line error string
         */
        ERROR('-'),
        /**
         * Number
         */
        NUMBER(':'),
        /**
         * Bulk(binary safe string)
         */
        BULK('$'),
        /**
         * Multi bulk(array)
         */
        MULTI('*');

        /**
         * map(mark: Type)
         */
        public static final Map<Character, Type> map = Stream.of(values()).collect(Collectors.toMap(t -> t.mark, t -> t));

        /**
         * Character code
         */
        public final char mark;

        /**
         * @param mark Character code
         */
        Type(char mark) {
            this.mark = mark;
        }
    }

    /**
     * Reply data
     *
     * @param <T> Data type
     */
    public static class Reply<T> {
        /**
         * Data type
         */
        public final Type type;
        /**
         * Value
         */
        public final T value;

        /**
         * @param type Data type
         * @param value Value
         */
        public Reply(Type type, T value) {
            this.type = type;
            this.value = value;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            if (type == Type.MULTI) {
                int i = 1;
                Reply<?>[] list = (Reply<?>[]) value;
                StringBuilder s = new StringBuilder();
                s.append(list.length).append(" items");
                for (Reply<?> reply : list) {
                    s.append(System.lineSeparator()).append('[').append(i).append("] ").append(reply);
                    i++;
                }
                return s.toString();
            }
            if (type == Type.BULK) {
                return (char) type.mark + new String((byte[]) value, StandardCharsets.UTF_8);
            }
            return (char) type.mark + String.valueOf(value);
        }
    }

    /**
     * Socket
     */
    protected final Socket socket;
    /**
     * Socket input
     */
    protected final InputStream in;
    /**
     * Socket output
     */
    protected final OutputStream out;

    /**
     * Default host
     */
    public static final String defaultHost = "127.0.0.1";
    /**
     * Default port
     */
    public static final int defaultPort = 6379;
    /**
     * logger
     */
    protected static final Logger logger = Logger.getLogger(Redis.class.getCanonicalName());

    /**
     * @param host Redis Host
     * @param port Redis port
     * @throws IOException I/O error
     */
    public Redis(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new BufferedInputStream(socket.getInputStream());
        out = new BufferedOutputStream(socket.getOutputStream());
    }

    /**
     * Default local connection
     * 
     * @throws IOException I/O error
     */
    public Redis() throws IOException {
        this(defaultHost, defaultPort);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close() throws Exception {
        out.close();
        in.close();
        socket.close();
    }

    /**
     * Write bytes and newline
     * 
     * @param bytes Bytes
     * @throws IOException I/O error
     */
    protected void writeln(byte... bytes) throws IOException {
        out.write(bytes);
        out.write('\r');
        out.write('\n');
    }

    /**
     * Write string
     * 
     * @param prefix + or -
     * @param value Value
     * @throws IOException I/O error
     */
    public void writeString(int prefix, String value) throws IOException {
        out.write(prefix);
        writeln(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Write integer
     * 
     * @param prefix $ or * or :
     * @param value Value
     * @throws IOException I/O error
     */
    public void writeLong(int prefix, long value) throws IOException {
        writeString(prefix, String.valueOf(value));
    }

    /**
     * Write bulk string
     * 
     * @param bytes Bytes
     * @throws IOException I/O error
     */
    public void writeBulk(byte[] bytes) throws IOException {
        writeLong('$', bytes.length);
        writeln(bytes);
    }

    /**
     * Write command
     * 
     * @param <T> Value type
     * 
     * @param texts Text
     * @return Reply
     */
    public <T> Reply<T> command(String... texts) {
        try {
            send(texts);
            return response();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Write command
     * 
     * @param texts Text
     * @throws IOException I/O error
     */
    public void send(String... texts) throws IOException {
        logger.config("[Redis] " + String.join(" ", texts));
        writeln(("*" + texts.length).getBytes(StandardCharsets.UTF_8));
        for (String text : texts) {
            writeBulk(text.getBytes(StandardCharsets.UTF_8));
        }
        out.flush();
    }

    /**
     * Write command
     * 
     * @param texts Text
     * @throws IOException I/O error
     */
    public void send(Object... texts) throws IOException {
        logger.config("[Redis] " + Stream.of(texts).map(i -> i instanceof byte[] ? "(bytes)" : i.toString()).collect(Collectors.joining(" ")));
        writeln(("*" + texts.length).getBytes(StandardCharsets.UTF_8));
        for (Object text : texts) {
            writeBulk(text instanceof byte[] ? (byte[]) text : text.toString().getBytes(StandardCharsets.UTF_8));
        }
        out.flush();
    }

    /**
     * Read character
     * 
     * @return character
     * @throws IOException I/O error
     */
    public int read() throws IOException {
        return in.read();
    }

    /**
     * Read character
     * 
     * @param expected Expected character
     * @return character
     * @throws IOException I/O error
     */
    public int read(int expected) throws IOException {
        int c = read();
        if (c != expected) {
            throw new IOException("unexpected character: " + c + " expected: " + expected);
        }
        return c;
    }

    /**
     * Read integer
     * 
     * @return Size
     * @throws IOException I/O error
     */
    public long readLong() throws IOException {
        StringBuilder s = new StringBuilder();
        for (;;) {
            int c = read();
            switch (c) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                s.append((char) c);
                break;
            case '-':
                if (s.length() <= 0) {
                    s.append((char) c);
                    break;
                }
            case '\r':
                read('\n');
                return Long.parseLong(s.toString());
            default:
                throw new IOException(String.format("Invalid character: %c(0x%<X)", c));
            }
        }
    }

    /**
     * Read integer
     * 
     * @param expectedPrefix Expected prefix character
     * @return Size
     * @throws IOException I/O error
     */
    public long readLong(int expectedPrefix) throws IOException {
        read(expectedPrefix);
        return readLong();
    }

    /**
     * Read String(not include read +/-)
     * 
     * @return String
     * @throws IOException I/O error
     */
    public String readString() throws IOException {
        StringBuilder s = new StringBuilder();
        int c;
        while ((c = read()) >= 0) {
            if (c == '\r') {
                read('\n');
                return s.toString();
            }
            s.append((char) c);
        }
        throw new IOException("Invalid data");
    }

    /**
     * Read bytes(not include read $size)
     * 
     * @param size Read size
     * @return Bytes
     * @throws IOException I/O error
     */
    public byte[] readBytes(int size) throws IOException {
        if (size < 0) {
            return null;
        }
        byte[] buffer = new byte[size];
        for (long i = 0; i < size;) {
            int n = in.read(buffer);
            if (n < 0) {
                throw new IOException(String.format("size small: %d/%d", i, size));
            }
            i += n;
        }
        read('\r');
        read('\n');
        return buffer;
    }

    /**
     * Read large bytes(not include read $size)
     * 
     * @param size Read size
     * @param out Output
     * @param buffers Buffer(auto preparing if empty)
     * @throws IOException I/O error
     */
    public void readBytes(long size, OutputStream out, byte[]... buffers) throws IOException {
        if (size < 0) {
            return;
        }
        byte[] buffer = buffers.length > 0 ? buffers[0] : new byte[1024 * 10];
        for (long i = 0; i < size;) {
            int n = in.read(buffer);
            if (n < 0) {
                throw new IOException(String.format("size small: %d/%d", i, size));
            }
            out.write(buffer, 0, n);
            i += n;
        }
        read('\r');
        read('\n');
    }

    /**
     * Read bulk string(include read $size)
     * 
     * @return Bytes
     * @throws IOException I/O error
     */
    public byte[] readBulk() throws IOException {
        return readBytes((int) readLong('$'));
    }

    /**
     * Read object(include read prefix)
     * 
     * @param <T> Value type(STRING, ERROR: String, NUMBER: Long, BULK: byte[], MULTI: Reply[])
     * 
     * @return Object
     * @throws IOException I/O error
     */
    @SuppressWarnings("unchecked")
    public <T> Reply<T> response() throws IOException {
        char c = (char) read();
        switch (c) {
        case '+':
        case '-':
            return (Reply<T>) new Reply<>(Type.map.get(c), readString());
        case ':':
            return (Reply<T>) new Reply<>(Type.map.get(c), readLong());
        case '$':
            return (Reply<T>) new Reply<>(Type.map.get(c), readBytes((int) readLong()));
        case '*':
            return (Reply<T>) new Reply<>(Type.map.get(c), IntStream.range(0, (int) readLong()).mapToObj(i -> {
                try {
                    return response();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).toArray(Reply[]::new));
        default:
            throw new IOException("Invalid data");
        }
    }

    /**
     * Example
     * 
     * @param args Not use
     * @throws Exception Error
     */
    public static void main(String[] args) throws Exception {
        PrintStream out = System.out;
        try (Redis redis = new Redis()) {
            String[] keys = { "KEYS", "*" };

            // Enumerate keys
            out.println("< " + String.join(" ", keys));
            out.println("> " + redis.command(keys));
            out.println();

            // set value
            String[] set = { "SET", "a", "テスト" };
            out.println("< " + String.join(" ", set));
            out.println("> " + redis.command(set));
            out.println();

            // set value
            String[] set2 = { "SET", "b", "あいう\nえお" };
            out.println("< " + String.join(" ", set2));
            out.println("> " + redis.command(set2));
            out.println();

            // get value
            String[] get = { "GET", "a" };
            out.println("< " + String.join(" ", get));
            out.println("> " + redis.command(get));
            out.println();

            // get value
            String[] get2 = { "GET", "b" };
            out.println("< " + String.join(" ", get2));
            out.println("> " + redis.command(get2));
            out.println();

            // Enumerate keys
            out.println("< " + String.join(" ", keys));
            out.println("> " + redis.command(keys));
            out.println();

            // delete value
            String[] del = { "DEL", "a" };
            out.println("< " + String.join(" ", del));
            out.println("> " + redis.command(del));
            out.println();

            // Enumerate keys
            out.println("< " + String.join(" ", keys));
            out.println("> " + redis.command(keys));
            out.println();

            // delete value
            String[] del2 = { "DEL", "b" };
            out.println("< " + String.join(" ", del2));
            out.println("> " + redis.command(del2));
            out.println();

            // Enumerate keys
            out.println("< " + String.join(" ", keys));
            out.println("> " + redis.command(keys));
            out.println();
        }
    }
}
