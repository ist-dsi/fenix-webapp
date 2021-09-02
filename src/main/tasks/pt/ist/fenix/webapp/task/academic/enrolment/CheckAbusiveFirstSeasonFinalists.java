package pt.ist.fenix.webapp.task.academic.enrolment;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.academic.domain.studentCurriculum.CycleCurriculumGroup;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.i18n.LocalizedString;

import java.util.Locale;
import java.util.function.Supplier;

public class CheckAbusiveFirstSeasonFinalists extends ReadCustomTask {

    private static final Locale PT = new Locale("pt");
    private static final Locale UK = Locale.UK;

    private static final String CODE = "INTEGRATED_FIRST_CYCLE_FINALIST";
    private static final LocalizedString NAME = new LocalizedString(PT, "Finalista 1º Ciclo Integrado")
            .with(UK, "1st Cycle Integrated Finalist");

    @Override
    public void runTask() throws Exception {
        final StatuteType statuteType = Bennu.getInstance().getStatuteTypesSet().stream()
                .filter(t -> t.getCode().equals(CODE))
                .findAny().orElse(null);

        Bennu.getInstance().getDegreesSet().stream()
                .filter(d -> d.getDegreeType().isIntegratedMasterDegree())
                .flatMap(d -> d.getRegistrationsSet().stream())
                .filter(r -> r.isActive())
                .filter(r -> hasStatus(statuteType, r))
                .forEach(this::check);
    }

    private void check(final Registration registration) {
        final long totalCount = registration.getStudentCurricularPlansSet().stream()
                .flatMap(scp -> scp.getEnrolmentStream())
                .filter(e -> e.getExecutionYear().isCurrent())
                .map(e -> e.getFinalEnrolmentEvaluation())
                .filter(ee -> ee != null && ee.getEvaluationSeason().isExtraordinary())
                .count();
        if (totalCount > 15d) {
            taskLog("Student %s enrolled in too many credits%n", registration.getPerson().getUsername());
        }

        final long secondCycleCount = registration.getStudentCurricularPlansSet().stream()
                .flatMap(scp -> scp.getEnrolmentStream())
                .filter(e -> e.getExecutionYear().isCurrent())
                .filter(e -> !isFirstCycle(e))
                .map(e -> e.getFinalEnrolmentEvaluation())
                .filter(ee -> ee != null && ee.getEvaluationSeason().isExtraordinary())
                .count();
        if (secondCycleCount > 0d) {
            taskLog("Student %s is abusing the rules%n", registration.getPerson().getUsername());
        }
    }

    private boolean isFirstCycle(final Enrolment enrolment) {
        final CycleCurriculumGroup group = enrolment.getParentCycleCurriculumGroup();
        return group != null && group.getCycleType() == CycleType.FIRST_CYCLE;
    }

    private boolean hasStatus(final StatuteType statuteType, final Registration registration) {
        return registration.getStudent().getStudentStatutesSet().stream()
                .filter(statute -> statute.getType().getCode().equals(statuteType.getCode()))
                .anyMatch(statute -> statute.getBeginExecutionPeriod().isCurrent() || statute.getEndExecutionPeriod().isCurrent());
    }

}
