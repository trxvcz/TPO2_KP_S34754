/**
 *
 *  @author Koc Paweł s34754
 *
 */

package zad1;


import java.util.List;

public record GeoTimeOptions(
    String serverZoneId,
    List<String> logLines
) {}
