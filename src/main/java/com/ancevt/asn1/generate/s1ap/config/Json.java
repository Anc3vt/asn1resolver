package com.ancevt.asn1.generate.s1ap.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class Json {
    public static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(SerializationFeature.INDENT_OUTPUT);
    private Json() { }
    public static JsonNode read(Path path) throws IOException { return MAPPER.readTree(Files.readString(path)); }
    public static String write(Object value) throws IOException { return MAPPER.writeValueAsString(value) + "\n"; }
    public static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
