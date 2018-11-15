package org.usac.bots.jbot.datasources;

import com.google.gson.Gson;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Repository
@ConfigurationProperties(prefix = "filesystem")
public class FileDataSource extends LogGrootDataSource {

    private String dataFolder;

    private final Gson gson = new Gson();

    @PostConstruct
    public void init() {

        try {
            File f = new File(dataFolder);
            if (!f.exists()) {
                boolean createDirectories = f.mkdirs();
                log.info("Created folder " + dataFolder + " for data storage");
                setEnabled(createDirectories);
            }
            else {
                setEnabled(true);
            }
        }
        catch (Exception e) {
            log.error(e.getMessage());
            setEnabled(false);
        }

    }

    @SuppressWarnings("unchecked")
    public <T> List<T> loadObjects(String owner, T type, Class clazz) {

        List<T> results = new ArrayList<>();


        File[] directoryListing = new File(getDataFolder()).listFiles();
        if (directoryListing != null) {
            for (File file : directoryListing) {
                try {
                    String prefix = clazz.getSimpleName() + "-" + (owner != null ? owner + "-" : "");
                    if (file.getName().startsWith(prefix)) {

                        byte[] encoded = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
                        String json = new String(encoded, StandardCharsets.UTF_8);
                        results.add((T)gson.fromJson(json, type.getClass()));

                    }

                } catch (Exception e) {
                    log.error(e.getMessage());
                }

            }
        }
        return results;
    }

    public void saveObject(String owner, Object object, String id, Class clazz) {


        try {

            String json = gson.toJson(object);
            String filename = getDataFolder() + "/" + clazz.getSimpleName() + "-" + (owner != null ? owner + "-" : "") + id + ".json";
            File file = new File(filename);

            try (PrintWriter out = new PrintWriter(file)) {
                out.print(json);
            }
        } catch (IOException ex) {
            log.error(ex.getMessage());
        }
    }

    public void deleteObject(String owner, String id, Class clazz) {

        String prefix = clazz.getSimpleName() + "-" + (owner != null ? owner + "-" : "");
        String filename = prefix + id + ".json";
        File f = new File(getDataFolder() + "/" + filename);
        if (!f.delete()) {
            log.error("Cannot delete file object");
        }

    }


    public String getDataFolder() {
        return dataFolder;
    }

    public void setDataFolder(String dataFolder) {
        this.dataFolder = dataFolder;
    }
}
