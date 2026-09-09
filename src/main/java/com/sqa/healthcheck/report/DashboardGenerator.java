package com.sqa.healthcheck.report;

import com.sqa.healthcheck.model.CheckResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardGenerator {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private static final ZoneId PAKISTAN_ZONE = ZoneId.of("Asia/Karachi");

    private String toPakistanTime(java.time.LocalDateTime utcTime) {
        return utcTime.atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(PAKISTAN_ZONE)
                .format(TIME_FORMAT);
    }
private String getPerformanceBadge(long responseTimeMillis) {
    double seconds = responseTimeMillis / 1000.0;
    if (seconds < 15) {
        return "<span class=\"perf perf-healthy\">\uD83D\uDFE2 Healthy</span>";
    } else if (seconds <= 35) {
        return "<span class=\"perf perf-slow\">\uD83D\uDFE1 Slow</span>";
    } else {
        return "<span class=\"perf perf-critical\">\uD83D\uDD34 Critical</span>";
    }
}
   
    }

    public void generate(List<CheckResult> results, String outputPath) throws IOException {
        long upCount = results.stream().filter(r -> r.status == CheckResult.Status.UP).count();
        long downCount = results.size() - upCount;
        String generatedAt = toPakistanTime(java.time.LocalDateTime.now());

        StringBuilder rows = new StringBuilder();
        for (CheckResult r : results) {
            boolean isUp = r.status == CheckResult.Status.UP;

            String screenshotCell;
            if (r.screenshotBase64 != null) {
                String dataUri = "data:image/png;base64," + r.screenshotBase64;
                screenshotCell = "<a href=\"" + dataUri + "\" target=\"_blank\">"
                        + "<img src=\"" + dataUri + "\" alt=\"Screenshot\" "
                        + "style=\"width:100px;border:1px solid #ddd;border-radius:4px;cursor:pointer;display:block;\">"
                        + "</a>";
            } else {
                screenshotCell = "-";
            }

            rows.append("<tr>")
                    .append("<td>").append(escape(r.name)).append("</td>")
                    .append("<td><span class=\"badge ").append(isUp ? "badge-up" : "badge-down").append("\">")
                    .append(isUp ? "UP" : "DOWN").append("</span></td>")
                    .append("<td>").append(r.responseTimeMillis).append(" ms</td>")
                    .append("<td>").append(getPerformanceBadge(r.responseTimeMillis)).append("</td>")
                    .append("<td>").append(isUp ? "-" : escape(r.failureReason)).append("</td>")
                    .append("<td>").append(screenshotCell).append("</td>")
                    .append("<td>").append(toPakistanTime(r.checkedAt)).append("</td>")
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
                  table { width: 100%%; border-collapse: collapse; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
                  th, td { padding: 12px 16px; text-align: left; font-size: 13px; border-bottom: 1px solid #eee; }
                  th { background: #fafafa; font-weight: 700; color: #444; text-transform: uppercase; font-size: 11px; letter-spacing: 0.04em; }
                  tr:last-child td { border-bottom: none; }
                  .badge { display: inline-block; padding: 3px 10px; border-radius: 12px; font-size: 11px; font-weight: 700; letter-spacing: 0.03em; }
                  .badge-up { background: #dcfce7; color: #15803d; }
                  .badge-down { background: #fee2e2; color: #b91c1c; }
                  .perf { font-size: 12px; font-weight: 600; white-space: nowrap; }
                  .perf-healthy { color: #15803d; }
                  .perf-slow { color: #b45309; }
                  .perf-critical { color: #b91c1c; }
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
                        <th>Performance</th>
                        <th>Failure Reason</th>
                        <th>Screenshot</th>
                        <th>Checked At</th>
                      </tr>
                    </thead>
                    <tbody>
                %s
                    </tbody>
                  </table>

                  <footer>
                    <div style="margin-bottom: 6px;">
                      Performance criteria based on response time:
                   <span class="perf perf-healthy" style="margin-left:6px;">\uD83D\uDFE2 Healthy (under 15s)</span>
<span class="perf perf-slow" style="margin-left:10px;">\uD83D\uDFE1 Slow (15\u201335s)</span>
<span class="perf perf-critical" style="margin-left:10px;">\uD83D\uDD34 Critical (over 35s)</span>
                    </div>
                    Generated automatically by the SQA Teams Health Check Agent
                  </footer>
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
