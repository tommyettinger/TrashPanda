package trash.panda;

import regexodus.*;
import regexodus.ds.CharCharMap;
import sarong.rng.StatefulRNG;
import sarong.util.Compatibility;

import java.util.ArrayList;

/**
 * Created by Tommy Ettinger on 8/21/2016.
 */
public class Scavenging {

    /**
     * 2<sup>64</sup> &middot; &phi;, &phi; = (&#x221A;5 &minus; 1)/2.
     */
    private static final long LONG_PHI = 0x9E3779B97F4A7C15L;
    /**
     * The reciprocal of {@link #LONG_PHI} modulo 2<sup>64</sup>.
     */
    private static final long INV_LONG_PHI = 0xF1DE83E19937733DL;

    /**
     * Quickly mixes the bits of a long integer.
     * <br>This method mixes the bits of the argument by multiplying by the golden ratio and
     * xorshifting twice the result. It is borrowed from <a href="https://github.com/OpenHFT/Koloboke">Koloboke</a>.
     *
     * @param x a long integer.
     * @return a hash value obtained by mixing the bits of {@code x}.
     */
    private static long mix(final long x) {
        long h = x * LONG_PHI;
        h ^= h >>> 32;
        return h ^ (h >>> 16);
    }

    /**
     * The inverse of {@link #mix(long)}. Used during unscrambling on bits mixed with {@link #mix(long)}.
     *
     * @param x a long integer.
     * @return a value that passed through {@link #mix(long)} would give {@code x}.
     */
    private static long invMix(long x) {
        x ^= x >>> 32;
        x ^= x >>> 16;
        return (x ^ x >>> 32) * INV_LONG_PHI;
    }

    public static final Pattern identifierPattern =
            Pattern.compile("(?<!\\p{Jp})\\p{Js}\\p{Jp}*"),
            numberPattern =
                    Pattern.compile("(?<!\\p{Jp})(?:" +
                            "({=INT}(?:({=BASE8}-?(?=0)[0-9]+)|({=BASE10}-?[1-9][0-9]*)|({=BASE16}-?0[Xx][0-9A-Fa-f]+))" +
                            "({=INTSUFFIX}(?:[Uu]?[Nn]?[Ll]{0,2})))|" +
                            "({=REAL}({=REALBODY}-?(?=[1-9]|0(?![0-9]))[0-9]+(\\.[0-9]+)?([Ee][+-]?[0-9]+)?)({=REALSUFFIX}[FfMm]?)))");
    public static char[] identifierStarts = Category.IdentifierStart.contents(),
            identifierParts = Category.IdentifierPart.contents();

    private static String scavenge(String original, CharCharMap startMapping, CharCharMap restMapping) {
        if (original == null || original.isEmpty())
            return "";
        char[] trash = new char[original.length()];
        trash[0] = startMapping.get(original.charAt(0));
        for (int i = 1; i < original.length(); i++) {
            trash[i] = restMapping.get(original.charAt(i));
        }
        return String.valueOf(trash);
    }

    /**
     * Takes a MatchResult matching a number, reads in the number, takes a figurative shotgun to the bits, and
     * re-encodes to the same numeric type before returning as a String.
     *
     * @param original a MatchResult matching a number
     * @return a mangled version of the original number, as a String
     */
    private static String shotgun(MatchResult original) {
        String check = original.group("REAL"), suffix;
        if (check != null && !check.isEmpty()) {
            suffix = original.group("REALSUFFIX");
            check = original.group("REALBODY");
            return String.valueOf(Double.longBitsToDouble(mix(Double.doubleToLongBits(Double.parseDouble(check))))) + suffix;
        }
        check = original.group("INT");
        if (check != null && !check.isEmpty()) {
            suffix = original.group("INTSUFFIX");
            check = original.group("BASE8");
            if (check != null && !check.isEmpty()) {
                return String.valueOf(mix(Long.parseLong(check, 8) + INV_LONG_PHI)) + suffix;
            }
            check = original.group("BASE10");
            if (check != null && !check.isEmpty()) {
                return String.valueOf(mix(Long.parseLong(check) + INV_LONG_PHI)) + suffix;
            }
            check = original.group("BASE16");
            if (check != null && !check.isEmpty()) {
                return String.valueOf(mix(Long.parseLong(check, 16) + INV_LONG_PHI)) + suffix;
            }
        }
        return original.group();
    }

