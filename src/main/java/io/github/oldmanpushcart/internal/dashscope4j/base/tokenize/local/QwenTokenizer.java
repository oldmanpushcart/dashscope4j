package io.github.oldmanpushcart.internal.dashscope4j.base.tokenize.local;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * QwenTokenizer
 * <p>copy from dashscope-sdk</p>
 */
class QwenTokenizer {

    private static final String SPECIAL_START = "<|";
    private static final String SPECIAL_END = "|>";
    private static final String SPECIAL_EOT = "<|endoftext|>";
    private static final String SPECIAL_IM_START = "<|im_start|>";
    private static final String SPECIAL_IM_END = "<|im_end|>";
    private static final Pattern PATTEN_STRING = Pattern.compile("(?i:'s|'t|'re|'ve|'m|'ll|'d)|[^\\r\\n\\p{L}\\p{N}]?\\p{L}+|\\p{N}| ?[^\\s\\p{L}\\p{N}]+[\\r\\n]*|\\s*[\\r\\n]+|\\s+(?!\\S)|\\s+");
    private static final int SPECIAL_START_ID = 151643;
    private static final String TOKEN_RANK_SEPARATOR = " ";
    private static final String vocabularyBpeFile = "qwen/qwen.tiktoken";
    private static final Map<EncodeBytesEntity, Integer> bpeMerges = initBpeMerges();
    private static final Map<String, Integer> specialBpeMerges = initSpecialBpeMerges();
    private static final Map<Integer, byte[]> reverseBpeMerges = initReverseBpeMerges();

    private static Map<String, Integer> initSpecialBpeMerges() {
        final var map = new LinkedHashMap<String, Integer>();
        int specialStartIndex = SPECIAL_START_ID;
        map.put(SPECIAL_EOT, specialStartIndex++);
        map.put(SPECIAL_IM_START, specialStartIndex++);
        map.put(SPECIAL_IM_END, specialStartIndex++);
        for (int i = 0; i < 205; i++) {
            String specialToken = String.format("<|extra_%d|>", i);
            map.put(specialToken, specialStartIndex++);
        }
        return Collections.unmodifiableMap(map);
    }

