package sandbox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * mstodo: Header
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 8/1/18
 */
public class WarBuilder {

    public static File build(List<URL> classesUrls) throws IOException {
//        return new File("/tmp/temp-tt-war4221518960687377638.war"); // mstodo bring back below:
//        return new File("/home/michal/job/tmp/thorn-1980/target/unzipped/_bootstrap/sandbox-0.0.1-SNAPSHOT.war"); // mstodo bring back below:
        File war = File.createTempFile("temp-tt-war", ".war");
        try (FileOutputStream fos = new FileOutputStream(war);
             ZipOutputStream out = new ZipOutputStream(fos)) {

            WarBuilder builder = new WarBuilder(out, classesUrls);
            // mstodo remove
            out.putNextEntry(new ZipEntry("test"));
            out.write("test".getBytes());
            out.closeEntry();
            // mstodo end
            builder.build();
        }
        System.out.println("built " + war.getAbsolutePath());
        return war;
    }

    private final ZipOutputStream output;
    private final List<URL> classesUrls;

    private WarBuilder(ZipOutputStream output, List<URL> classesUrls) {
        this.output = output;
        this.classesUrls = classesUrls;
    }

    private void build() {
        classesUrls.forEach(this::addClassesToWar);
    }

    private void addClassesToWar(URL url) {
        String file = url.getFile();
        File classesDirectory = new File(url.getFile());
        if (!classesDirectory.isDirectory()) {
            throw new RuntimeException("Invalid classes directory on classpath: " + file);
        }
        addClassesToWar(classesDirectory);
    }

    private void addClassesToWar(File classesDirectory) {
        try {
            Files.walk(classesDirectory.toPath())
                    .map(Path::toFile)
                    .filter(File::isFile)
                    .forEach(file -> addFileToWar(file, classesDirectory));
        } catch (IOException e) {
            throw new RuntimeException("Failed to add classes to war", e);
        }
    }

    private void addFileToWar(File file, File classesDirectory) {
        if (file.getAbsolutePath().contains("sandbox")) {
            return;
        }
        try {
            String filePath = file.getAbsolutePath();
            String name = filePath.replaceFirst(classesDirectory.getAbsolutePath(), "");
            name = name.replaceAll("^/", "");
            name = name.replaceAll("^\\\\", "");   // todo test it on windows?
            name = "/WEB-INF/classes/" + name;
            ZipEntry entry = new ZipEntry(name);
            output.putNextEntry(entry);
//            output.write("test".getBytes());
//
            try (FileInputStream input = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int length;
                while ((length = input.read(buffer)) >= 0) {
                    output.write(buffer, 0, length);
                }
            }
            output.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException("Failed to add file " + file.getAbsolutePath() + " to war", e);
        }
    }
}
