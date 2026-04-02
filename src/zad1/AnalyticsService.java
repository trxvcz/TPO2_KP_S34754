/**
 *
 *  @author Koc Paweł s34754
 *
 */

package zad1;


import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record AnalyticsService(
    LogParser logParser,
    TimestampRepairService timestampRepairService
) {

  public AnalysisReport analyze(GeoTimeOptions options, GeoLookup lookup) throws Exception {
    ZoneId serverZone = ZoneId.of(options.serverZoneId());

    int invalidLines = 0;
    List<LogEntry> entries = new ArrayList<>();
    if (options.logLines() != null) {
      for (String line:options.logLines()){
        Optional<LogEntry> entryLog = logParser.parseLine(line);
        if (entryLog.isPresent()) {
          entries.add(entryLog.get());
        } else {
          invalidLines++;
        }
      }
    }
    List<ResolvedLogEntry> resolvedEntries = timestampRepairService.repair(entries, serverZone);

    int repairedGaps = 0;
    int resolvedOverlaps = 0;
    int droppedAmbiguous = 0;
    List<String> droppedAmbiguousLines = new ArrayList<>();

    int geolookupFailures = 0;
    Map<String,Long> byCountry = new LinkedHashMap<>();
    Map<String,Long> byTimezone = new LinkedHashMap<>();
    long[] globalHourky = new long[24];
    Map<String, long[]> hourlyByTimezone = new LinkedHashMap<>();

    for (ResolvedLogEntry entry: resolvedEntries) {
      switch (entry.resolutionKind()) {
        case GAP_REPAIRED:
            repairedGaps++;
            break;
        case OVERLAP_RESOLVED:
            resolvedOverlaps++;
            break;
        case AMBIGUOUS_DROPPED:
            droppedAmbiguous++;
            droppedAmbiguousLines.add(entry.source().requestId());
            continue;
        case OK:
            break;
        }
      

      GeoInfo geoInfo;
      try {
        geoInfo = lookup.lookup(entry.source().clientIp());
      } catch (Exception e) {
        geolookupFailures++;
        continue;
      }

      byCountry.merge(geoInfo.countryCode(), 1L, Long::sum);
      byTimezone.merge(geoInfo.zoneId().getId(), 1L, Long::sum);

      ZonedDateTime senderTime = entry.serverTime().withZoneSameInstant(geoInfo.zoneId());
       int hour = senderTime.getHour();
      globalHourky[hour]++;
      hourlyByTimezone.computeIfAbsent(geoInfo.zoneId().getId(), z -> new long[24])[hour]++;
    }

    return new AnalysisReport(
      invalidLines,
      repairedGaps,
      resolvedOverlaps,
      droppedAmbiguous,
      geolookupFailures,
      droppedAmbiguousLines,
      byCountry,
      byTimezone,
      globalHourky,
      hourlyByTimezone
    );    
  }
}
