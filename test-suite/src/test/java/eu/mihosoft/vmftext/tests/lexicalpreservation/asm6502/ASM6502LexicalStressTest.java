package eu.mihosoft.vmftext.tests.lexicalpreservation.asm6502;

import eu.mihosoft.vmftext.tests.asm6502.ASM6502Model;
import eu.mihosoft.vmftext.tests.asm6502.ASM6502SourceBundle;
import eu.mihosoft.vmftext.tests.asm6502.parser.ASM6502ModelParser;
import eu.mihosoft.vmftext.tests.asm6502.unparser.ASM6502ModelUnparser;
import eu.mihosoft.vmftext.tests.lexicalpreservation.RoundTripAssertions;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ASM6502LexicalStressTest {

    private final ASM6502ModelParser parser = new ASM6502ModelParser();
    private final ASM6502ModelUnparser unparser = new ASM6502ModelUnparser();

    @Test
    public void exactRoundTripForCommentsLabelsAndRepeatedLines() {
        String source = "; header\n" +
                "START:\n" +
                "  LDA #$01 ; load accumulator\n" +
                "\tSTA $0200\n" +
                "\n";

        RoundTripAssertions.assertExactRoundTrip(source, parser::parse, unparser::unparse);
    }

    @Test
    public void semanticRoundTripForExistingCombsortSample() throws IOException {
        String source = new String(Files.readAllBytes(Paths.get("test-code/asm6502/combsort.txt")));
        RoundTripAssertions.assertSemanticRoundTrip(source, parser::parse, unparser::unparse);
    }

    @Test
    public void sourceBundleRestoresAsmExactly() {
        String source = "  ORG $0800\n" +
                "LABEL:\n" +
                "  NOP ; keep\n";

        ASM6502Model model = parser.parse(source);
        ASM6502SourceBundle bundle = parser.toSourceBundle(model, source);

        Assert.assertEquals(source, unparser.unparse(parser.restoreFromSourceBundle(bundle)));
    }

    @Test
    public void appendedInstructionSourceRemainsParseable() {
        String changedSource = "START:\n" +
                "  LDA #$01\n" +
                "  STA $0200\n";

        ASM6502Model model = parser.parse(changedSource);
        parser.parse(unparser.unparse(model));
    }
}
