/**
 *
 *  @author Koc Paweł s34754
 *
 */

package zad1;


import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.List;

public class TimestampRepairService {

    public List<ResolvedLogEntry> repair(List<LogEntry> entries, ZoneId serverZone) {
        List<ResolvedLogEntry> result = new ArrayList<>();
        List<LogEntry> overlapBlock = new ArrayList<>();
        ZoneRules rules = serverZone.getRules();

        for (LogEntry entry : entries) {
            LocalDateTime ldt = entry.serverLocalTime();
            ZoneOffsetTransition trans = rules.getTransition(ldt);

            if (trans != null && trans.isOverlap()) {
                overlapBlock.add(entry);
            } else {
                if (!overlapBlock.isEmpty()) {
                    resolveOverlapBlock(overlapBlock, rules, serverZone, result);
                    overlapBlock.clear();
                }

                if (trans != null && trans.isGap()) {
                    LocalDateTime shifted = ldt.plus(trans.getDuration());
                    result.add(new ResolvedLogEntry(entry, shifted.atZone(serverZone), ResolutionKind.GAP_REPAIRED));
                } else {
                    result.add(new ResolvedLogEntry(entry, ldt.atZone(serverZone), ResolutionKind.OK));
                }
            }
        }

        if (!overlapBlock.isEmpty()) {
            resolveOverlapBlock(overlapBlock, rules, serverZone, result);
            
        }

        return result;
    }

    private void resolveOverlapBlock(List<LogEntry> block, ZoneRules rules, ZoneId zone, List<ResolvedLogEntry> result) {
        int dropCount = 0;
        int dropIndex = -1;

        for (int i = 1; i < block.size(); i++) {
            if (block.get(i).serverLocalTime().isBefore(block.get(i - 1).serverLocalTime())) {
                dropCount++;
                dropIndex = i;
            }
        }

        ZoneOffsetTransition trans = rules.getTransition(block.get(0).serverLocalTime());

        if (dropCount == 1) {
            for (int i = 0; i < block.size(); i++) {
                LocalDateTime ldt = block.get(i).serverLocalTime();
                if (i < dropIndex) {
                    result.add(new ResolvedLogEntry(block.get(i), ZonedDateTime.ofStrict(ldt, trans.getOffsetBefore(), zone), ResolutionKind.OVERLAP_RESOLVED));
                } else {
                    result.add(new ResolvedLogEntry(block.get(i), ZonedDateTime.ofStrict(ldt, trans.getOffsetAfter(), zone), ResolutionKind.OVERLAP_RESOLVED));
                }
            }
        } else {
            for (LogEntry entry : block) {
                result.add(new ResolvedLogEntry(entry, entry.serverLocalTime().atZone(zone), ResolutionKind.AMBIGUOUS_DROPPED));
            }
        }
    }
}
