package tel.core.s1ap.core.model;

import tel.core.s1ap.core.asn.BitInput;
import tel.core.s1ap.spec.ProtocolIeId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Registry used to decode OPEN TYPE payloads by ProtocolIE-ID. */
public final class InformationElementDecoderRegistry {
    private static final Map<Integer, Decoder> DECODERS = new ConcurrentHashMap<>();

    private InformationElementDecoderRegistry() {
    }

    public static void register(int id, Decoder decoder) {
        DECODERS.put(id, decoder);
    }

    public static void register(ProtocolIeId id, Decoder decoder) {
        register(id.getValue(), decoder);
    }

    public static Decoder getDecoder(int id) {
        tel.core.s1ap.spec.InformationElements.init();
        Decoder decoder = DECODERS.get(id);
        if (decoder == null) {
            throw new IllegalStateException("Decoder id %d not registered".formatted(id));
        }
        return decoder;
    }

    @FunctionalInterface
    public interface Decoder extends Function<BitInput, InformationElement> {
    }
}
