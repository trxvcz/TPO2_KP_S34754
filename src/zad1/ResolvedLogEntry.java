/**
 *
 *  @author Koc Paweł s34754
 *
 */

package zad1;


import java.time.ZonedDateTime;

public record ResolvedLogEntry(
    LogEntry source,
    ZonedDateTime serverTime,
    ResolutionKind resolutionKind
) {}
