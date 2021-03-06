//package sandbox;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.List;
//import java.util.zip.ZipEntry;
//import java.util.zip.ZipOutputStream;
//
///**
// * mstodo: Header
// *
// * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
// * <br>
// * Date: 8/1/18
// */
//public class WarBuilder {
//    // mstodo webapp resources pointed by properties
//
//    public static File build(List<String> classesDirs, List<File> classpathJars) throws IOException {
//        File war = File.createTempFile("temp-tt-war", ".war");   // mstodo better path
//        try (FileOutputStream fos = new FileOutputStream(war);
//             ZipOutputStream out = new ZipOutputStream(fos)) {
//
//            WarBuilder builder = new WarBuilder(out, classesDirs, classpathJars);
//            builder.build();
//        }
//        System.out.println("built " + war.getAbsolutePath());
//        return war;
//    }
//
//    private final ZipOutputStream output;
//    private final List<String> classesDirs;
//    private final List<File> jars;
//
//    private WarBuilder(ZipOutputStream output, List<String> classesDirs, List<File> jars) {
//        this.output = output;
//        this.classesDirs = classesDirs;
//        this.jars = jars;
//    }
//
//    private void build() {
//        classesDirs.forEach(this::addClassesToWar);
//        addWebAppResourcesToWar();
//        jars.stream()
//        .filter(this::notJdkJar)
//        .forEach(this::addJarToWar);
//    }
//
//    private boolean notJdkJar(File file) {
//        // mstodo test it!
//        return !file.getAbsolutePath().contains(System.getProperty("java.home"));
//    }
//
//    private void addClassesToWar(String directory) {
//        File classesDirectory = new File(directory);
//        if (!classesDirectory.isDirectory()) {
//            throw new RuntimeException("Invalid classes directory on classpath: " + directory);
//        }
//        addClassesToWar(classesDirectory);
//    }
//
//    private void addWebAppResourcesToWar() {
//        try {
//            Path webappPath = Paths.get("src", "main", "webapp");
//            if (!webappPath.toFile().exists()) {
//                return;
//            }
//            Files.walk(webappPath)
//                    .forEach(this::addWebappResourceToWar);
//        } catch (IOException e) {
//            throw new RuntimeException("Unable to get webapp dir");
//        }
//    }
//
//    private void addJarToWar(File file) {
//        String jarName = file.getName();
//        try {
//            writeFileToZip(file, "WEB-INF/lib/" + jarName); // mstodo: test on windows
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to add jar " + file.getAbsolutePath() + " to war", e);
//        }
//    }
//
//    private void addWebappResourceToWar(Path path) {
//        File file = path.toFile();
//        if (file.isFile()) {
//            try {
//                String projectDir = Paths.get("src", "main", "webapp").toFile().getAbsolutePath();
//                // mstodo test on windows!
//
//                String fileName = file.getAbsolutePath().replace(projectDir, "");
//                System.out.println("filename: "+ fileName + ", abs path: " + file.getAbsolutePath() + ", to replace from it: " + Paths.get(".").toFile().getAbsolutePath());
//                writeFileToZip(file, fileName);
//            } catch (IOException e) {
//                throw new RuntimeException("Unable to add file: " + path.toAbsolutePath() + "  from webapp to the war", e);
//            }
//        }
//    }
//
//    private void addClassesToWar(File classesDirectory) {
//        try {
//            Files.walk(classesDirectory.toPath())
//                    .map(Path::toFile)
//                    .filter(File::isFile)
//                    .forEach(file -> addClassToWar(file, classesDirectory));
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to add classes to war", e);
//        }
//    }
//
//    private void addClassToWar(File file, File classesDirectory) {
//        try {
//            String filePath = file.getAbsolutePath();
//            String name = filePath.replaceFirst(classesDirectory.getAbsolutePath(), "");
//            name = name.replaceAll("^/", "");
//            name = name.replaceAll("^\\\\", "");   // mstodo test it on windows
//            name = "/WEB-INF/classes/" + name;
//            writeFileToZip(file, name);
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to add file " + file.getAbsolutePath() + " to war", e);
//        }
//    }
//
//    private void writeFileToZip(File file, String name) throws IOException {
//        ZipEntry entry = new ZipEntry(name);
//        output.putNextEntry(entry);
//        try (FileInputStream input = new FileInputStream(file)) {
//            byte[] buffer = new byte[4096];
//            int length;
//            while ((length = input.read(buffer)) >= 0) {
//                output.write(buffer, 0, length);
//            }
//        }
//        output.closeEntry();
//    }
//}
