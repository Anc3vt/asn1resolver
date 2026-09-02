package com.ancevt.asn1;

import com.ancevt.asn1.model.Asn1Document;
import com.ancevt.asn1.parse.Asn1Parser;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Concise facade intended for application code and debugger exploration. */
public final class Asn1 {
    private Asn1() { }

    public static Asn1Document read(Path path) throws IOException {
        return read(path, StandardCharsets.UTF_8);
    }

    public static Asn1Document read(Path path, Charset charset) throws IOException {
        Path absolute = path.toAbsolutePath().normalize();
        return Asn1Parser.parse(Files.readString(absolute, charset), absolute.toString());
    }

    public static Asn1Document parse(String source) {
        return Asn1Parser.parse(source);
    }

    public static Asn1Document parse(String source, String sourceName) {
        return Asn1Parser.parse(source, sourceName);
    }
}
