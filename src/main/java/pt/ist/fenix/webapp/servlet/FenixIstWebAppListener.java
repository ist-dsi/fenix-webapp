package pt.ist.fenix.webapp.servlet;

import org.fenixedu.academic.domain.SchoolLevelType;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.admissions.ist.util.QualificationLevelUtil;
import org.fenixedu.ulisboa.integration.sas.service.process.AbstractFillScholarshipService;
import pt.ist.fenixframework.FenixFramework;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@WebListener
public class FenixIstWebAppListener implements ServletContextListener {

    private SchoolLevelType toSchoolLevelType(final String qualificationLevel) {
        if (qualificationLevel == null) {
            return SchoolLevelType.UNKNOWN;
        }
        if (qualificationLevel.equals("qualificationLevel1")) {
            return SchoolLevelType.FIRST_CYCLE_BASIC_SCHOOL;
        }
        if (qualificationLevel.equals("qualificationLevel2")) {
            return SchoolLevelType.THIRD_CYCLE_BASIC_SCHOOL;
        }
        if (qualificationLevel.equals("qualificationLevel3")) {
            return SchoolLevelType.HIGH_SCHOOL_OR_EQUIVALENT;
        }
        if (qualificationLevel.equals("qualificationLevel4")) {
            return SchoolLevelType.TECHNICAL_SPECIALIZATION;
        }
        if (qualificationLevel.equals("qualificationLevel5")) {
            return SchoolLevelType.MEDIUM_EDUCATION;
        }
        if (qualificationLevel.equals("qualificationLevel6")) {
            return SchoolLevelType.BACHELOR_DEGREE;
        }
        if (qualificationLevel.equals("qualificationLevel7")) {
            return SchoolLevelType.MASTER_DEGREE;
        }
        if (qualificationLevel.equals("qualificationLevel8")) {
            return SchoolLevelType.DOCTORATE_DEGREE;
        }
        return null;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        FenixFramework.atomic(() -> {
            AbstractFillScholarshipService.completedQualificationsSchoolLevelTypeSupplier = (person) -> {
                final Student student = person.getStudent();
                return Stream.concat(
                        student.getPersonalIngressionsDataSet().stream()
                                .flatMap(x -> x.getPrecedentDegreesInformationsSet().stream())
                                .map(x -> x.getSchoolLevel()),
                        person.getUser().getIdentity().getAccountSet().stream()
                                .flatMap(account -> account.getApplicationSet().stream())
                                .map(QualificationLevelUtil::maxCompletedQualificationLevel)
                                .map(this::toSchoolLevelType)
                ).collect(Collectors.toSet());
            };
        });
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }

}
