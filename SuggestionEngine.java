import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class SuggestionEngine {
    private static final String[] ROUTINE_ORDER = {
            "Wake up", "Breakfast", "Bath", "Commute", "Work/Study", "Lunch",
            "Short walk", "Tea/Coffee break", "Exercise", "Dinner", "Relax",
            "Read a book", "Plan tomorrow", "Sleep"
    };

    private static final String[] GENERIC_ACTIVITIES = {
            "Read a book", "Power nap", "Go for a walk", "Meditate",
            "Tidy your room", "Listen to a podcast", "Journal", "Light stretching"
    };

    public static List<String> suggest(String rawTask, List<String> existingLowercase) {
        List<String> out = new ArrayList<>();
        if (rawTask == null || rawTask.trim().isEmpty()) {
            out.add("Add a time to your task (e.g., 'at 6 AM' or '3 PM to 4 PM').");
            out.add("You can add duration too (e.g., 'for 45 minutes').");
            return out;
        }

        String desc = rawTask.toLowerCase();

        // 1) After wake up → breakfast
        if (desc.contains("wake") && !containsLower(existingLowercase, "breakfast")) {
            out.add("Consider: Breakfast after wake up.");
            return out;
        }

        // 2) meeting/coffee → Bath, Commute, Prepare notes
        if (desc.contains("meeting") || desc.contains("meet") || desc.contains("coffee")) {
            addIfMissing(out, "Bath", existingLowercase);
            addIfMissing(out, "Commute", existingLowercase);
            addIfMissing(out, "Prepare notes", existingLowercase);
            if (!out.isEmpty()) return out;
        }

        // 3) next routine step
        String next = nextRoutineStep(rawTask, existingLowercase);
        if (next != null) {
            out.add("Next up: " + next);
            return out;
        }

        // 4) fallback: random generic (prefer unused)
        List<String> pool = new ArrayList<>();
        for (String g : GENERIC_ACTIVITIES) {
            if (!containsLower(existingLowercase, g)) pool.add(g);
        }
        if (pool.isEmpty()) pool = Arrays.asList(GENERIC_ACTIVITIES);
        out.add("How about: " + pool.get(new Random().nextInt(pool.size())));
        return out;
    }

    private static String nextRoutineStep(String current, List<String> existingLowercase) {
        int idx = -1;
        for (int i = 0; i < ROUTINE_ORDER.length; i++) {
            if (ROUTINE_ORDER[i].equalsIgnoreCase(current)) {
                idx = i; break;
            }
        }
        if (idx == -1) return null;

        for (int i = idx + 1; i < ROUTINE_ORDER.length; i++) {
            String next = ROUTINE_ORDER[i];
            if (!containsLower(existingLowercase, next)) return next;
        }
        return null;
    }

    private static boolean containsLower(List<String> list, String needle) {
        String n = needle.toLowerCase();
        for (String s : list) {
            if (s != null && s.equals(n)) return true;
        }
        return false;
    }

    private static void addIfMissing(List<String> out, String suggestion, List<String> existingLowercase) {
        if (!containsLower(existingLowercase, suggestion)) out.add(suggestion);
    }
}
