package team.projectpulse.student;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * PMD UseLocaleWithCaseConversions on StudentSpecs: the search lowercases what the user typed with the server's
 * default locale, then compares it with the database's lower(). Under a Turkish locale, Java lowercases "I" to the
 * dotless "ı", so a search for "IAN" looks for "ıan" and finds no one. Each test runs under that locale and expects
 * the search to look for plain "ian".
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StudentSpecs lowercases search input the same way on every server locale")
class StudentSpecsTest {

    private static final Locale TURKISH = Locale.forLanguageTag("tr-TR");

    @Mock
    Root<Student> root;

    @Mock
    CriteriaQuery<?> query;

    @Mock
    CriteriaBuilder criteriaBuilder;

    @Mock
    Path<Object> path;

    @Mock
    Path<Object> parentPath;

    @Mock
    Expression<String> loweredColumn;

    private Locale originalLocale;


    @BeforeEach
    void setUp() {
        this.originalLocale = Locale.getDefault();
        Locale.setDefault(TURKISH);
        given(this.criteriaBuilder.lower(any())).willReturn(this.loweredColumn);
    }

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(this.originalLocale);
    }

    private String likePatternOf(org.springframework.data.jpa.domain.Specification<Student> spec) {
        spec.toPredicate(this.root, this.query, this.criteriaBuilder);
        ArgumentCaptor<String> pattern = ArgumentCaptor.forClass(String.class);
        then(this.criteriaBuilder).should().like(eq(this.loweredColumn), pattern.capture());
        return pattern.getValue();
    }

    private Object equalValueOf(org.springframework.data.jpa.domain.Specification<Student> spec) {
        spec.toPredicate(this.root, this.query, this.criteriaBuilder);
        ArgumentCaptor<Object> value = ArgumentCaptor.forClass(Object.class);
        then(this.criteriaBuilder).should().equal(eq(this.loweredColumn), value.capture());
        return value.getValue();
    }

    @Test
    void testContainsFirstNameUnderTurkishLocale() {
        given(this.root.get("firstName")).willReturn(this.path);

        assertThat(likePatternOf(StudentSpecs.containsFirstName("IAN"))).isEqualTo("%ian%");
    }

    @Test
    void testContainsLastNameUnderTurkishLocale() {
        given(this.root.get("lastName")).willReturn(this.path);

        assertThat(likePatternOf(StudentSpecs.containsLastName("IAN"))).isEqualTo("%ian%");
    }

    @Test
    void testHasEmailUnderTurkishLocale() {
        given(this.root.get("email")).willReturn(this.path);

        assertThat(equalValueOf(StudentSpecs.hasEmail("IAN@ABC.EDU"))).isEqualTo("ian@abc.edu");
    }

    @Test
    void testHasTeamNameUnderTurkishLocale() {
        given(this.root.get("team")).willReturn(this.parentPath);
        given(this.parentPath.get("teamName")).willReturn(this.path);

        assertThat(equalValueOf(StudentSpecs.hasTeamName("TEAM IRIS"))).isEqualTo("team iris");
    }

    @Test
    void testHasSectionNameUnderTurkishLocale() {
        given(this.root.get("section")).willReturn(this.parentPath);
        given(this.parentPath.get("sectionName")).willReturn(this.path);

        assertThat(equalValueOf(StudentSpecs.hasSectionName("SECTION I"))).isEqualTo("section i");
    }

}
