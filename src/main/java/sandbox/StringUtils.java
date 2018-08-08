//package sandbox;
//
//import java.util.concurrent.ThreadLocalRandom;
//
///**
// * mstodo: Header
// *
// * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
// * <br>
// * Date: 8/6/18
// */
//public class StringUtils {
//    private static final ThreadLocalRandom random = ThreadLocalRandom.current();
//
//    public static String randomAlphabetic(int length) {
//        StringBuilder result = new StringBuilder();
//        for (int i = 0; i < length; i++) {
//
//            char nextChar = (char) ('a' + random.nextInt('z' - 'a'));
//            result.append(nextChar);
//        }
//        return result.toString();
//    }
//
//}
