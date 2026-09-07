package com.ancevt.asn1.generate.s1ap.normalize;

import com.ancevt.asn1.generate.s1ap.diagnostic.GenerationException;
import com.ancevt.asn1.model.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Immutable effective root set; separated intervals are retained for validation. */
public record Bounds(List<Interval> intervals, boolean extensible) {
    public record Interval(BigInteger min, BigInteger max) { }
    public Bounds { intervals = List.copyOf(intervals); }
    public BigInteger min() { return intervals.get(0).min(); }
    public BigInteger max() { return intervals.get(intervals.size() - 1).max(); }
    public static Bounds normalize(List<Constraint> constraints) {
        Bounds result = null;
        for (Constraint c : constraints) {
            if (c.getKind() == Constraint.Kind.TABLE) continue;
            Bounds next = constraint(c);
            if (result == null) result = next;
            else {
                // Intersection of closed root ranges is exact. Extensible subtype intersections
                // need an extension-set algebra; refuse them instead of widening a constraint.
                if (result.extensible || next.extensible) throw unsupported(c, "Extensible alias intersection");
                List<Interval> values = new ArrayList<>();
                for (Interval a : result.intervals) for (Interval b : next.intervals) {
                    BigInteger min = a.min.max(b.min), max = a.max.min(b.max);
                    if (min.compareTo(max) <= 0) values.add(new Interval(min, max));
                }
                if (values.isEmpty()) throw unsupported(c, "Empty subtype intersection");
                result = new Bounds(merge(values), false);
            }
        }
        return result;
    }
    private static Bounds constraint(Constraint c) {
        if (c.isExtensible()) {
            String tail = c.getSourceText().stripTrailing();
            while (tail.endsWith(")")) tail = tail.substring(0, tail.length() - 1).stripTrailing();
            if (!tail.endsWith("...")) throw unsupported(c, "Explicit extension constraint set requires extension-set algebra");
        }
        List<Constraint> children = c.getChildren();
        return switch (c.getKind()) {
            case SIZE -> {
                if (children.size() != 1) throw unsupported(c, "SIZE without typed child");
                yield constraint(children.get(0));
            }
            case RANGE -> {
                if (children.size() != 2) throw unsupported(c, "Incomplete range");
                BigInteger min = leaf(children.get(0)), max = leaf(children.get(1));
                if (min.compareTo(max) > 0) throw unsupported(c, "Reversed range");
                yield new Bounds(List.of(new Interval(min, max)), c.isExtensible());
            }
            case UNION -> {
                List<Interval> items = new ArrayList<>();
                for (Constraint child : children) items.addAll(constraint(child).intervals);
                yield new Bounds(merge(items), c.isExtensible());
            }
            case SINGLE_VALUE -> {
                BigInteger value = leaf(c);
                yield new Bounds(List.of(new Interval(value, value)), c.isExtensible());
            }
            default -> throw unsupported(c, "Unsupported constraint " + c.getKind());
        };
    }
    private static BigInteger leaf(Constraint c) {
        if (c.getLiteralInteger() != null) return c.getLiteralInteger();
        // The resolver retains parenthesis delimiters on SINGLE_VALUE nodes.
        String literal = c.getSourceText().trim();
        while (literal.startsWith("(") && literal.endsWith(")")) literal = literal.substring(1, literal.length() - 1).trim();
        if (c.isExtensible()) literal = literal.replaceFirst(",\\s*\\.\\.\\.$", "").trim();
        if (c.getReferences().size() == 1 && c.getReferences().get(0).getTarget() instanceof ValueAssignment v
                && literal.equals(c.getReferences().get(0).getQualifiedName())) return IntegerValues.evaluate(v);
        try { return new BigInteger(literal); }
        catch (NumberFormatException e) { throw unsupported(c, "Expected finite linked bound"); }
    }
    private static List<Interval> merge(List<Interval> intervals) {
        List<Interval> sorted = new ArrayList<>(intervals);
        sorted.sort(Comparator.comparing(Interval::min));
        List<Interval> result = new ArrayList<>();
        for (Interval i : sorted) {
            if (!result.isEmpty() && result.get(result.size() - 1).max.add(BigInteger.ONE).compareTo(i.min) >= 0) {
                Interval last = result.remove(result.size() - 1);
                result.add(new Interval(last.min, last.max.max(i.max)));
            } else result.add(i);
        }
        return result;
    }
    private static GenerationException unsupported(Constraint c, String message) {
        return new GenerationException("UNSUPPORTED_CONSTRAINT", 6, message + ": " + c.getSourceText(), c.getSourceRange(), List.of());
    }
}
