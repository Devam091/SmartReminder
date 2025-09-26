import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Task {
    private final String description;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final boolean important;  // already there
    private final boolean hasTime;    // ✅ flag for whether user gave time

    // Default: hasTime = true, important = false
    public Task(String description, LocalDateTime startTime, LocalDateTime endTime) {
        this(description, startTime, endTime, false, startTime != null && endTime != null);
    }

    // Constructor with important flag
    public Task(String description, LocalDateTime startTime, LocalDateTime endTime, boolean important) {
        this(description, startTime, endTime, important, startTime != null && endTime != null);
    }

    // Full constructor
    public Task(String description, LocalDateTime startTime, LocalDateTime endTime, boolean important, boolean hasTime) {
        this.description = description.trim();
        this.startTime = startTime;
        this.endTime = endTime;
        this.important = important;
        this.hasTime = hasTime;
    }

    public String getDescription() { return description; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public boolean isImportant() { return important; }
    public boolean hasTime() { return hasTime; }

    public boolean overlapsWith(Task other) {
        return this.startTime != null && this.endTime != null &&
                other.startTime != null && other.endTime != null &&
                this.startTime.isBefore(other.endTime) && this.endTime.isAfter(other.startTime);
    }

    public boolean sameActivity(Task other) {
        return this.description.equalsIgnoreCase(other.description);
    }

    @Override
    public String toString() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, yyyy | hh:mm a");
        String base;
        if (hasTime && startTime != null && endTime != null) {
            base = description + " (" + startTime.format(fmt) + " - " + endTime.format(fmt) + ")";
        } else {
            base = description; // ⬅ timeless task only shows text
        }
        return important ? "[IMPORTANT] " + base : base;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task)) return false;
        Task task = (Task) o;
        return important == task.important &&
                hasTime == task.hasTime &&
                Objects.equals(description, task.description) &&
                Objects.equals(startTime, task.startTime) &&
                Objects.equals(endTime, task.endTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, startTime, endTime, important, hasTime);
    }
}
