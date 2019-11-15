package ru.vachok.networker.restapi.message;


import com.eclipsesource.json.JsonObject;
import org.jetbrains.annotations.Contract;
import ru.vachok.networker.data.enums.ConstantsFor;
import ru.vachok.networker.data.enums.FileNames;
import ru.vachok.networker.data.enums.PropertiesNames;

import java.io.*;
import java.text.MessageFormat;


class MessageFileLocal implements MessageToUser {
    
    
    private static final File appLog = new File(FileNames.APP_JSON);
    
    private static final String WARN = "warn";
    
    private static final String INFO = "info";
    
    private String headerMsg;
    
    private String bodyMsg;
    
    private String titleMsg;
    
    @Override
    public void setHeaderMsg(String headerMsg) {
        this.headerMsg = headerMsg;
    }
    
    @Contract(pure = true)
    MessageFileLocal(String titleMsg) {
        this.titleMsg = titleMsg;
    }
    
    @Override
    public void errorAlert(String headerMsg, String titleMsg, String bodyMsg) {
        this.headerMsg = headerMsg;
        this.bodyMsg = bodyMsg;
        this.titleMsg = titleMsg;
        pringAppLog(ConstantsFor.STR_ERROR);
    }
    
    @Override
    public void info(String headerMsg, String titleMsg, String bodyMsg) {
        this.headerMsg = headerMsg;
        this.titleMsg = titleMsg;
        this.bodyMsg = bodyMsg;
        pringAppLog(INFO);
    }
    
    @Override
    public void infoNoTitles(String bodyMsg) {
        this.bodyMsg = bodyMsg;
        pringAppLog(INFO);
    }
    
    @Override
    public void info(String bodyMsg) {
        this.bodyMsg = bodyMsg;
        pringAppLog(INFO);
    }
    
    @Override
    public void error(String bodyMsg) {
        this.bodyMsg = bodyMsg;
        pringAppLog(ConstantsFor.STR_ERROR);
    }
    
    @Override
    public void error(String headerMsg, String titleMsg, String bodyMsg) {
        this.headerMsg = headerMsg;
        this.titleMsg = titleMsg;
        this.bodyMsg = bodyMsg;
        pringAppLog(ConstantsFor.STR_ERROR);
    }
    
    @Override
    public void warn(String headerMsg, String titleMsg, String bodyMsg) {
        this.headerMsg = headerMsg;
        this.titleMsg = titleMsg;
        this.bodyMsg = bodyMsg;
        pringAppLog(WARN);
    }
    
    @Override
    public void warn(String bodyMsg) {
        this.bodyMsg = bodyMsg;
        pringAppLog(WARN);
    }
    
    @Override
    public void warning(String headerMsg, String titleMsg, String bodyMsg) {
        this.headerMsg = headerMsg;
        this.titleMsg = titleMsg;
        this.bodyMsg = bodyMsg;
        pringAppLog(WARN);
    }
    
    @Override
    public void warning(String bodyMsg) {
        this.bodyMsg = bodyMsg;
        pringAppLog(WARN);
    }
    
    private void pringAppLog(String logType) {
        JsonObject jsonObject = new JsonObject();
        try {
            jsonObject.set(PropertiesNames.TIMESTAMP, System.currentTimeMillis());
            jsonObject.set(logType, this.headerMsg);
            jsonObject.set(titleMsg, bodyMsg);
        }
        catch (RuntimeException e) {
            jsonObject.set("JSON Error", e.getClass().getSimpleName());
        }
        finally {
            try (OutputStream outputStream = new FileOutputStream(appLog, true);
                 PrintStream printStream = new PrintStream(outputStream, true)) {
                printStream.println(jsonObject.toString());
            }
            catch (IOException e) {
                System.err.println(MessageFormat.format("{0}: {1} see line: 124 ***", MessageFileLocal.class.getSimpleName(), e.getMessage()));
            }
        }
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MessageFileLocal{");
        sb.append("titleMsg='").append(titleMsg).append('\'');
        sb.append(", headerMsg='").append(headerMsg).append('\'');
        sb.append(", bodyMsg='").append(bodyMsg).append('\'');
        sb.append('}');
        return sb.toString();
    }
}