import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class BotIntegration {

    public static String runBot(String input) {
        String result = "";
        try {
            // Path to your Python executable (venv)
            String pythonExe = "C:/Users/DEVAM/IdeaProjects/AI Task Manager/.venv/Scripts/python.exe";

            // Path to your bot.py
            String scriptPath = "C:/Users/DEVAM/IdeaProjects/AI Task Manager/src/bot.py";

            // Run python with input as an argument
            ProcessBuilder pb = new ProcessBuilder(pythonExe, scriptPath, input);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Read Python output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                result += line + "\n";
            }

            process.waitFor();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return result.trim();
    }
}
