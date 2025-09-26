import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DBHelper {
    private static final String URL = "jdbc:mysql://localhost:3306/taskmanager";
    private static final String USER = "root";   // change if needed
    private static final String PASS = "";       // default XAMPP MySQL has no password

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // load MySQL driver
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveTask(Task task) {
        String sql = "INSERT INTO tasks (description, start_time, end_time) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, task.getDescription());
            ps.setTimestamp(2, Timestamp.valueOf(task.getStartTime()));
            ps.setTimestamp(3, Timestamp.valueOf(task.getEndTime()));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<Task> getTasksByDate(LocalDate date) {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks WHERE DATE(start_time) = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(date));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String desc = rs.getString("description");
                LocalDateTime start = rs.getTimestamp("start_time").toLocalDateTime();
                LocalDateTime end = rs.getTimestamp("end_time").toLocalDateTime();
                tasks.add(new Task(desc, start, end));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tasks;
    }

    public static void deleteTask(Task task) {
        String sql = "DELETE FROM tasks WHERE description=? AND start_time=? AND end_time=?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, task.getDescription());
            ps.setTimestamp(2, Timestamp.valueOf(task.getStartTime()));
            ps.setTimestamp(3, Timestamp.valueOf(task.getEndTime()));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
