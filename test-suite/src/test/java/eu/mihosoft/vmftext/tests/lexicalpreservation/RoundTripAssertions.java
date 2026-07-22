package eu.mihosoft.vmftext.tests.lexicalpreservation;

import org.junit.Assert;

import java.util.function.Consumer;
import java.util.function.Function;

public final class RoundTripAssertions {

    private RoundTripAssertions() {
        throw new AssertionError("Don't instantiate me!");
    }

    public static <T> T assertExactRoundTrip(String source, Function<String,T> parser, Function<T,String> unparser) {
        T model = parser.apply(source);
        String unparsed = unparser.apply(model);
        Assert.assertEquals(source, unparsed);
        return model;
    }

    public static <T> void assertSemanticRoundTrip(String source, Function<String,T> parser, Function<T,String> unparser) {
        T model = parser.apply(source);
        String unparsed = unparser.apply(model);
        T reparsed = parser.apply(unparsed);
        Assert.assertEquals(model, reparsed);
    }

    public static <T> void assertParseableAfterMutation(String source, Function<String,T> parser,
                                                       Function<T,String> unparser, Consumer<T> mutation) {
        T model = parser.apply(source);
        mutation.accept(model);
        String unparsed = unparser.apply(model);
        T reparsed = parser.apply(unparsed);
        Assert.assertEquals(model, reparsed);
    }
}
