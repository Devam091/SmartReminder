import java.time.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses natural language tasks:
 *  - "Wake up at 6 AM"
 *  - "Meeting Jon for coffee at 5 PM for 1 hour"
 *  - "6 PM to 7 PM games"
 *  - "Call mom at 3.30 pm for 45 minutes"
 *
 * Rules:
 *  - If only start time provided → default 30 minutes (except keywords like "sleep")
 *  - "Sleep" defaults to 7 hours if no duration
 *  - Tasks without time → description-only
 */
public class TaskParser {

    // Patterns (support both ":" and ".")
    private static final Pattern RANGE =
            Pattern.compile("(?i)(\\d{1,2})([:.](\\d{2}))?\\s*(am|pm)\\s*(to|-)\\s*(\\d{1,2})([:.](\\d{2}))?\\s*(am|pm)");
    private static final Pattern AT_TIME =
            Pattern.compile("(?i)\\bat\\s*(\\d{1,2})([:.](\\d{2}))?\\s*(am|pm)\\b");
    private static final Pattern FOR_DURATION =
            Pattern.compile("(?i)\\bfor\\s*(\\d{1,3})\\s*(min|mins|minute|minutes|hr|hrs|hour|hours)\\b");

    public static Task parse(String input, LocalDate day) {
        String desc = input.trim();

        // Case 1: Explicit time range → "6 PM to 7 PM"
        Matcher mRange = RANGE.matcher(desc);
        if (mRange.find()) {
            LocalTime start = toLocalTime(mRange.group(1), mRange.group(3), mRange.group(4));
            LocalTime end = toLocalTime(mRange.group(6), mRange.group(8), mRange.group(9));

            LocalDateTime sdt = LocalDateTime.of(day, start);
            LocalDateTime edt = LocalDateTime.of(day, end);

            String cleaned = desc.replace(mRange.group(0), "").replaceAll("\\s{2,}", " ").trim();
            if (cleaned.isEmpty()) cleaned = "Task";

            return new Task(cleaned, sdt, edt);
        }

        // Case 2: "at <time>" + optional "for <duration>"
        Matcher mAt = AT_TIME.matcher(desc);
        if (mAt.find()) {
            LocalTime start = toLocalTime(mAt.group(1), mAt.group(3), mAt.group(4));
            LocalDateTime sdt = LocalDateTime.of(day, start);

            long minutes = 30; // default duration
            Matcher mDur = FOR_DURATION.matcher(desc);
            if (mDur.find()) {
                int qty = Integer.parseInt(mDur.group(1));
                String unit = mDur.group(2).toLowerCase();
                if (unit.startsWith("min")) minutes = qty;
                else minutes = qty * 60L;
            } else if (desc.toLowerCase().contains("sleep")) {
                minutes = 7 * 60; // default 7 hours sleep
            }
            LocalDateTime edt = sdt.plusMinutes(minutes);

            String cleaned = desc
                    .replace(mAt.group(0), "")
                    .replaceAll(FOR_DURATION.pattern(), "")
                    .replaceAll("\\s{2,}", " ")
                    .trim();
            if (cleaned.isEmpty()) cleaned = "Task";

            return new Task(cleaned, sdt, edt);
        }

        // Case 3: No explicit time → timeless task
        return new Task(desc, null, null);
    }

    /** Lenient parse: safe fallback */
    public static Task tryParseLenient(String input, LocalDate day) {
        try {
            return parse(input, day);
        } catch (Exception ex) {
            return null;
        }
    }

    // Helper to parse time parts
    private static LocalTime toLocalTime(String hh, String mm, String ampm) {
        int h = Integer.parseInt(hh);
        int m = (mm != null && !mm.isEmpty()) ? Integer.parseInt(mm) : 0;

        if (ampm.equalsIgnoreCase("am")) {
            if (h == 12) h = 0;
        } else if (ampm.equalsIgnoreCase("pm")) {
            if (h != 12) h += 12;
        }

        return LocalTime.of(h % 24, m);
    }
}
