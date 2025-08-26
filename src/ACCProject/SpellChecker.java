package ACCProject;
import java.util.*;
public class SpellChecker {
    private static Trie trie;
    /** Build the Trie from the word list once */
    public static void buildTrie(List<String> words) {
        trie = new Trie();
        for (String word : words) {
            trie.insert(word);
        }
    }
    /**
     * Called reflectively from SparkServer to return suggestions.
     * Must be public so SparkServer’s reflective lookup + invocation compiles cleanly.
     */
    public static List<String> getSuggestions(String query, List<String> wordList) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        if (trie == null) {
            buildTrie(wordList);
        }
        return trie.getSuggestions(query.toLowerCase(), 5);  // up to 5 suggestions
    }
}

/** Trie data structure used by SpellChecker */
class Trie {
    private static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isWordEnd = false;
    }

    private final TrieNode root = new TrieNode();

    public void insert(String word) {
        TrieNode node = root;
        for (char c : word.toLowerCase().toCharArray()) {
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
        }
        node.isWordEnd = true;
    }

    public List<String> getSuggestions(String prefix, int limit) {
        List<String> results = new ArrayList<>();
        TrieNode node = root;
        for (char c : prefix.toLowerCase().toCharArray()) {
            node = node.children.get(c);
            if (node == null) return results;
        }
        dfs(node, new StringBuilder(prefix), results, limit);
        return results;
    }

    private void dfs(TrieNode node, StringBuilder sb, List<String> results, int limit) {
        if (results.size() >= limit) return;
        if (node.isWordEnd) results.add(sb.toString());
        for (var e : node.children.entrySet()) {
            sb.append(e.getKey());
            dfs(e.getValue(), sb, results, limit);
            sb.setLength(sb.length() - 1);
        }
    }
}