    /**
     * Takes a MatchResult matching a number that has been blown apart by {@link #shotgun(MatchResult)}, reads
     * in the number, reconstitutes the bits as if making a fur hat, and re-encodes to the correct type of
     * number as a String.
     *
     * @param original a String storing a double
     * @return a de-mangled version of the original double.
     */
    private static String hat(MatchResult original) {
        String check = original.group("REAL"), suffix;
        if (check != null && !check.isEmpty()) {
            suffix = original.group("REALSUFFIX");
            check = original.group("REALBODY");
            return String.valueOf(Double.longBitsToDouble(invMix(Double.doubleToLongBits(Double.parseDouble(check))))) + suffix;
        }
        check = original.group("INT");
        if (check != null && !check.isEmpty()) {
            suffix = original.group("INTSUFFIX");
            check = original.group("BASE8");
            if (check != null && !check.isEmpty()) {
                return String.valueOf(invMix(Long.parseLong(check, 8)) - INV_LONG_PHI) + suffix;
            }
            check = original.group("BASE10");
            if (check != null && !check.isEmpty()) {
                return String.valueOf(invMix(Long.parseLong(check)) - INV_LONG_PHI) + suffix;
            }
            check = original.group("BASE16");
            if (check != null && !check.isEmpty()) {
                return String.valueOf(invMix(Long.parseLong(check, 16)) - INV_LONG_PHI) + suffix;
            }
        }
        return original.group();
    }

    public static String scramble(String input, long seed) {
        StatefulRNG iRNG = new StatefulRNG(seed),
                nRNG = new StatefulRNG(iRNG.nextLong()),
                oRNG;
        Matcher match = identifierPattern.matcher(input);
        ArrayList<String> segments = match.foundStrings();
        int ilen = identifierParts.length;
        int[] mixer = iRNG.randomOrdering(segments.size());
        oRNG = new StatefulRNG(iRNG.nextLong());
        int[] megaMix = oRNG.randomOrdering(ilen);
        CharCharMap partMapping = new CharCharMap(ilen);
        for (int i = 0; i < ilen; i++) {
            partMapping.put(identifierParts[i], identifierParts[megaMix[i]]);
        }
        ilen = identifierStarts.length;
        megaMix = oRNG.randomOrdering(ilen);
        CharCharMap startMapping = new CharCharMap(ilen);
        for (int i = 0; i < ilen; i++) {
            startMapping.put(identifierStarts[i], identifierStarts[megaMix[i]]);
        }
        match.setPosition(0);
        for (int i = 0; i < mixer.length; i++) {
            segments.set(i, scavenge(segments.get(i), startMapping, partMapping));
        }
        Scrambler scram = new Scrambler(mixer, segments);
        String altered = match.replaceAll(scram);
        match = numberPattern.matcher(altered);
        segments.clear();
        MatchIterator it = match.findAll();
        while (it.hasNext()) {
            segments.add(shotgun(it.next()));
        }
        mixer = nRNG.randomOrdering(segments.size());
        match.setPosition(0);

        scram.reset(mixer, segments);
        return match.replaceAll(scram);
    }

    private static class Scrambler implements Substitution {
        private int counter;
        private int[] ordering;
        private ArrayList<String> segments;

        public Scrambler(int[] ordering, ArrayList<String> segments) {
            this.ordering = ordering;
            this.segments = segments;
            counter = 0;
        }

        public void reset(int[] ordering, ArrayList<String> segments) {
            this.ordering = ordering;
            this.segments = segments;
            counter = 0;
        }

        @Override
        public void appendSubstitution(MatchResult match, TextBuffer dest) {
            dest.append(segments.get(ordering[counter++]));
        }
    }

    public static String unscramble(String input, long seed) {
        StatefulRNG iRNG = new StatefulRNG(seed),
                nRNG = new StatefulRNG(iRNG.nextLong()),
                oRNG;
        Matcher match = identifierPattern.matcher(input);
        ArrayList<String> segments = match.foundStrings();
        int ilen = identifierParts.length;
        int[] mixer = Compatibility.invertOrdering(iRNG.randomOrdering(segments.size()));
        oRNG = new StatefulRNG(iRNG.nextLong());
        int[] megaMix = oRNG.randomOrdering(ilen);
        CharCharMap partMapping = new CharCharMap(ilen);
        for (int i = 0; i < ilen; i++) {
            partMapping.put(identifierParts[megaMix[i]], identifierParts[i]);
        }
        ilen = identifierStarts.length;
        megaMix = oRNG.randomOrdering(ilen);
        CharCharMap startMapping = new CharCharMap(ilen);
        for (int i = 0; i < ilen; i++) {
            startMapping.put(identifierStarts[megaMix[i]], identifierStarts[i]);
        }
        match.setPosition(0);
        for (int i = 0; i < mixer.length; i++) {
            segments.set(i, scavenge(segments.get(i), startMapping, partMapping));
        }
        Scrambler scram = new Scrambler(mixer, segments);
        String altered = match.replaceAll(scram);

        match = numberPattern.matcher(altered);
        segments.clear();
        MatchIterator it = match.findAll();
        while (it.hasNext()) {
            segments.add(hat(it.next()));
        }
        mixer = Compatibility.invertOrdering(nRNG.randomOrdering(segments.size()));
        match.setPosition(0);

        scram.reset(mixer, segments);
        return match.replaceAll(scram);
    }
}
