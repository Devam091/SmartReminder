import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;


public class TaskApp {

    private final ObservableList<Task> tasks = FXCollections.observableArrayList();
    private final Label statusLabel = new Label("Welcome! Add your first task.");
    private final Label suggestionLabel = new Label("💡 Suggestions will appear here…");
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, yyyy | hh:mm a");
    private ListView<Task> taskListView;

    private final TelegramNotifier notifier = new TelegramNotifier();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ConcurrentHashMap<Task, ScheduledFuture<?>> reminders = new ConcurrentHashMap<>();

    private Connection conn;

    private double xOffset = 0;
    private double yOffset = 0;

    public TaskApp(Stage stage) {
        connectDB();
        loadTasksFromDB();

        Label title = new Label("Smart Reminder System");
        title.getStyleClass().add("title");

        TextField input = new TextField();
        input.setPromptText("e.g., Meeting Jon for coffee at 5 PM for 1 hour");
        input.getStyleClass().add("input");

        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.getStyleClass().add("date-picker");

        Button addBtn = new Button("Add Task");
        addBtn.getStyleClass().add("btn-primary");

        HBox inputBar = new HBox(10, datePicker, input, addBtn);
        inputBar.setPadding(new Insets(10));
        HBox.setHgrow(input, Priority.ALWAYS);

        suggestionLabel.getStyleClass().add("suggestion");

        taskListView = new ListView<>();
        taskListView.setItems(tasks);
        taskListView.setCellFactory(lv -> new ListCell<Task>() {
            @Override
            protected void updateItem(Task item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    String text;
                    if (item.hasTime()) {
                        text = item.getDescription() + " (" +
                                item.getStartTime().format(fmt) + " - " +
                                item.getEndTime().format(fmt) + ")";
                    } else {
                        text = item.getDescription();
                    }

                    Label lbl = new Label(text);
                    Button del = new Button("❌");
                    del.getStyleClass().add("btn-delete");
                    del.setOnAction(e -> {
                        tasks.remove(item);
                        deleteTaskFromDB(item);
                        statusLabel.setText("🗑 Removed: " + item.getDescription());
                        updateLiveSuggestions(input.getText());

                        ScheduledFuture<?> future = reminders.remove(item);
                        if (future != null) {
                            future.cancel(false);
                            System.out.println("🛑 Reminder cancelled for: " + item.getDescription());
                        }
                    });
                    HBox row = new HBox(12, lbl, del);
                    row.getStyleClass().add("task-row");
                    setGraphic(row);
                }
            }
        });

        input.textProperty().addListener((obs, oldV, newV) -> updateLiveSuggestions(newV));

        // ✅ FIXED: Add button now only saves task (no bot.py call)
        addBtn.setOnAction(e -> {
            String raw = input.getText().trim();
            LocalDate chosenDate = datePicker.getValue();

            if (raw.isEmpty()) {
                statusLabel.setText("⚠ Please type a task.");
                return;
            }

            Task parsed = TaskParser.parse(raw, chosenDate);
            if (parsed == null) {
                statusLabel.setText("❌ Could not understand the task.");
                return;
            }

            tasks.add(parsed);
            saveTaskToDB(parsed);

            String msg;
            if (parsed.hasTime()) {
                msg = "✅ Added: " + parsed.getDescription() + " (" +
                        parsed.getStartTime().format(fmt) + " - " +
                        parsed.getEndTime().format(fmt) + ")";
            } else {
                msg = "✅ Added: " + parsed.getDescription();
            }

            statusLabel.setText(msg);
            suggestionLabel.setText("✅ Task added. Start typing a new one…");
            input.clear();

            try {
                notifier.sendMessage("📌 New Task Added:\n" + msg);
            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println("⚠ Failed to send Telegram notification");
            }

            if (parsed.hasTime()) {
                long delay = java.time.Duration.between(
                        LocalDateTime.now(),
                        parsed.getStartTime().minusMinutes(10)
                ).toMillis();

                if (delay > 0) {
                    ScheduledFuture<?> future = scheduler.schedule(() -> {
                        notifier.sendMessage("⏰ Reminder: Your task \"" +
                                parsed.getDescription() + "\" starts at " +
                                parsed.getStartTime().format(fmt));
                    }, delay, TimeUnit.MILLISECONDS);

                    reminders.put(parsed, future);
                }
            }
        });

        // ================== ASK AI SECTION ==================
        Label askAILabel = new Label("Ask AI:");
        TextField askAIField = new TextField();
        askAIField.setPromptText("e.g., Am I free at 4 PM? or Improve my schedule");
        Button askAIButton = new Button("Ask");
        TextArea aiOutput = new TextArea();
        aiOutput.setEditable(false);
        aiOutput.setPrefHeight(80);

        HBox askAIBar = new HBox(10, askAILabel, askAIField, askAIButton);
        askAIBar.setPadding(new Insets(10));
        HBox.setHgrow(askAIField, Priority.ALWAYS);

