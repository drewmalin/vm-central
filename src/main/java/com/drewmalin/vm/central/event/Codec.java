package com.drewmalin.vm.central.event;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * A general purpose codec to encode and decode some {@link T} as a byte array. Used to pass objects as messages using
 * vertx's message bus.
 */
public class Codec<T>
    implements MessageCodec<T, T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Codec.class);

    private final Class<T> cls;

    public Codec(final Class<T> cls) {
        super();
        this.cls = cls;
    }

    @Override
    public void encodeToWire(final Buffer buffer, final T object) {
        try (
            final var bos = new ByteArrayOutputStream();
            final var out = new ObjectOutputStream(bos)) {

            // Write the object to the buffer
            out.writeObject(object);
            out.flush();

            // Write the buffer to the stream
            final byte[] objectBytes = bos.toByteArray();

            // Object byte count
            buffer.appendInt(objectBytes.length);

            // Object data
            buffer.appendBytes(objectBytes);
        }
        catch (final IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    @Override
    public T decodeFromWire(final int pos, final Buffer buffer) {
        var bufferPosition = pos;

        final var objectByteCount = buffer.getInt(bufferPosition);

        bufferPosition += 4; // jump to the beginning of the next byte (Integer is 4 bytes)

        byte[] objectBytes = buffer.getBytes(bufferPosition, bufferPosition + objectByteCount);

        try (
            final var bis = new ByteArrayInputStream(objectBytes)) {
            final var ois = new ObjectInputStream(bis);

            @SuppressWarnings("unchecked") final T msg = (T) ois.readObject();
            return msg;
        }
        catch (final IOException | ClassNotFoundException e) {
            LOGGER.error("Decode failed " + e.getMessage());
        }
        return null;
    }

    @Override
    public T transform(final T customMessage) {
        return customMessage;
    }

    @Override
    public String name() {
        return this.cls.getSimpleName() + "Codec";
    }

    @Override
    public byte systemCodecID() {
        return -1; // TODO
    }
}
