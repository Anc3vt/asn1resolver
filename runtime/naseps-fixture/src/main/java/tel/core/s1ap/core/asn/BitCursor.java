package tel.core.s1ap.core.asn;

/** Tracks the current bit position in an aligned PER stream. */
final class BitCursor {
    private int byteIndex;
    private int bitIndex;

    int byteIndex() {
        return byteIndex;
    }

    int bitIndex() {
        return bitIndex;
    }

    void advanceBit() {
        if (++bitIndex == Byte.SIZE) {
            bitIndex = 0;
            byteIndex++;
        }
    }

    void alignToByte() {
        if (bitIndex != 0) {
            bitIndex = 0;
            byteIndex++;
        }
    }
}
