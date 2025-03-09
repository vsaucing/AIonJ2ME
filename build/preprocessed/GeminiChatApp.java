import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;

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
  
  private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
  
  private static final String API_KEY = "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";
  
  private static final String MODEL = "google/gemini-2.0-flash-thinking-exp:free";
  
  private void initializeHistory() {
    this.historyItem = new StringItem("History:", "");
    this.mainForm.append((Item)this.historyItem);
  }
  
  public GeminiChatApp() {
    this.display = Display.getDisplay(this);
    this.introForm = new Form("Welcome to Gemini Chat");
    Image geminiIcon = null;
    try {
      geminiIcon = Image.createImage("/gemini.png");
    } catch (IOException e) {
      e.printStackTrace();
    } 
    if (geminiIcon != null) {
      ImageItem geminiIconItem = new ImageItem(null, geminiIcon, 0, "Gemini Icon");
      this.introForm.append((Item)geminiIconItem);
    } 
    StringItem introText = new StringItem("", "AI Chat for J2ME mobile.");
    this.startCommand = new Command("Start", 4, 1);
    this.introForm.append((Item)introText);
    this.introForm.addCommand(this.startCommand);
    this.introForm.setCommandListener(this);
    this.mainForm = new Form("Gemini Chat");
    this.inputField = new TextField("Message:", "", 256, 0);
    this.copiedField = new TextField("Copied Text:", "", 5000, 0);
    this.responseItem = new StringItem("Response:", "");
    this.sendCommand = new Command("Send", 4, 1);
    this.clearCommand = new Command("Clear", 1, 2);
    this.copyCommand = new Command("Copy", 1, 3);
    this.mainForm.append((Item)this.inputField);
    this.mainForm.append((Item)this.responseItem);
    this.mainForm.addCommand(this.sendCommand);
    this.mainForm.addCommand(this.copyCommand);
    initializeHistory();
    this.mainForm.setCommandListener(this);
  }
  
  private void updateHistory(String userText, String response) {
    String currentHistory = this.historyItem.getText();
    this.historyItem.setText(currentHistory + "\nUser: " + userText + "\nResponse: " + response);
  }
  
  protected void startApp() {
    this.display.setCurrent((Displayable)this.introForm);
  }
  
  protected void pauseApp() {}
  
  protected void destroyApp(boolean unconditional) {}
  
  public void commandAction(Command cmd, Displayable disp) {
    if (cmd == this.startCommand) {
        this.display.setCurrent(this.mainForm);
    } else if (cmd == this.sendCommand) {
        final String userText = this.inputField.getString().trim();
        this.responseItem.setText("Wait for result...");

        // Pass `userText` directly into the Runnable
        Runnable task = new Runnable() {
            private String userInput = userText;

            public void run() {
                String response = generateContentWithGemini(userInput);
                responseItem.setText(response);
                updateHistory(userInput, response);
                mainForm.removeCommand(clearCommand);
                mainForm.addCommand(copyCommand);
                display.setCurrentItem(mainForm.get(0));
            }
        };

        new Thread(task).start();
        this.inputField.setString("");
    } else if (cmd == this.clearCommand) {
        // Clear logic remains unchanged
    } else if (cmd == this.copyCommand) {
        copyToMainTextBox(this.responseItem.getText());
        this.mainForm.removeCommand(this.copyCommand);
        this.mainForm.addCommand(this.clearCommand);
        this.mainForm.removeCommand(this.sendCommand);
    }
}

  
  private String generateContentWithGemini(String userText) {
    HttpConnection connection = null;
    OutputStream os = null;
    InputStream is = null;
    StringBuffer response = new StringBuffer();
    try {
      connection = (HttpConnection)Connector.open("https://openrouter.ai/api/v1/chat/completions");
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Authorization", "Bearer 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");
      String requestBody = "{\"model\":\"google/gemini-2.0-flash-thinking-exp:free\",\"messages\":[{\"role\":\"user\",\"content\":[{\"type\":\"text\",\"text\":\"" + userText + "\"}]}]}";
      os = connection.openOutputStream();
      os.write(requestBody.getBytes());
      os.flush();
      is = connection.openInputStream();
      int ch;
      while ((ch = is.read()) != -1)
        response.append((char)ch); 
      String rawResponse = response.toString();
      int refusalIndex = rawResponse.indexOf("\"refusal\":");
      if (refusalIndex != -1)
        rawResponse = rawResponse.substring(0, refusalIndex); 
      int modelIndex = rawResponse.indexOf("\"content\":");
      if (modelIndex != -1)
        rawResponse = rawResponse.substring(modelIndex); 
      StringBuffer cleanedResult = new StringBuffer();
      for (int i = 0; i < rawResponse.length(); i++) {
        if (rawResponse.charAt(i) == '\\' && i + 1 < rawResponse.length() && rawResponse.charAt(i + 1) == 'n') {
          cleanedResult.append('\n');
          i++;
        } else {
          cleanedResult.append(rawResponse.charAt(i));
        } 
      } 
      return cleanedResult.toString();
    } catch (IOException e) {
      e.printStackTrace();
      return "Error connecting to OpenRouter API: " + e.getMessage();
    } finally {
      try {
        if (is != null)
          is.close(); 
        if (os != null)
          os.close(); 
        if (connection != null)
          connection.close(); 
      } catch (IOException e) {
        e.printStackTrace();
      } 
    } 
  }
  
  private void copyToMainTextBox(String text) {
    this.copiedField.setString(text);
    if (this.mainForm.size() > 1) {
      this.mainForm.delete(0);
      this.mainForm.insert(0, (Item)this.copiedField);
      this.mainForm.removeCommand(this.sendCommand);
    } 
  }
}