        askAIButton.setOnAction(e -> {
            String question = askAIField.getText().toLowerCase().trim();
            if (question.isEmpty()) {
                aiOutput.setText("⚠ Please type a question.");
                return;
            }

            aiOutput.setText("🤖 Thinking...");

            CompletableFuture.runAsync(() -> {
                try {
                    String pythonExe = "C:\\Users\\DEVAM\\IdeaProjects\\AI Task Manager\\.venv\\Scripts\\python.exe";
                    String botScript = "C:\\Users\\DEVAM\\IdeaProjects\\AI Task Manager\\src\\bot.py";

                    ProcessBuilder pb = new ProcessBuilder(pythonExe, botScript, question);
                    pb.redirectErrorStream(true);
                    pb.directory(new java.io.File("C:\\Users\\DEVAM\\IdeaProjects\\AI Task Manager"));


                    Process process = pb.start();

                    try (java.io.BufferedReader reader =
                                 new java.io.BufferedReader(new java.io.InputStreamReader(
                                         process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {

                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line).append('\n');
                        }

                        int exitCode = process.waitFor();
                        String finalResponse;
                        if (exitCode == 0 && response.length() > 0) {
                            finalResponse = "🤖 " + response.toString().trim();
                        } else {
                            finalResponse = "⚠ Python bot returned no response.\n"
                                    + "[DEBUG exitCode=" + exitCode + ", output=" + response + "]";
                        }

                        String toShow = finalResponse;
                        Platform.runLater(() -> aiOutput.setText(toShow));
                    }


                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> aiOutput.setText("⚠ Failed to run AI bot: " + ex.getMessage()));
                }
            });
        });
        // ================== /ASK AI SECTION ==================

        Button closeBtn = new Button("✖");
        Button minBtn = new Button("—");
        Button maxBtn = new Button("⬜");

        closeBtn.getStyleClass().addAll("window-button", "close");
        minBtn.getStyleClass().addAll("window-button", "min");
        maxBtn.getStyleClass().addAll("window-button", "max");

        closeBtn.setOnAction(e -> stage.close());
        minBtn.setOnAction(e -> stage.setIconified(true));
        maxBtn.setOnAction(e -> stage.setMaximized(!stage.isMaximized()));

        HBox windowControls = new HBox(8, minBtn, maxBtn, closeBtn);
        windowControls.getStyleClass().add("window-controls");
        windowControls.setPadding(new Insets(5));

        VBox content = new VBox(12, title, inputBar, suggestionLabel, taskListView, statusLabel, askAIBar, aiOutput);
        content.setPadding(new Insets(16));
        content.getStyleClass().add("root-wrap");

        BorderPane root = new BorderPane();
        root.setTop(windowControls);
        root.setCenter(content);

        Scene scene = new Scene(root, 1000, 700);
        scene.getStylesheets().add("style.css");

        stage.setTitle("Smart Reminder");
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(javafx.scene.input.KeyCombination.NO_MATCH);

        windowControls.setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });
        windowControls.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - xOffset);
            stage.setY(e.getScreenY() - yOffset);
        });

        stage.show();
    }

    private void connectDB() {
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/taskmanager", "root", "");
            System.out.println("✅ Connected to DB");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadTasksFromDB() {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM tasks");
            while (rs.next()) {
                LocalDateTime start = (rs.getTimestamp("start_time") != null)
                        ? rs.getTimestamp("start_time").toLocalDateTime()
                        : null;

                LocalDateTime end = (rs.getTimestamp("end_time") != null)
                        ? rs.getTimestamp("end_time").toLocalDateTime()
                        : null;

                Task t = new Task(
                        rs.getString("description"),
                        start,
                        end
                );

                if (!t.hasTime() || !end.isBefore(LocalDateTime.now())) {
                    tasks.add(t);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveTaskToDB(Task task) {
        try {
            String sql = "INSERT INTO tasks (description, start_time, end_time) VALUES (?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, task.getDescription());

            if (task.hasTime()) {
                ps.setTimestamp(2, Timestamp.valueOf(task.getStartTime()));
                ps.setTimestamp(3, Timestamp.valueOf(task.getEndTime()));
            } else {
                ps.setNull(2, java.sql.Types.TIMESTAMP);
                ps.setNull(3, java.sql.Types.TIMESTAMP);
            }

            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteTaskFromDB(Task task) {
        try {
            String sql = "DELETE FROM tasks WHERE description=? AND start_time=? AND end_time=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, task.getDescription());

            if (task.hasTime()) {
                ps.setTimestamp(2, Timestamp.valueOf(task.getStartTime()));
                ps.setTimestamp(3, Timestamp.valueOf(task.getEndTime()));
            } else {
                ps.setNull(2, java.sql.Types.TIMESTAMP);
                ps.setNull(3, java.sql.Types.TIMESTAMP);
            }

            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateLiveSuggestions(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            suggestionLabel.setText("💡 Suggestions will appear here…");
            return;
        }

        // Build lowercase list of existing task descriptions
        List<String> existing = new ArrayList<>();
        for (Task t : tasks) {
            if (t != null && t.getDescription() != null) {
                existing.add(t.getDescription().toLowerCase());
            }
        }

        List<String> suggestions = SuggestionEngine.suggest(rawText, existing);
        if (suggestions == null || suggestions.isEmpty()) {
            suggestionLabel.setText("⚠ No suggestions.");
        } else {
            suggestionLabel.setText(String.join("\n", suggestions));
        }
    }


}
