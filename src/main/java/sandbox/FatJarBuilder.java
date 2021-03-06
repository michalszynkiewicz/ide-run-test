//package sandbox;
//
//import org.w3c.dom.Document;
//import org.wildfly.swarm.spi.meta.SimpleLogger;
//import org.wildfly.swarm.tools.ArtifactResolvingHelper;
//import org.wildfly.swarm.tools.ArtifactSpec;
//import org.wildfly.swarm.tools.BuildTool;
//import org.wildfly.swarm.tools.DeclaredDependencies;
//
//import javax.xml.parsers.DocumentBuilder;
//import javax.xml.parsers.DocumentBuilderFactory;
//import javax.xml.xpath.XPath;
//import javax.xml.xpath.XPathConstants;
//import javax.xml.xpath.XPathExpression;
//import javax.xml.xpath.XPathFactory;
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStream;
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.net.URLClassLoader;
//import java.nio.file.FileSystem;
//import java.nio.file.FileSystems;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.*;
//import java.util.function.Supplier;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
//import static java.util.stream.Collectors.toList;
//import static sandbox.StringUtils.randomAlphabetic;
//
///**
// * mstodo: Header
// * mstodo: try on license dictionary
// *
// * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
// * <br>
// * Date: 7/30/18
// */
//public class FatJarBuilder {
//
//    private final List<URL> classPathUrls;
//    private final File target;
//
//    public FatJarBuilder(List<URL> classPathUrls, File target) {
//        this.classPathUrls = classPathUrls;
//        this.target = target;
//    }
//
//    public static void main(String[] args) throws Exception {
//        File fatJar = new File(args[0]);
//
//        long start = System.currentTimeMillis();
//
//        buildFatJarTo(fatJar);
//
//        System.out.printf("total time %d ms\n", System.currentTimeMillis() - start);
//    }
//
//    private static File buildFatJarTo(File target) throws Exception {
//        List<URL> urls = getClasspathUrls();
//
//        FatJarBuilder b = new FatJarBuilder(urls, target);
//        return b.doBuild();
//    }
//
//    private static List<URL> getClasspathUrls() throws MalformedURLException {
//        URLClassLoader urlLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
//        List<String> urlList =
//                Stream.of(urlLoader.getURLs())
//                        .map(URL::toString)
//                        .collect(Collectors.toList());
//
//        List<URL> urls = new ArrayList<>();
//
//        for (String urlString : urlList) {
//            urls.add(new URL(urlString));
//        }
//        return urls;
//    }
//
//    private File doBuild() throws Exception {
//        final String type = "war";
//
//        // NOTE: we don't know which props are transitive!!!
//        long start = System.currentTimeMillis();
//        List<ArtifactOrFile> classPathEntries = analyzeClasspath();
//        System.out.println("Classpath analyzing time: " + (System.currentTimeMillis() - start + " ms"));
//
//        // non-maven jars are not taken into account above
//        // we need to make sure that none of them are skipped and add to war the ones that were
//        // mstodo
//        List<String> nonMavenJars = classPathEntries.stream()
//                .map(e -> e.file)
//                .filter(Objects::nonNull)
//                .collect(toList());
//
//        File war = buildWar(classPathEntries);
//        final BuildTool tool = new BuildTool(mavenArtifactResolvingHelper(), true)
//                .projectArtifact("tt",
//                        "tt-user-app",
//                        "0.1-SNAPSHOT",
//                        type,
//                        war,
//                        "artificial.war")// mstodo
//                .properties(System.getProperties())
////                .mainClass(this.mainClass)
////                .bundleDependencies(this.bundleDependencies)
//                .fractionDetectionMode(BuildTool.FractionDetectionMode.when_missing) // mstodo is this reasonable?
//                .hollow(false)
//                .logger(new SimpleLogger() {
//                    // mstodo proper logging
//                    @Override
//                    public void debug(String msg) {
//                        System.out.println(msg);
//                    }
//
//                    @Override
//                    public void info(String msg) {
//                        System.out.println(msg);
//                    }
//
//                    @Override
//                    public void error(String msg) {
//                        System.out.println(msg);
//                    }
//
//                    @Override
//                    public void error(String msg, Throwable t) {
//                        System.out.println(msg);
//                        t.printStackTrace();
//                    }
//                });
//
////        mstodo replace with system or env property
////        this.fractions.forEach(f -> {
////            if (f.startsWith(EXCLUDE_PREFIX)) {
////                tool.excludeFraction(ArtifactSpec.fromFractionDescriptor(FractionDescriptor.fromGav(FractionList.get(), f.substring(1))));
////            } else {
////                tool.fraction(ArtifactSpec.fromFractionDescriptor(FractionDescriptor.fromGav(FractionList.get(), f)));
////            }
////        });
//
////        Map<ArtifactSpec, Set<ArtifactSpec>> buckets = createBuckets(this.project.getArtifacts(), this.project.getDependencies());
//
//
//        // mstodo:
//        // - go through the classpath jars
//        //      -> for the ones built with maven, we have maven coordinates in the META-INF/maven/../pom.xml
//        // - run the tool on it
//        // - check which jars are missing from the fat jar, add them to war
//        // - add classes to war
//        tool.declaredDependencies(declaredDependencies(classPathEntries));
//
//
//        // mstodo can any non-file url get here?
//        this.classPathUrls.parallelStream()
//                .filter(url -> !url.toString().matches(".*\\.[^/]*"))
//                .forEach(r -> tool.resourceDirectory(r.getFile()));
//
//        // mstodo replace/remove
//        Path uberjarResourcesDir = File.createTempFile("uberjar-resources-placehodler", "bs").toPath();
//        tool.uberjarResourcesDirectory(uberjarResourcesDir);
//
//        File jar = tool.build(target.getName(), target.getParentFile().toPath());
//
//
////            if (this.project.getPackaging().equals(WAR)) {
////            mstodo: create a war with everything that didn't make into the jars above
//        tool.repackageWar(war);
////            }
//        return jar;
//    }
//
//    /*
//    mstodo: it is to early to add jars here!
//     */
//    /**
//     * builds war with classes inside
//     *
//     * @param classPathEntries class path entries as ArtifactSpec or URLs
//     * @return the war file
//     */
//    private File buildWar(List<ArtifactOrFile> classPathEntries) {
//        try {
//            List<String> classesUrls = classPathEntries.stream()
//                    .filter(ArtifactOrFile::hasSpec)
//                    .map(ArtifactOrFile::file)
//                    .filter(url -> url.endsWith("/classes/"))
//                    .collect(Collectors.toList());
//
//            List<File> classpathJars = classPathEntries.stream()
//                    .map(ArtifactOrFile::file)
//                    .filter(file -> file.endsWith(".jar"))
//                    .map(File::new)
//                    .collect(Collectors.toList());
//
//
//            return WarBuilder.build(classesUrls, classpathJars);
//        } catch (IOException e) {
//            throw new RuntimeException("failed to build war", e);
//        }
//    }
//
//
//    private ArtifactResolvingHelper mavenArtifactResolvingHelper() {
//        // all artifacts should have files defined, no need to resolve anything
//        return new ArtifactResolvingHelper() {
//            @Override
//            public ArtifactSpec resolve(ArtifactSpec spec) throws Exception {
//                return spec;
//            }
//
//            @Override
//            public Set<ArtifactSpec> resolveAll(Collection<ArtifactSpec> specs, boolean transitive, boolean defaultExcludes) throws Exception {
//                return new HashSet<>(specs);
//            }
//        };
//    }
//
//    private List<ArtifactOrFile> analyzeClasspath() {
//        return classPathUrls.parallelStream()    // [mstodo remove comment] verified
//                .map(this::urlToSpec)
//                .collect(toList());
//    }
//
//    private DeclaredDependencies declaredDependencies(List<ArtifactOrFile> specsOrUrls) {
//        List<ArtifactSpec> specs =
//                specsOrUrls.stream()
//                        .filter(ArtifactOrFile::hasSpec)
//                        .map(specOrUrl -> specOrUrl.spec)
//                        .collect(toList());
//        return new DeclaredDependencies() {
//            @Override
//            public Collection<ArtifactSpec> getDirectDeps() {
//                return specs;
//            }
//
//            @Override
//            public Collection<ArtifactSpec> getTransientDeps(ArtifactSpec parent) {
//                return Collections.emptyList();
//            }
//        };
//    }
//
//    private ArtifactOrFile urlToSpec(URL url) {
//        String file = resolveUrlToFile(url);
//        if (!url.toString().endsWith(".jar")) {
//            return ArtifactOrFile.file(file);
//        }
//        try (FileSystem fs = FileSystems.newFileSystem(Paths.get(file), getClass().getClassLoader())) {
//            Optional<Path> maybePomXml = findPom(fs);
//
//            ArtifactSpec spec = maybePomXml
//                    .map(pom -> toArtifactSpec(pom, file))
//                    .orElse(mockArtifactSpec(file));
//            return ArtifactOrFile.spec(spec);
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to parse jar: " + file, e);
//        }
//    }
//
//    private Optional<Path> findPom(FileSystem fs) throws IOException {
//        Optional<Path> maybePomXml;
//        Path path = fs.getPath("/META-INF/maven");
//
//        if (path == null || !Files.exists(path)) {
//            maybePomXml = Optional.empty();
//        } else {
//
//            /*mstodo: switch this to getPathMatcher?*/
//            maybePomXml = Files.walk(path)
//                    .filter(p -> p.endsWith("pom.xml"))
//                    .findAny();
//        }
//        return maybePomXml;
//    }
//
//    private ArtifactSpec mockArtifactSpec(String jarPath) {
//        return new ArtifactSpec("compile",
//                "com.fakegroupid", randomAlphabetic(10), "0.0.1", "jar", null,
//                new File(jarPath));
//    }
//
//
//    private ArtifactSpec toArtifactSpec(Path pom, String jarPath) {
//        try {
//            // mstodo watch out for properties?
//            String groupId = extract(pom, "/project/groupId",
//                    () -> extract(pom, "/project/parent/groupId"));
//            String artifactId = extract(pom, "/project/artifactId");
//            String version = extract(pom, "/project/version",
//                    () -> extract(pom, "/project/parent/version"));
//            String packaging = extract(pom, "/project/packaging", "jar");
//            String classifier = extract(pom, "/project/classifier", (String) null);
//
//            return new ArtifactSpec("compile",
//                    groupId, artifactId, version, packaging, classifier,
//                    new File(jarPath));
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to read artifact spec from pom " + pom, e);
//        }
//    }
//
//    private String extract(Path source, String expression, Supplier<String> defaultValueProvider) {
//        String extracted = extract(source, expression);
//
//        return extracted == null || "".equals(extracted)
//                ? defaultValueProvider.get()
//                : extracted;
//    }
//
//    private String extract(Path source, String expression, String defaultValue) {
//        String extracted = extract(source, expression);
//
//        return extracted == null || "".equals(extracted)
//                ? defaultValue
//                : extracted;
//    }
//
//    private String extract(Path sourcePath, String expression) {   // mstodo simplify, make the extract great again
//        try (InputStream source = Files.newInputStream(sourcePath)) {
//            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//            DocumentBuilder builder = factory.newDocumentBuilder();
//            Document doc = builder.parse(source);
//            XPathFactory xPathfactory = XPathFactory.newInstance();
//            XPath xpath = xPathfactory.newXPath();
//            XPathExpression expr = xpath.compile(expression);
//            return (String) expr.evaluate(doc, XPathConstants.STRING);
//        } catch (Exception any) {
//            throw new RuntimeException("Failure when trying to find a match for " + expression, any);
//        }
//    }
//
//    private String resolveUrlToFile(URL url) {
//        String zipFile = url.getFile();
//        if (zipFile == null) {
//            try (InputStream stream = url.openStream()) {
//                zipFile = File.createTempFile("tt-dependency", ".jar").getAbsolutePath();
//                Files.copy(stream, Paths.get(zipFile));
//            } catch (IOException e) {
//                throw new RuntimeException("Unable to resolve: " + url);
//            }
//        }
//        return zipFile;
//    }
//
//
//    public static class ArtifactOrFile {
//        private final String file;     // todo: check if holding file is not better
//        private final ArtifactSpec spec;     // todo not needed
//
//        private ArtifactOrFile(String file, ArtifactSpec spec) {
//            this.file = file;
//            this.spec = spec;
//        }
//
//        public String file() {
//            return file;
//        }
//
//        public ArtifactSpec spec() {
//            return spec;
//        }
//
//        public boolean hasSpec() {
//            return spec != null;
//        }
//
//        private static ArtifactOrFile file(String file) {
//            return new ArtifactOrFile(file, null);
//        }
//        private static ArtifactOrFile spec(ArtifactSpec spec) {
//            return new ArtifactOrFile(spec.file.getAbsolutePath(), spec);
//        }
//    }
//}
