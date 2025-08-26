package ACCProject;

import java.io.*;

public class WordFrequencyBM {

    static class BoyerMoore {
        private final int ALPHABET_SIZE;
        private int[] skipTable;
        private String patternToFind;

        public BoyerMoore(String searchText) {
            this.ALPHABET_SIZE = 256;
            this.patternToFind = searchText;
            skipTable = new int[ALPHABET_SIZE];
            for (int i = 0; i < ALPHABET_SIZE; i++)
                skipTable[i] = -1;
            for (int j = 0; j < searchText.length(); j++)
                skipTable[searchText.charAt(j)] = j;
        }

        public int search(String textLine) {
            int m = patternToFind.length();
            int n = textLine.length();

            for (int i = 0; i <= n - m; ) {
                int j = m - 1;
                while (j >= 0 && patternToFind.charAt(j) == textLine.charAt(i + j)) {
                    j--;
                }
                if (j < 0) return i;
                int skip = Math.max(1, j - skipTable[textLine.charAt(i + j)]);
                i += skip;
            }
            return -1;
        }
    }

    // Clean string (lowercase, remove punctuation)
    private static String clean(String raw) {
        return raw.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    public static int countWithBM(String searchTerm) throws IOException {
        if (searchTerm == null || searchTerm.isEmpty()) return 0;

        File csvSource = new File("/Users/alekhyamysore/Desktop/streamfinder-project/public/all_content.csv");
        if (!csvSource.exists()) {
            throw new FileNotFoundException("CSV file not found");
        }

        String cleanedSearch = clean(searchTerm);
        if (cleanedSearch.isEmpty()) return 0;

        BoyerMoore bm = new BoyerMoore(cleanedSearch);
        BufferedReader reader = new BufferedReader(new FileReader(csvSource));
        String line;
        int count = 0;

        while ((line = reader.readLine()) != null) {
            String cleanedLine = clean(line);
            int fromIndex = 0;

            while (cleanedLine.length() - fromIndex >= cleanedSearch.length()) {
                String sub = cleanedLine.substring(fromIndex);
                int index = bm.search(sub);
                if (index == -1) break;
                count++;
                fromIndex += index + 1;
            }
        }

        reader.close();
        return count;
    }
}
