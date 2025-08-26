package ACCProject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

public class PageRanking {

    // ─── Point at your CSV in public/ ─────────────────────────────────────────────
    private static final String CSV_PATH = Paths
        .get(System.getProperty("user.dir"), "public", "all_content.csv")
        .toAbsolutePath()
        .toString();

    // ─── Load once at startup ─────────────────────────────────────────────────────
    private static final List<Map<String, String>> contentList = loadCsv(CSV_PATH);
    private static final List<String> dictionary  = buildDictionary(contentList);

    /** Exposed so SparkServer can call SpellChecker.getSuggestions(...) */
    public static List<String> getDictionary() {
        return dictionary;
    }

    /**
     * Search by title, genre, or website (case‑insensitive substring).
     */
    public static List<Map<String, String>> getFilteredResults(String keyword) {
        String kw = Optional.ofNullable(keyword).orElse("").toLowerCase();
        return contentList.stream()
            .filter(e ->
                e.get("title").toLowerCase().contains(kw) ||
                e.get("genre").toLowerCase().contains(kw) ||
                e.get("website").toLowerCase().contains(kw)
            )
            .collect(Collectors.toList());
    }

    /**
     * One entry per site with its subscription plan.
     */
    public static List<Map<String, String>> getAllSubscriptions() {
        Map<String,String> seen = new LinkedHashMap<>();
        for (var e : contentList) {
            seen.putIfAbsent(e.get("website"), e.get("subscription"));
        }
        List<Map<String,String>> out = new ArrayList<>();
        for (var kv : seen.entrySet()) {
            out.add(Map.of("website", kv.getKey(),
                           "subscription", kv.getValue()));
        }
        return out;
    }

    /**
     * Pick the single cheapest plan (parses first “$X.YY”).
     */
    public static Map<String, String> getBestSubscriptionPlan() {
        double best = Double.MAX_VALUE;
        Map<String,String> winner = Map.of("website","", "subscription","");
        Pattern p = Pattern.compile("\\$([0-9]+(?:\\.[0-9]+)?)");
        for (var e : contentList) {
            var m = p.matcher(e.get("subscription"));
            if (m.find()) {
                double price = Double.parseDouble(m.group(1));
                if (price < best) {
                    best = price;
                    winner = Map.of(
                        "website",      e.get("website"),
                        "subscription", e.get("subscription")
                    );
                }
            }
        }
        return winner;
    }

    // ─── Private helpers ──────────────────────────────────────────────────────────

    /** Build a word‑list from all titles, genres & websites for spell suggestions */
    private static List<String> buildDictionary(List<Map<String, String>> list) {
        Pattern pat = Pattern.compile("[A-Za-z']+");
        Set<String> set = new HashSet<>();
        for (var e : list) {
            String text = e.get("title") + " " + e.get("genre") + " " + e.get("website");
            Matcher m = pat.matcher(text);
            while (m.find()) {
                set.add(m.group().toLowerCase());
            }
        }
        return new ArrayList<>(set);
    }

    /** Read your CSV, strip quotes, normalize URLs & build each entry map */
    private static List<Map<String, String>> loadCsv(String path) {
        List<Map<String, String>> out = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
            if (lines.size() <= 1) return out;
            lines.remove(0);  // drop header

            for (String line : lines) {
                // split into at most 6 parts (subscription may contain commas)
                String[] cols = line.split(",", 6);
                if (cols.length < 6) continue;

                String site   = stripQuotes(cols[0].trim());
                String genre  = stripQuotes(cols[1].trim());
                String title  = stripQuotes(cols[2].trim());
                String rawLink= stripQuotes(cols[3].trim());
                String image  = stripQuotes(cols[4].trim());
                String sub    = stripQuotes(cols[5].trim());

                Map<String,String> m = new HashMap<>();
                m.put("website",      site);
                m.put("genre",        genre);
                m.put("title",        title);
                m.put("link",         normalizeUrl(rawLink, site));
                // allow either absolute or public‑folder paths
                if (image.toLowerCase().startsWith("http")) {
                    m.put("image", image);
                } else {
                    m.put("image", "/" + image.replaceAll("^/+", ""));
                }
                m.put("subscription", sub);
                out.add(m);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("🔍 Loaded " + out.size() + " entries from CSV");
        return out;
    }

    
    /** Remove wrapping "quotes" if present */
    private static String stripQuotes(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
    public static int countMentions(String keyword) {
        if (keyword == null || keyword.isBlank()) return 0;
        String kw = keyword.toLowerCase();
        int total = 0;
        for (Map<String,String> entry : contentList) {
            total += countOccurrences(entry.get("title"),       kw);
            total += countOccurrences(entry.get("genre"),       kw);
            total += countOccurrences(entry.get("subscription"), kw);
        }
        return total;
    }

    /** Helper to count non‑overlapping occurrences of sub in text */
private static int countOccurrences(String text, String sub) {
    if (text == null || sub.isEmpty()) return 0;
    int count = 0, idx = 0;
    while ((idx = text.toLowerCase().indexOf(sub, idx)) != -1) {
        count++;
        idx += sub.length();
    }
    return count;
}
    /**
     * Normalize a raw link into a proper absolute URL:
     *  • //foo/bar → https://foo/bar  
     *  • /baz      → https://site.com/baz  
     *  • http…     → unchanged  
     *  • else      → https:// + raw
     */
    private static String normalizeUrl(String url, String site) {
        if (url == null || url.isBlank()) return "";
        url = url.trim();
        if (url.startsWith("//")) {
            return "https:" + url;
        }
        if (url.startsWith("/")) {
            String domain = site.toLowerCase().replaceAll("[^a-z0-9]", "") + ".com";
            return "https://" + domain + url;
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        return "https://" + url;
    }
}
