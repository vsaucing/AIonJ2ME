import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import java.io.*;

public class GeminiChatApp extends MIDlet implements CommandListener {
    private Display display;
    private Form introForm;
    private Form mainForm;
    private TextField inputField;
    private TextField copiedField;
    private StringItem responseItem;
    private Command startCommand;
    private Command sendCommand;
    private Command clearCommand;
    private Command copyCommand;

    public GeminiChatApp() {
        display = Display.getDisplay(this);

        // Intro form
        introForm = new Form("Welcome to Gemini Chat");
        Image geminiIcon = null;
        try {
            geminiIcon = Image.createImage("/gemini.png");
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (geminiIcon != null) {
            ImageItem geminiIconItem = new ImageItem(null, geminiIcon, ImageItem.LAYOUT_DEFAULT, "Gemini Icon");
            introForm.append(geminiIconItem);
        }
        StringItem introText = new StringItem("", "AI Chat for J2ME mobile.");
        startCommand = new Command("Start", Command.OK, 1);
        introForm.append(introText);
        introForm.addCommand(startCommand);
        introForm.setCommandListener(this);

        // Main form for chat interface
        mainForm = new Form("Gemini Chat");
        inputField = new TextField("Message:", "", 256, TextField.ANY);
        copiedField = new TextField("Copied Text:", "", 5000, TextField.ANY);
        responseItem = new StringItem("Response:", "");

        sendCommand = new Command("Send", Command.OK, 1);
        clearCommand = new Command("Clear", Command.SCREEN, 2);
        copyCommand = new Command("Copy", Command.SCREEN, 3);

        mainForm.append(inputField);
        mainForm.append(responseItem);
        mainForm.addCommand(sendCommand); // Left key and middle button
        mainForm.addCommand(copyCommand); // Right key initially

        mainForm.setCommandListener(this);
    }

    protected void startApp() {
        display.setCurrent(introForm);
    }

    protected void pauseApp() {}

    protected void destroyApp(boolean unconditional) {}

    public void commandAction(Command cmd, Displayable disp) {
        if (cmd == startCommand) {
            display.setCurrent(mainForm);
        } else if (cmd == sendCommand) {
            final String userText = inputField.getString().trim();
            responseItem.setText("Wait for result...");
            new Thread(new Runnable() {
                public void run() {
                    String response = generateContentWithGemini(userText);
                    responseItem.setText(response);
                    // Show copy button and hide clear button if response is available
                    mainForm.removeCommand(clearCommand);
                    mainForm.addCommand(copyCommand);
                }
            }).start();
            inputField.setString("");
        } else if (cmd == clearCommand) {
            inputField.setString("");
            responseItem.setText("");
            copiedField.setString("");
            if (mainForm.size() > 1) {
                mainForm.delete(0); // Remove copied field if it exists
            }
            if (mainForm.size() == 1) {
                mainForm.insert(0, inputField); // Add input field if it was hidden
            }
            // Show copy button and hide clear button
            mainForm.removeCommand(clearCommand);
            mainForm.addCommand(copyCommand);
            // Show send button
            mainForm.addCommand(sendCommand);
        } else if (cmd == copyCommand) {
            copyToMainTextBox(responseItem.getText());
            // Show clear button and hide copy button after copying
            mainForm.removeCommand(copyCommand);
            mainForm.addCommand(clearCommand);
            // Hide send button
            mainForm.removeCommand(sendCommand);
        }
    }

    private String generateContentWithGemini(String userText) {
        HttpConnection connection = null;
        OutputStream os = null;
        InputStream is = null;
        StringBuffer response = new StringBuffer();

        try {
            connection = (HttpConnection) Connector.open("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=AIzaSyBF4mboDjI5RifeKn-lrYXLChWnVt8x-nE");
            connection.setRequestMethod(HttpConnection.POST);
            connection.setRequestProperty("Content-Type", "application/json");

            String inputJson = "{\"contents\": [{\"parts\": [{\"text\": \"" + userText + "\"}]}]}";
            os = connection.openOutputStream();
            os.write(inputJson.getBytes());
            os.flush();

            is = connection.openInputStream();
            int ch;
            while ((ch = is.read()) != -1) {
                response.append((char) ch);
            }

            String rawResponse = response.toString();
            int startIndex = rawResponse.indexOf("\"text\":") + 8; // Adjusted index to skip past "text\":"
            int endIndex = rawResponse.indexOf("\"role\":", startIndex) - 2; // Adjusted index to skip past "
            if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                String result = rawResponse.substring(startIndex, endIndex);

                // Replace \\n with an actual newline
                StringBuffer cleanedResult = new StringBuffer();
                for (int i = 0; i < result.length(); i++) {
                    if (result.charAt(i) == '\\' && i + 1 < result.length() && result.charAt(i + 1) == 'n') {
                        cleanedResult.append('\n');
                        i++; // Skip 'n' character
                    } else {
                        cleanedResult.append(result.charAt(i));
                    }
                }
                return cleanedResult.toString();
            }
            return "Error: Unable to find the required text parts.";

        } catch (IOException e) {
            e.printStackTrace();
            return "Error connecting to Gemini API: " + e.getMessage();
        } finally {
            try {
                if (is != null) is.close();
                if (os != null) os.close();
                if (connection != null) connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void copyToMainTextBox(String text) {
        copiedField.setString(text);
        if (mainForm.size() > 1) {
            mainForm.delete(0); // Remove input field if exists
            mainForm.insert(0, copiedField); // Add copied field
            // Hide send button when copying
            mainForm.removeCommand(sendCommand);
        }
    }
}
