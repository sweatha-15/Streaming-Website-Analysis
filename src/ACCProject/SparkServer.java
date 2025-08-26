// File: src/ACCProject/SparkServer.java
package ACCProject;

import static spark.Spark.*;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SparkServer {
    private static final int PORT = 3000;

    public static void main(String[] args) throws IOException {
        // ─── Spark setup ───────────────────────────────────────────────────────
        port(PORT);
        staticFiles.externalLocation("public");
        before((req, res) -> res.header("Access-Control-Allow-Origin", "*"));

        Gson gson = new Gson();

        // ─── 1) frequency (mentions count) ─────────────────────────────────────
        get("/frequency", (req, res) -> {
            String word = Optional.ofNullable(req.queryParams("word"))
                                  .orElse("")
                                  .trim();
            int count = PageRanking.countMentions(word);
            res.type("application/json");
            return gson.toJson(Map.of("word", word, "count", count));
        });

        // ─── 2) suggestions ─────────────────────────────────────────────────────
        get("/suggest", (req, res) -> {
            String query = Optional.ofNullable(req.queryParams("word"))
                                   .orElse("")
                                   .trim();
            List<String> suggestions =
                SpellChecker.getSuggestions(query, PageRanking.getDictionary());
            res.type("application/json");
            return gson.toJson(Map.of("word", query, "suggestions", suggestions));
        });

        // ─── 3) filter by title/genre/website ──────────────────────────────────
        get("/filter", (req, res) -> {
            String kw = Optional.ofNullable(req.queryParams("word")).orElse("");
            var filtered = PageRanking.getFilteredResults(kw);
            res.type("application/json");
            return gson.toJson(filtered);
        });

        // ─── 4) list all subscription plans ────────────────────────────────────
        get("/subscriptions", (req, res) -> {
            res.type("application/json");
            return gson.toJson(PageRanking.getAllSubscriptions());
        });

        // ─── 5) cheapest subscription plan ────────────────────────────────────
        get("/bestplans", (req, res) -> {
            res.type("application/json");
            return gson.toJson(PageRanking.getBestSubscriptionPlan());
        });
    }
}
