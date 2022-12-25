import operators.OperationResult;
import operators.Utils;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;

import static operators.Operators.*;

class EquivalentAssertionFailedError extends AssertionFailedError {
    public EquivalentAssertionFailedError(OperationResult a, OperationResult b) {
        super(
                String.format("Expected '%s' (normalized: '%s') to be equivalent to '%s' (normalized: '%s'), but wasn't!%n",
                        a, a.getNormalized(),
                        b, b.getNormalized()
                )
        );
    }
}

public class OperatorTest {
    private void assertEquivalent(OperationResult a, OperationResult b) {
        if (!a.isEquivalent(b)) throw new EquivalentAssertionFailedError(a, b);
    }

    private void assertNormalizedFormContainsOriginalNumbers(OperationResult original) {
        HashMap<Double, Integer> originalOriginals = original.usedOriginalsWithCounts();
        HashMap<Double, Integer> normalizedOriginals = original.getNormalized().usedOriginalsWithCounts();
        for (Double key : originalOriginals.keySet()) {
            int expectedOccurrences = originalOriginals.get(key);
            int actualOccurrences = normalizedOriginals.getOrDefault(key, 0);
            try {
                if (actualOccurrences < expectedOccurrences) {
                    throw new AssertionFailedError(
                            String.format(
                                    "Normalized form '%s' of '%s' contained fewer original numbers than original expression.",
                                    original.getNormalized(),
                                    original
                            ),
                            String.format("At least %d occurrences of %s", expectedOccurrences, key),
                            String.format("%d occurrences of %s", actualOccurrences, key)
                    );
                }
            } catch (AssertionFailedError e) {
                if (!Utils.doubleEquals(key, 1) && !Utils.doubleEquals(key, -1)) throw e;
                System.out.printf("Insufficient occurrences (%s < %s) of key %s in normalized form '%s' of '%s'%n",
                        actualOccurrences, expectedOccurrences, key, original.getNormalized(), original);
                System.out.printf("Ignoring because reduction of the occurrences of %s can be allowed in normalization%n", key);
            }
        }
    }

    private void assertNormalizationDoesNotChangeOriginal(OperationResult original) {
        if (original.hasBeenNormalized())
            throw new IllegalArgumentException(String.format(
                    "Expected %s that had not yet been normalized, but got normalized '%s'!",
                    OperationResult.class, original
            ));
        String preString = original.toString();
        original.getNormalized();
        String postString = original.toString();
        assertEquals(preString, postString);
    }

    private void assertNormalizationIsReflexive(OperationResult original) {
        assertEquals(original.getNormalized().toString(), original.getNormalized().getNormalized().toString());
    }

    private void assertNormalizationWorksBasic(OperationResult... originals) {
        for (OperationResult original : originals) {
            assertNormalizationDoesNotChangeOriginal(original);
            assertNormalizationIsReflexive(original);
            assertNormalizedFormContainsOriginalNumbers(original);
        }
    }

    private void assertEquivalent(OperationResult... operationResults) {
        for (OperationResult left : operationResults) {
            for (OperationResult right : operationResults) {
                assertEquivalent(left, right);
            }
        }
    }

    private void assertEquivalenceAndNormalization(OperationResult... operationResults) {
        for (OperationResult operationResult : operationResults)
            assertNormalizationWorksBasic(operationResult);
        assertEquivalent(operationResults);
    }

    @Test
    public void test1() {
        OperationResult a = new OperationResult(1).apply(ADD, 2).apply(ADD, new OperationResult(3).apply(MUL, 4)).apply(ADD, 5);  // 1 + 2 + 3 * 4 + 5
        OperationResult b = new OperationResult(1).apply(ADD, new OperationResult(2).apply(ADD, new OperationResult(3).apply(MUL, 4))).apply(ADD, 5);  // 1 + (2 + 3 * 4) + 5
        OperationResult c = new OperationResult(1).apply(ADD, new OperationResult(2).apply(ADD, new OperationResult(3).apply(MUL, 4)).apply(ADD, 5));  // 1 + (2 + 3 * 4 + 5)

        assertEquivalenceAndNormalization(a, b, c);
    }

