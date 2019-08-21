// Copyright (c) all rights. http://networker.vachok.ru 2019.

package ru.vachok.networker;


import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.vachok.networker.componentsrepo.ArgsReader;
import ru.vachok.networker.enums.PropertiesNames;
import ru.vachok.networker.exe.ThreadConfig;
import ru.vachok.networker.fileworks.FileSystemWorker;
import ru.vachok.networker.restapi.MessageToUser;
import ru.vachok.networker.restapi.message.MessageLocal;
import ru.vachok.networker.systray.SystemTrayHelper;

import java.awt.*;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;


/**
 @see ru.vachok.networker.IntoApplicationTest */
@SuppressWarnings("AccessStaticViaInstance")
@SpringBootApplication
@EnableScheduling
@EnableAutoConfiguration
public class IntoApplication {
    
    
    /**
     {@link MessageLocal}
     */
    private static final MessageToUser MESSAGE_LOCAL = MessageToUser.getInstance(MessageToUser.DB, IntoApplication.class.getSimpleName());
    
    private static final boolean IS_TRAY_SUPPORTED = SystemTray.isSupported();
    
    protected static Properties localCopyProperties = AppComponents.getProps();
    
    private static ConfigurableApplicationContext configurableApplicationContext = new SpringApplication().run(IntoApplication.class);
    
    @Contract(pure = true)
    public static ConfigurableApplicationContext getConfigurableApplicationContext() {
        ThreadConfig.dumpToFile("IntoApplication.getConfigurableApplicationContext");
        return configurableApplicationContext;
    }
    
    public static @NotNull String reloadConfigurableApplicationContext() {
        String killAssStr = AppComponents.threadConfig().killAll();
        MESSAGE_LOCAL.warn(killAssStr);
        if (configurableApplicationContext != null && configurableApplicationContext.isActive()) {
            configurableApplicationContext.stop();
            configurableApplicationContext.close();
        }
        try {
            configurableApplicationContext = SpringApplication.run(IntoApplication.class);
        }
        catch (ApplicationContextException e) {
            MESSAGE_LOCAL.error(FileSystemWorker.error(IntoApplication.class.getSimpleName() + ".reloadConfigurableApplicationContext", e));
        }
        return killAssStr;
    }
    
    public static void main(@NotNull String[] args) {
        if (!Arrays.toString(args).contains("test")) {
            UsefulUtilities.startTelnet();
        }
        if (configurableApplicationContext == null) {
            try {
                configurableApplicationContext = new SpringApplication().run(IntoApplication.class);
            }
            catch (RuntimeException e) {
                MESSAGE_LOCAL.error(MessageFormat.format("IntoApplication.main: {0}, ({1})", e.getMessage(), e.getClass().getName()));
            }
        }
        if (args.length > 0) {
            new ArgsReader(configurableApplicationContext, args).run();
        }
        else {
            startContext();
        }
    }
    
    public static void closeContext() {
        configurableApplicationContext.stop();
        configurableApplicationContext.close();
        if (configurableApplicationContext.isActive()) {
            configurableApplicationContext.isRunning();
        }
        AppComponents.threadConfig().killAll();
    }
    
    @Override
    public String toString() {
        return new StringJoiner(",\n", IntoApplication.class.getSimpleName() + "[\n", "\n]")
                .toString();
    }
    
    protected static void afterSt() {
        @NotNull Runnable infoAndSched = new AppInfoOnLoad();
        Executors.unconfigurableExecutorService(Executors.newSingleThreadExecutor()).execute(infoAndSched);
    }
    
    private static void startContext() {
        beforeSt();
        try {
            configurableApplicationContext.start();
        }
        catch (RuntimeException e) {
            MESSAGE_LOCAL.error(MessageFormat.format("IntoApplication.startContext threw away: {0}, ({1}).\n\n{2}",
                    e.getMessage(), e.getClass().getName(), new TForms().fromArray(e)));
        }
        if (!configurableApplicationContext.isRunning() & !configurableApplicationContext.isActive()) {
            throw new RejectedExecutionException(configurableApplicationContext.toString());
        }
        else {
            afterSt();
        }
    }
    
    protected static void beforeSt() {
        @NotNull StringBuilder stringBuilder = new StringBuilder();
        checkTray();
        stringBuilder.append(UsefulUtilities.ipFlushDNS());
        stringBuilder.append(LocalDate.now().getDayOfWeek().getValue()).append(" - day of week\n");
        stringBuilder.append(LocalDate.now().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault())).append("\n\n");
        stringBuilder.append("Current default encoding = ").append(System.getProperty(PropertiesNames.PR_ENCODING)).append("\n");
        System.setProperty(PropertiesNames.PR_ENCODING, "UTF8");
        stringBuilder.append(new TForms().fromArray(System.getProperties()));
        FileSystemWorker.writeFile("system", stringBuilder.toString());
    }
    
    private static void checkTray() {
        Optional optionalTray = SystemTrayHelper.getI();
        try {
            if (IS_TRAY_SUPPORTED && optionalTray.isPresent()) {
                ((SystemTrayHelper) optionalTray.get()).trayAdd();
            }
        }
        catch (HeadlessException e) {
            MESSAGE_LOCAL.error(MessageFormat
                    .format("IntoApplication.checkTray {0} - {1}\nStack:\n{2}", e.getClass().getTypeName(), e.getMessage(), new TForms().fromArray(e)));
        }
    }
}