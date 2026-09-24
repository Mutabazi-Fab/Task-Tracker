package com.throughline.taskmanagement.mapper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Business Unit is now a department name, but incidents recorded while it was still an enum hold
 *  constants like "INFORMATION_TECHNOLOGY". */
class IncidentMapperBusinessUnitTest {

    private final IncidentMapper mapper = new IncidentMapper();

    @Test
    void legacyEnumConstantsAreRespelledAsWords() {
        assertEquals("Cybersecurity", mapper.displayBusinessUnit("CYBERSECURITY"));
        assertEquals("Information Technology", mapper.displayBusinessUnit("INFORMATION_TECHNOLOGY"));
        assertEquals("Branch Network", mapper.displayBusinessUnit("BRANCH_NETWORK"));
    }

    @Test
    void departmentNamesPassThroughUnchanged() {
        assertEquals("Payments & Settlements", mapper.displayBusinessUnit("Payments & Settlements"));
        assertEquals("Human Resources", mapper.displayBusinessUnit("Human Resources"));
        assertEquals("Other", mapper.displayBusinessUnit("Other"));
    }

    @Test
    void nullStaysNull() {
        assertNull(mapper.displayBusinessUnit(null));
    }
}