    @Test
    public void test2() {
        OperationResult nine = new OperationResult(9);
        OperationResult three = new OperationResult(3);
        OperationResult four = new OperationResult(4);
        OperationResult seven = new OperationResult(7);

        OperationResult a = nine.apply(ADD, three.apply(MUL, four).apply(SUB, seven));  // 9 + (3 * 4 - 7)
        OperationResult b = nine.apply(ADD, three.apply(MUL, four)).apply(SUB, seven);  // 9 + (3 * 4) - 7

        assertEquivalenceAndNormalization(a, b);
    }

    @Test
    public void test3() {
        OperationResult two = new OperationResult(2);
        OperationResult three = new OperationResult(3);
        OperationResult seven = new OperationResult(7);
        OperationResult eight = new OperationResult(8);
        OperationResult nine = new OperationResult(9);

        OperationResult a = two.apply(SUB, three.apply(MUL, nine).apply(SUB, eight).apply(ADD, seven)).apply(SUB, 2);  // 2 - (3 * 9 - 8 + 7) - 2;
        OperationResult b = eight.apply(SUB, three.apply(MUL, nine)).apply(SUB, seven.apply(ADD, two)).apply(ADD, two);  // 8 - 3 * 9 - (7 + 2) + 2;

        assertEquivalenceAndNormalization(a, b);
    }

    @Test
    public void divisionTest1() {
        OperationResult two = new OperationResult(2);
        OperationResult seven = new OperationResult(7);
        OperationResult eight = new OperationResult(8);
        OperationResult nine = new OperationResult(9);

        OperationResult a = seven.apply(DIV, nine.apply(SUB, eight).apply(DIV, two));  // 7 / ((9 - 8) / 2)
        OperationResult b = two.apply(DIV, nine.apply(SUB, eight).apply(DIV, seven));  // 2 / ((9 - 8) / 7)

        assertEquivalenceAndNormalization(a, b);
    }

    @Test
    public void divisionTestInvertSimple() {
        OperationResult one = new OperationResult(1);
        OperationResult two = new OperationResult(2);
        OperationResult seven = new OperationResult(7);

        OperationResult a = one.apply(DIV, seven).getNormalized();

        assertEquals(two, a.left);
        assertEquals(MUL, a.operator);
        assertNotNull(a.right);
        assert a.right.left != null;
        assertEquals(one, a.right.left);
        assertEquals(DIV, a.right.operator);
        assertEquals(seven, a.right.right);
    }

    @Test
    public void divisionTestDoubleInvertSimple() {
        OperationResult one = new OperationResult(1);
        OperationResult seven = new OperationResult(7);

        OperationResult a = one.apply(DIV, one.apply(DIV, seven));

        assertEquivalenceAndNormalization(a, seven);
    }

    @Test
    public void divisionTestNegative() {
        OperationResult two = new OperationResult(2);
        OperationResult three = new OperationResult(3);
        OperationResult five = new OperationResult(5);
        OperationResult seven = new OperationResult(7);

        OperationResult a = two.apply(SUB, three).apply(DIV, five.apply(SUB, seven));  // (2 - 3) / (5 - 7)
        OperationResult b = three.apply(SUB, two).apply(DIV, seven.apply(SUB, five));  // (3 - 2) / (7 - 5)

        assertEquivalenceAndNormalization(a, b);
    }

    @Test
    public void divisionTestDistribute() {
        OperationResult two = new OperationResult(2);
        OperationResult three = new OperationResult(3);
        OperationResult five = new OperationResult(5);
        OperationResult seven = new OperationResult(7);

        OperationResult a = two.apply(ADD, three).apply(SUB, 5).apply(DIV, seven);  // (2 + 3 - 5) / 7
        OperationResult b = two.apply(DIV, seven).apply(ADD, three.apply(DIV, seven)).apply(SUB, five.apply(DIV, seven));  // 2 / 7 + 3 / 7 - 5 / 7

        assertEquivalenceAndNormalization(a, b);
    }

    // TODO: Test for false positives

    @Test
    public void divisionBasic() {
        OperationResult a = new OperationResult(5).apply(DIV, 3);  // 5 / 3
        OperationResult b = new OperationResult(5).apply(MUL, new OperationResult(1).apply(DIV, 3));  // 5 * (1 / 3)

        assertEquivalenceAndNormalization(a, b);
    }

    @Test
    public void dev() {
        // 7 / ((9 - 8) / 2)
        OperationResult or = new OperationResult(7).apply(DIV, new OperationResult(9).apply(SUB, 8).apply(DIV, 2));
        System.out.println(or);
        System.out.println(or.getNormalized());
    }
}
