//import org.junit.Test;
//import sandbox.StringUtils;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
///**
// * mstodo: Header
// *
// * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
// * <br>
// * Date: 8/6/18
// */
//public class StringUtilsTest {    // mstodo copy to TT
//    @Test
//    public void shouldGenerateProperLength() {
//        String generated = StringUtils.randomAlphabetic(13);
//        assertThat(generated).hasSize(13);
//    }
//
//    @Test
//    public void shouldGenerateAlphabetic() {
//        String generated = StringUtils.randomAlphabetic(200);
//        for (int i = 0; i < generated.length(); i++) {
//            assertThat(generated.charAt(i)).isBetween('a', 'z');
//        }
//    }
//}
