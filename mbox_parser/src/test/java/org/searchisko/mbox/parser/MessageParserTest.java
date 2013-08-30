package org.searchisko.mbox.parser;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.junit.Assert.assertEquals;

/**
 * @author Lukáš Vlček (lvlcek@redhat.com)
 */
@RunWith(JUnit4.class)
public class MessageParserTest {

    @Test
    public void subjectNormalizationShouldPass() {

        assertEquals("Test subject", MessageParser.normalizeSubject(" [] [x x] Re: Vor: Test RE: subject "));
        assertEquals("Test subject", MessageParser.normalizeSubject(" Re: Fw: [] [x x] Test RE: [x] [] subject "));
        assertEquals("Web Container Integration testing (WCI)", MessageParser.normalizeSubject("Re: [gatein-dev] Web Container Integration testing (WCI)"));
        assertEquals("Type Substitution doesn't work with Schema2Java Client a [Re: this should stay here]", MessageParser.normalizeSubject("[jbossws-users] [JBossWS] - Re: [another-tag] Fw: Type Substitution doesn't work with\tSchema2Java Client a [Re: this should stay here]"));
        assertEquals("Multiple Assignment of a task in - jBPM4", MessageParser.normalizeSubject("[jbpm-users] [jBPM Users] - [JBPM4] Multiple Assignment of a task in - jBPM4"));
    }

}
