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
    private StringItem historyItem;
    private void initializeHistory() {
    historyItem = new StringItem("History:", "");
    mainForm.append(historyItem);
}

    // Updated API details is this api for free AI model
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String API_KEY = "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";
    //private static final String MODEL = "google/gemini-2.0-flash-thinking-exp-1219:free";

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

    initializeHistory(); // Call the method to initialize the history

    mainForm.setCommandListener(this);
}
    
private void updateHistory(String userText, String response) {
String currentHistory = historyItem.getText();
String newEntry = "User: " + userText + "\nResponse: " + response + "\n" ; // Removed the initial newline \n
historyItem.setText(newEntry + currentHistory);
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
                responseItem.setText("Response: " + response + "\r\n");
                // Update history
                updateHistory(userText, response);
                // Show copy button and hide clear button if response is available
                mainForm.removeCommand(clearCommand);
                mainForm.addCommand(copyCommand);
                // Scroll to the first item after response appears
                display.setCurrentItem((Item) mainForm.get(0));
            }
        }).start();
        inputField.setString("");
    } else if (cmd == clearCommand) {
    inputField.setString("");
    responseItem.setText("");
    copiedField.setString("");

    // Ensure inputField is always the first item
    if (mainForm.size() > 0 && mainForm.get(0) != inputField) {
        mainForm.set(0, inputField); // Set inputField as the first item
    } else if (mainForm.size() == 0) {
        mainForm.append(inputField); // Add inputField if the form is empty
    }

    // Show copy button and hide clear button
    mainForm.removeCommand(clearCommand);
    mainForm.addCommand(copyCommand);
    
    // Show send button
    mainForm.addCommand(sendCommand);

    // Set cursor back to inputField after clearing
    display.setCurrentItem(inputField);
    } else if (cmd == copyCommand) {
        copyToMainTextBox(responseItem.getText());
        // Show clear button and hide copy button after copying
        mainForm.removeCommand(copyCommand);
        mainForm.addCommand(clearCommand);
        // Hide send button
        mainForm.removeCommand(sendCommand);
    }
}

 // multiple model for changing each found error in AI result making it almost unlimited token
 private static final String[] MODELS = {
        "google/gemini-2.0-flash-thinking-exp:free",
        "google/gemini-2.0-pro-exp-02-05:free",
        "google/gemini-2.0-flash-lite-preview-02-05:free",
        "google/gemini-2.0-flash-thinking-exp-1219:free",
        "google/gemini-exp-1206:free",
        "google/learnlm-1.5-pro-experimental:free"
    };

private String generateContentWithGemini(String userText) {
    for (int i = 0; i < MODELS.length; i++) {
        String model = MODELS[i];
        String response = tryGenerateContentWithModel(userText, model);
        if (response != null) {
            return response;
        }
    }
    return "Error connecting to OpenRouter API with all models.";
}

private String tryGenerateContentWithModel(String userText, String model) {
    HttpConnection connection = null;
    OutputStream os = null;
    InputStream is = null;
    StringBuffer response = new StringBuffer();

    try {
        connection = (HttpConnection) Connector.open(API_URL);
        connection.setRequestMethod(HttpConnection.POST);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + API_KEY);

        String requestBody = "{\"model\":\"" + model + "\",\"messages\":[{\"role\":\"user\",\"content\":[{\"type\":\"text\",\"text\":\"" + userText + "\"}]}]}";
        os = connection.openOutputStream();
        os.write(requestBody.getBytes());
        os.flush();

        is = connection.openInputStream();
        int ch;
        while ((ch = is.read()) != -1) {
            response.append((char) ch);
        }

        String rawResponse = response.toString();

        // Check for errors in the response
        int errorIndex = rawResponse.indexOf("\"error\":");
        if (errorIndex != -1) {
            return null; // Error detected, retry with next model
        }

        // Manually replace "content" with "AI chat"
        String target = "content";
        String replacement = "AI chat";
        StringBuffer cleanedResponse = new StringBuffer();
        int start = 0;
        int end = 0;
        while ((end = rawResponse.indexOf(target, start)) >= 0) {
            cleanedResponse.append(rawResponse.substring(start, end));
            cleanedResponse.append(replacement);
            start = end + target.length();
        }
        cleanedResponse.append(rawResponse.substring(start));
        rawResponse = cleanedResponse.toString();

        int refusalIndex = rawResponse.indexOf("\"refusal\":");
        if (refusalIndex != -1) {
            rawResponse = rawResponse.substring(0, refusalIndex);
        }

        int modelIndex = rawResponse.indexOf("\"AI chat\":");
        if (modelIndex != -1) {
            rawResponse = rawResponse.substring(modelIndex);
        }

StringBuffer cleanedResult = new StringBuffer();
for (int i = 0; i < rawResponse.length(); i++) {
    if (rawResponse.charAt(i) == '\\' && i + 1 < rawResponse.length() && rawResponse.charAt(i + 1) == 'n') {
        cleanedResult.append('\n');
        i++;
    } else if (rawResponse.charAt(i) == '\\' && i + 1 < rawResponse.length() && rawResponse.charAt(i + 1) == '\"') {
        cleanedResult.append('"');
        i++;
    } else {
        cleanedResult.append(rawResponse.charAt(i));
    }
}

        return cleanedResult.toString();

    } catch (IOException e) {
        e.printStackTrace();
        return null; // Return null to indicate failure
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