    private static Map<EncodeBytesEntity, Integer> initBpeMerges() {
        final var map = new LinkedHashMap<EncodeBytesEntity, Integer>();
        try (final var scanner = new Scanner(Objects.requireNonNull(QwenTokenizer.class.getClassLoader().getResourceAsStream(vocabularyBpeFile)), UTF_8)) {
            while (scanner.hasNextLine()) {
                final var line = scanner.nextLine();

                final var splits = line.split(TOKEN_RANK_SEPARATOR);
                assert splits.length == 2 : "Invalid line in " + vocabularyBpeFile + ": " + line;

                final var token = Base64.getDecoder().decode(splits[0].getBytes(UTF_8));
                final var rank = Integer.parseInt(splits[1]);
                map.put(new EncodeBytesEntity(token, rank), rank);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private static Map<Integer, byte[]> initReverseBpeMerges() {
        final var map = new HashMap<Integer, byte[]>(bpeMerges.size() + specialBpeMerges.size());
        bpeMerges.forEach((k, v) -> map.put(v, k.bytes));
        specialBpeMerges.forEach((k, v) -> map.put(v, k.getBytes(UTF_8)));
        return Collections.unmodifiableMap(map);
    }

    public QwenTokenizer() {
    }

    private static EncodeBytesEntity mergePair(EncodeBytesEntity first, EncodeBytesEntity second) {
        final var mergeBytes = new byte[first.bytes.length + second.bytes.length];
        System.arraycopy(first.bytes, 0, mergeBytes, 0, first.bytes.length);
        System.arraycopy(second.bytes, 0, mergeBytes, first.bytes.length, second.bytes.length);
        return new EncodeBytesEntity(mergeBytes);
    }

    private static EncodeBytesEntity[] merge(EncodeBytesEntity[] ids, EncodeBytesEntity bytePair) {
        final var merged = new EncodeBytesEntity[ids.length];
        int mergedIndex = 0;
        for (int i = 0; i < ids.length; ) {
            if (i < ids.length - 1) {
                final var mergePair = mergePair(ids[i], ids[i + 1]);
                if (mergePair.equals(bytePair)) {
                    merged[mergedIndex++] = bytePair;
                    i += 2;
                } else {
                    merged[mergedIndex++] = ids[i];
                    i += 1;
                }
            } else {
                merged[mergedIndex++] = ids[i];
                i += 1;
            }
        }
        return Arrays.copyOfRange(merged, 0, mergedIndex);
    }

    private EncodeBytesEntity getLowestIndexBytePair(EncodeBytesEntity[] ids) {
        final var uniqueSet = new HashSet<EncodeBytesEntity>();
        int minRank = Integer.MAX_VALUE;
        EncodeBytesEntity minRankPair = null;
        for (int i = 0; i < ids.length - 1; ++i) {
            final var mergePair = mergePair(ids[i], ids[i + 1]);
            if (uniqueSet.contains(mergePair)) {
                continue;
            }
            final var rank = bpeMerges.get(mergePair);
            if (rank == null) {
                mergePair.rank = Integer.MAX_VALUE;
            } else {
                mergePair.rank = rank;
                if (rank < minRank) {
                    minRank = rank;
                    minRankPair = mergePair;
                }
            }
            uniqueSet.add(mergePair);
        }
        return minRankPair;
    }


    // Encode chunk return the token ids
    private List<Integer> encodeChunk(String chunk) {
        final var chunkBytes = chunk.getBytes(UTF_8);
        EncodeBytesEntity[] ids = new EncodeBytesEntity[chunkBytes.length];
        // convert bytes to integers range 0..255
        int idx = 0;
        for (byte b : chunkBytes) {
            EncodeBytesEntity rankKey = new EncodeBytesEntity(new byte[]{b});
            rankKey.rank = bpeMerges.get(rankKey);
            ids[idx++] = rankKey;
        }
        List<Integer> tokens = new ArrayList<>();
        if (ids.length < 2) {
            for (EncodeBytesEntity key : ids) {
                tokens.add(key.rank);
            }
            return tokens;
        }
        // merge the byte pair
        while (ids.length >= 2) {
            // find the lowest rank mergeable byte pair
            EncodeBytesEntity bytePair = getLowestIndexBytePair(ids);
            if (bytePair == null) { // no more token can be merged.
                break;
            }
            // merge the lowest merge index
            ids = merge(ids, bytePair);
        }
        for (EncodeBytesEntity key : ids) {
            tokens.add(key.rank);
        }
        return tokens;
    }

    /**
     * Encoding that ignores any special tokens.
     *
     * @param text The input.
     * @return The list of token ids.
     */
    public List<Integer> encodeOrdinary(String text) {
        final var tokenIds = new ArrayList<Integer>();
        // 1. split the input text to trunks use regex
        for (final var matcher = PATTEN_STRING.matcher(text); matcher.find(); ) {
            // encode the chunk.
            tokenIds.addAll(encodeChunk(matcher.group()));
        }
        return tokenIds;
    }

    private List<String> splitWithSpecial(String text) {
        return text.contains(SPECIAL_START) && text.contains(SPECIAL_END)
                ? splitByStrings(text, specialBpeMerges.keySet())
                : List.of(text);
    }

    public enum SpecialTokenMode {
        ALL,
        NONE,
        NONE_RAISE
    }

    /**
     * Encode the input text, handles special tokens.
     *
     * @param text The input to be encoded.
     * @param mode The special token options can be "all"|"none"|"none_raise", if
     *             none_raise, then an error is raised if any special token is encountered in text, if null,
     *             use "all"
     * @return The list of token encoding.
     * @throws NoSpecialTokenExistsException        No special token in the input.
     * @throws UnSupportedSpecialTokenModeException the allowedSpecial is not["all"|"none"|"none_raise"]
     */
    public List<Integer> encode(String text, SpecialTokenMode mode) {
        if (mode == null) {
            mode = SpecialTokenMode.ALL;
        }
        final var usedSpecialBpeMerges = initUsedSpecialBpeMerges(text, mode);

        // use ordinary encode
        if (usedSpecialBpeMerges.isEmpty()) {
            return encodeOrdinary(text);
        }

        /*
         * 1. process special tokens. split the text with special tokens.
         *
         * e.g.:
         * <|im_start|>system\nYou are a helpful assistant.<|im_end|>
         * <|im_start|>user\nSan Francisco is a<|im_end|>
         * <|im_start|>assistant\n"
         *
         * will be split to:
         * [
         *  "<|im_start|>",
         *  "system\nYou are a helpful assistant.",
         *  "<|im_end|>",
         *  "\n",
         *  "<|im_start|>",
         *  "user\nSan Francisco is a",
         *  "<|im_end|>",
         *  "\n",
         *  "<|im_start|>",
         *  "assistant\n"
         * ]
         */
        final var chunks = splitWithSpecial(text);

        // 2. process the chunks
        final var tokens = new ArrayList<Integer>();
        for (String chunk : chunks) {
            if (usedSpecialBpeMerges.containsKey(chunk)) {
                tokens.add(usedSpecialBpeMerges.get(chunk)); // is a special token
            } else {
                tokens.addAll(encodeOrdinary(chunk)); // ordinary inputs
            }
        }
        return tokens;
    }

    private static Map<String, Integer> initUsedSpecialBpeMerges(String text, SpecialTokenMode mode) {
        Map<String, Integer> specialTokensUse;
        switch (mode) {
            case ALL -> specialTokensUse = specialBpeMerges;
            case NONE -> specialTokensUse = new LinkedHashMap<>();
            case NONE_RAISE -> {
                specialTokensUse = new LinkedHashMap<>();
                boolean isSpecialTokenExists = false;
                for (String token : specialBpeMerges.keySet()) {
                    if (text.contains(token)) {
                        isSpecialTokenExists = true;
                        break;
                    }
                }
                if (!isSpecialTokenExists) {
                    throw new NoSpecialTokenExistsException(String.format("No special token in %s", text));
                }
            }
            default ->
                    throw new UnSupportedSpecialTokenModeException(String.format("UnSupport allowedSpecial: %s", mode));
        }
        return specialTokensUse;
    }

    public String decode(List<Integer> tokens) {
        return tokens.stream()
                .map(token -> new String(reverseBpeMerges.get(token), UTF_8))
                .collect(Collectors.joining());
    }

    public String mapping(Integer token) {
        return new String(reverseBpeMerges.get(token), UTF_8);
    }

    private static class UnSupportedSpecialTokenModeException extends RuntimeException {
        public UnSupportedSpecialTokenModeException(String msg) {
            super(msg);
        }
    }

    private static class NoSpecialTokenExistsException extends RuntimeException {
        public NoSpecialTokenExistsException(String msg) {
            super(msg);
        }
    }

    private static List<String> splitByStrings(String text, Collection<String> spliters) {
        List<String> chunks = new ArrayList<>();
        chunks.add(text);
        for (String specialToken : spliters) {
            List<String> thisSplits = new ArrayList<>();
            for (String chunk : chunks) {
                thisSplits.addAll(splitByString(chunk, specialToken));
            }
            chunks = thisSplits;
        }
        return chunks;
    }

    private static List<String> splitByString(String src, String spliter) {
        List<String> parts = new ArrayList<>();
        int from = 0;
        int first = src.indexOf(spliter, from);
        while (first != -1) {
            if (from == first) { // starts with special
                parts.add(spliter);
                from += spliter.length();
            } else {
                parts.add(src.substring(from, first));
                parts.add(spliter);
                from += first - from + spliter.length();
            }
            first = src.indexOf(spliter, from);
        }
        String remain = src.substring(from);
        if (!remain.isEmpty()) {
            parts.add(src.substring(from));
        }
        return parts;
    }

    private static class EncodeBytesEntity {
        public final byte[] bytes;
        public int rank = Integer.MAX_VALUE;

        public EncodeBytesEntity(byte[] bytes) {
            this.bytes = bytes;
        }

        public EncodeBytesEntity(byte[] bytes, int rank) {
            this.bytes = bytes;
            this.rank = rank;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof EncodeBytesEntity other
                   && Arrays.equals(bytes, other.bytes);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(bytes);
        }

        @Override
        public String toString() {
            return Arrays.toString(bytes);
        }

    }

    public static void main(String... args) {
        final var tokenizer = new QwenTokenizer();
        final var list = tokenizer.encodeOrdinary("""
                北京有哪些好玩地方？
                故宫、颐和园、天坛等都是可以去游玩的景点哦。
                帮我安排一些行程
                """);

        System.out.println(tokenizer.decode(list));

    }

}
