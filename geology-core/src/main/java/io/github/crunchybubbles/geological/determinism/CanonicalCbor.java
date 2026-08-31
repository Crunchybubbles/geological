package io.github.crunchybubbles.geological.determinism;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;

/** Minimal deterministic RFC 8949 CBOR encoder for identity tuples. */
public final class CanonicalCbor {
  private CanonicalCbor() {}

  public static byte[] encodeTuple(Object... values) {
    return encode(List.of(values));
  }

  public static byte[] encode(Object value) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    write(output, value);
    return output.toByteArray();
  }

  private static void write(ByteArrayOutputStream output, Object value) {
    switch (value) {
      case String text -> writeText(output, text);
      case Integer number -> writeInteger(output, number.longValue());
      case Long number -> writeInteger(output, number);
      case byte[] bytes -> writeBytes(output, bytes);
      case Boolean bool -> output.write(bool ? 0xf5 : 0xf4);
      case List<?> list -> {
        writeHeader(output, 4, list.size());
        list.forEach(item -> write(output, item));
      }
      case null -> output.write(0xf6);
      default ->
          throw new IllegalArgumentException(
              "Unsupported canonical CBOR type: " + value.getClass().getName());
    }
  }

  private static void writeInteger(ByteArrayOutputStream output, long value) {
    if (value >= 0) {
      writeHeader(output, 0, value);
    } else {
      writeHeader(output, 1, -1L - value);
    }
  }

  private static void writeText(ByteArrayOutputStream output, String value) {
    String normalized = Normalizer.normalize(value, Normalizer.Form.NFC);
    byte[] bytes = normalized.getBytes(StandardCharsets.UTF_8);
    writeHeader(output, 3, bytes.length);
    output.writeBytes(bytes);
  }

  private static void writeBytes(ByteArrayOutputStream output, byte[] value) {
    writeHeader(output, 2, value.length);
    output.writeBytes(value);
  }

  private static void writeHeader(ByteArrayOutputStream output, int majorType, long value) {
    if (value < 0) {
      throw new IllegalArgumentException("CBOR length/value must not be negative");
    }
    int prefix = majorType << 5;
    if (value < 24) {
      output.write(prefix | (int) value);
    } else if (value <= 0xffL) {
      output.write(prefix | 24);
      output.write((int) value);
    } else if (value <= 0xffffL) {
      output.write(prefix | 25);
      writeBigEndian(output, value, 2);
    } else if (value <= 0xffff_ffffL) {
      output.write(prefix | 26);
      writeBigEndian(output, value, 4);
    } else {
      output.write(prefix | 27);
      writeBigEndian(output, value, 8);
    }
  }

  private static void writeBigEndian(ByteArrayOutputStream output, long value, int bytes) {
    for (int shift = (bytes - 1) * Byte.SIZE; shift >= 0; shift -= Byte.SIZE) {
      output.write((int) (value >>> shift) & 0xff);
    }
  }
}
