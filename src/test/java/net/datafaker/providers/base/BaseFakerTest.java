package net.datafaker.providers.base;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockitoAnnotations;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BaseFakerTest<T extends BaseFaker> {

    protected T faker = getFaker();

    @BeforeEach
    @SuppressWarnings("EmptyTryBlock")
    final void resetMocks() throws Exception {
        try (AutoCloseable ignored = MockitoAnnotations.openMocks(this)) {
            // Need to reset all @Spy and @Mock fields
            // because all test methods share the same test class instance due to @TestInstance(PER_CLASS)
        }
    }

    @SuppressWarnings("unchecked")
    protected T getFaker() {
        return (T) new BaseFaker();
    }

    protected void setFaker(T faker) {
        this.faker = faker;
    }

    protected List<String> getBaseList(String key) {
        return faker.fakeValuesService().fetchObject(key, faker.getContext());
    }

    @ParameterizedTest(name = "{0}", allowZeroInvocations = true)
    @MethodSource("providerListTest")
    protected void testProviderList(TestSpec testSpec, TestInfo testInfo) {
        // Given
        Set<String> actual = new HashSet<>(getBaseList(testSpec.key));
        // When
        String item = (String) testSpec.supplier.get();
        // Then
        assertThat(item).as("Check item isn't empty").isNotEmpty();
        String collection = "\"" + testSpec.key + "\"";
        assertThat(actual).as("Check actual list isn't empty and contains the item for the key " + collection).isNotEmpty()
            .anyMatch(item::equals);
        assertThat(actual).as("Actual should not have empty entries. " + collection).noneMatch(single -> single.isBlank());
        if (!testSpec.regex.isEmpty()) {
            assertThat(item).as("Check item matches regex").matches(Pattern.compile(testSpec.regex));
        }
    }

    @ParameterizedTest(allowZeroInvocations = true)
    @MethodSource("providerListTest")
    void testNoDuplications(TestSpec testSpec) {
        var terms = getBaseList(testSpec.key);

        Set<String> uniques = new HashSet<>();
        Set<String> duplicates = new HashSet<>();
        for (String term : terms) {
            if (!uniques.add(term)) {
                duplicates.add(term);
            }
        }
        assertThat(duplicates)
            .as("Check no duplications in " + testSpec.key + " with terms " + terms)
            .isEmpty();
    }

    protected Collection<TestSpec> providerListTest() {
        return Set.of();
    }

    protected static class TestSpec {
        private final Supplier<?> supplier;
        private final String key;
        @SuppressWarnings("unused")
        private final String regex;

        private TestSpec(Supplier<?> supplier, String key, String regex) {
            this.supplier = supplier;
            this.key = key;
            this.regex = regex;
        }

        public static TestSpec of(Supplier<?> supplier, String key) {
            return new TestSpec(supplier, key, "");
        }

        public static TestSpec of(Supplier<?> supplier, String key, String regex) {
            return new TestSpec(supplier, key, regex);
        }

        @Override
        public String toString() {
            // The result of this toString will be used by IDE in test report
            return "Key: " + key;
        }
    }
}
