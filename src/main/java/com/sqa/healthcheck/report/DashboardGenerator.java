package com.sqa.healthcheck.report;

import com.sqa.healthcheck.model.CheckResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Builds a single self-contained HTML file listing every system's
 * status, response time, and failure reason (if any). No external
 * CSS/JS files needed, so it can be opened directly or hosted anywhere
 * (e.g. GitHub Pages) as one file.
 */
public class DashboardGenerator {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    public void generate(List<CheckResult> results, String outputPath) throws IOException {
        long upCount = results.stream().filter(r -> r.status == CheckResult.Status.UP).count();
        long downCount = results.size() - upCount;
        String generatedAt = java.time.LocalDateTime.now().format(TIME_FORMAT);

        StringBuilder rows = new StringBuilder();
        for (CheckResult r : results) {
            boolean isUp = r.status == CheckResult.Status.UP;
            rows.append("<tr>")
                    .append("<td>").append(escape(r.name)).append("</td>")
                    .append("<td><span class=\"badge ").append(isUp ? "badge-up" : "badge-down").append("\">")
                    .append(isUp ? "UP" : "DOWN").append("</span></td>")
                    .append("<td>").append(r.responseTimeMillis).append(" ms</td>")
                    .append("<td>").append(isUp ? "-" : escape(r.failureReason)).append("</td>")
                    .append("<td>").append(r.checkedAt.format(TIME_FORMAT)).append("</td>")
                    .append("</tr>\n");
        }

        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                <meta charset="UTF-8">
                <title>System Health Check Dashboard</title>
                <style>
                  body { font-family: -apple-system, Segoe UI, Roboto, Arial, sans-serif; background: #f5f6f8; margin: 0; padding: 32px; color: #1a1a1a; }
                  .container { max-width: 960px; margin: 0 auto; }
                  h1 { font-size: 22px; margin-bottom: 4px; }
                  .subtitle { color: #666; font-size: 13px; margin-bottom: 24px; }
                  .summary { display: flex; gap: 16px; margin-bottom: 24px; }
                  .card { flex: 1; background: white; border-radius: 8px; padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); text-align: center; }
                  .card .num { font-size: 28px; font-weight: 700; }
                  .card.up .num { color: #16a34a; }
                  .card.down .num { color: #dc2626; }
                  .card .label { font-size: 12px; color: #666; text-transform: uppercase; letter-spacing: 0.04em; }
                  table { width: 100%; border-collapse: collapse; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
                  th, td { padding: 12px 16px; text-align: left; font-size: 13px; border-bottom: 1px solid #eee; }
                  th { background: #fafafa; font-weight: 600; color: #444; text-transform: uppercase; font-size: 11px; letter-spacing: 0.04em; }
                  tr:last-child td { border-bottom: none; }
                  .badge { display: inline-block; padding: 3px 10px; border-radius: 12px; font-size: 11px; font-weight: 700; letter-spacing: 0.03em; }
                  .badge-up { background: #dcfce7; color: #15803d; }
                  .badge-down { background: #fee2e2; color: #b91c1c; }
                  footer { margin-top: 20px; font-size: 12px; color: #999; text-align: center; }
                </style>
                </head>
                <body>
                <div class="container">
                  <h1>System Health Check Dashboard</h1>
                  <div class="subtitle">Last run: %s</div>

                  <div class="summary">
                    <div class="card up"><div class="num">%d</div><div class="label">Systems Up</div></div>
                    <div class="card down"><div class="num">%d</div><div class="label">Systems Down</div></div>
                    <div class="card"><div class="num">%d</div><div class="label">Total Checked</div></div>
                  </div>

                  <table>
                    <thead>
                      <tr>
                        <th>System Name</th>
                        <th>Status</th>
                        <th>Response Time</th>
                        <th>Failure Reason</th>
                        <th>Checked At</th>
                      </tr>
                    </thead>
                    <tbody>
                %s
                    </tbody>
                  </table>

                  <footer>Generated automatically by the SQA Health Check Agent</footer>
                </div>
                </body>
                </html>
                """.formatted(generatedAt, upCount, downCount, results.size(), rows);

        Path path = Paths.get(outputPath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, html);
    }

    private String escape(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
