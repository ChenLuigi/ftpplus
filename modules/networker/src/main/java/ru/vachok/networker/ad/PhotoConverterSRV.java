package ru.vachok.networker.ad;


import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import ru.vachok.mysqlandprops.RegRuMysql;
import ru.vachok.networker.ConstantsFor;
import ru.vachok.networker.componentsrepo.AppComponents;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;


/**
 <h1>Создаёт команды для MS Power Shell, чтобы добавить фото пользователей</h1>

 @since 21.08.2018 (15:57) */
@Service("photoConverter")
public class PhotoConverterSRV {

    /**
     {@link Logger}
     */
    private static final Logger LOGGER = AppComponents.getLogger();

    private static final Properties PROPERTIES = ConstantsFor.PROPS;

    /**
     Путь до папки с фото.
     */
    private String adPhotosPath = PROPERTIES.getProperty("adPhotosPath");

    /**
     Файл-фото
     */
    private File adFotoFile;

    private List<String> psCommands = new ArrayList<>();

    /**
     <b>Преобразование в JPG</b>
     Подготавливает фотографии для импорта в ActiveDirectory. Преобразует любой понимаемый {@link BufferedImage} формат в jpg.
     */
    private BiConsumer<String, BufferedImage> imageBiConsumer = (x, y) -> {
        File outFile = new File("\\\\srv-mail3\\c$\\newmailboxes\\foto\\" + x + ".jpg");
        String fName = "jpg";
        Set<String> samAccountNames = samAccFromDB();
        for (String sam : samAccountNames) {
            if (sam.toLowerCase().contains(x)) x = sam;
        }
        try {
            BufferedImage bufferedImage = new BufferedImage(y.getWidth(), y.getHeight(), BufferedImage.TYPE_INT_RGB);
            bufferedImage.createGraphics().drawImage(y, 0, 0, Color.WHITE, null);
            ImageIO.write(bufferedImage, fName, outFile);
            String msg = outFile.getAbsolutePath() + " written";
            LOGGER.info(msg);
            msg = "Import-RecipientDataProperty -Identity " +
                x +
                " -Picture -FileData ([Byte[]] $(Get-Content -Path “C:\\newmailboxes\\foto\\" +
                outFile.getName().split("\\Q.\\E")[0] +
                ".jpg” -Encoding Byte -ReadCount 0))";
            LOGGER.warn(msg);
            psCommands.add(msg);
        } catch (IOException e) {
            AppComponents.getLogger().error(e.getMessage(), e);
        }
    };

    public File getAdFotoFile() {
        return adFotoFile;
    }

    public void setAdFotoFile(File adFotoFile) {
        this.adFotoFile = adFotoFile;
    }

    public String getAdPhotosPath() {
        return adPhotosPath;
    }

    public void setAdPhotosPath(String adPhotosPath) {
        this.adPhotosPath = adPhotosPath;
    }


    String psCommands() throws NullPointerException {
        try {
            convertFoto();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (String s : psCommands) {
            stringBuilder.append(s);
            stringBuilder.append("<br>");
        }
        return stringBuilder.toString();
    }

    private void convertFoto() throws NullPointerException, IOException {
        File photosDirectory = new File(this.adPhotosPath);
        File[] fotoFiles = photosDirectory.listFiles();
        Map<String, BufferedImage> filesList = new HashMap<>();
        if (fotoFiles != null) {
            for (File f : fotoFiles) {
                String key = f.getName().split("\\Q.\\E")[0];
                for (String format : ImageIO.getWriterFormatNames()) {
                    if (f.getName().toLowerCase().contains(format)) filesList.put(key, ImageIO.read(f));
                }
            }
        } else filesList.put("No files", null);
        filesList.forEach(imageBiConsumer);
    }

    private Set<String> samAccFromDB() {
        Boolean call = new DataBaseADUsers().call();
        LOGGER.warn(call + " db create");
        Set<String> samAccounts = new HashSet<>();
        Connection c = new RegRuMysql().getDefaultConnection(ConstantsFor.APP_NAME);
        try (PreparedStatement p = c.prepareStatement("select * from u0466446_velkom.adusers");
             ResultSet r = p.executeQuery()) {
            while (r.next()) {
                samAccounts.add(r.getString("samAccountName"));
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return samAccounts;
    }
}
