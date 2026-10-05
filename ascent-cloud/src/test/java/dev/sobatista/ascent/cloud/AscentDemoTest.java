package dev.sobatista.ascent.cloud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AscentDemoTest {
    @Test
    void demoLearnsRoutesAndScales() {
        AscentDemo.DemoResult r = AscentDemo.run();
        assertTrue(r.peakWorkers() > r.finalWorkers(), "it scaled up under the burst");
        assertEquals(1, r.finalWorkers(), "and back down to the minimum when drained");

        long errBefore = Math.abs(r.estimateBefore() - r.actual());
        long errAfter = Math.abs(r.estimateAfter() - r.actual());
        assertTrue(errAfter < errBefore, "the estimate sharpened after measuring one real run");
        assertEquals(10, r.served(), "every request was served");
    }
}
