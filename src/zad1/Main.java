/**
 *
 *  @author Koc Paweł s34754
 *
 */

package zad1;


public class Main {
  public static void main(String[] args) throws Exception {
    String fileName = "GeoLogOptions.yaml";
    GeoTimeOptions options = new OptionsLoader().load(fileName);

    GeoLookup lookup = new IpWhoIsGeoLookup();
    AnalyticsService analyticsService = new AnalyticsService(
        new LogParser(),
        new TimestampRepairService()
    );

    AnalysisReport report = analyticsService.analyze(options, lookup);
    System.out.print(report.toText());

    report.requestsBySenderTimezoneId().keySet().stream()
        .sorted()
        .forEach(zoneId -> {
          System.out.println();
          System.out.println(report.timezoneHistogram(zoneId));
        });
  }
}
