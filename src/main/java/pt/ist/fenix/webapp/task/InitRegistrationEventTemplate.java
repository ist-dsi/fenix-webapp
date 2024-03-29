package pt.ist.fenix.webapp.task;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.EventTemplate;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import pt.ist.fenix.webapp.config.academic.accounting.EventConfig;
import pt.ist.fenix.webapp.config.academic.accounting.EventConfig.EventTemplateCode;
import pt.ist.fenixframework.FenixFramework;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO : in the future this should be set upon registration from Application according to the Admissions Process
@Task(englishTitle = "Init Event Template for all registrations", readOnly = true)
public class InitRegistrationEventTemplate extends CronTask {

    @Override
    public void runTask() throws Exception {
        final Set<Registration> set = ExecutionYear.readCurrentExecutionYear().getExecutionPeriodsSet().stream()
                .flatMap(executionSemester -> executionSemester.getEnrolmentsSet().stream())
                .map(enrolment -> enrolment.getRegistration())
                .collect(Collectors.toSet());
        final Set<Registration> mobility = Bennu.getInstance().getRegistrationsSet().stream()
                .filter(registration -> registration.isInMobilityState())
                .collect(Collectors.toSet());
        Stream.concat(set.stream(), mobility.stream()).distinct().parallel().forEach(this::init);
    }

    private void init(final Registration registration) {
        FenixFramework.atomic(() -> {
            if (registration.getEventTemplate() == null) {
                final EventTemplate eventTemplate = findBestTemplateFor(registration);
                if (eventTemplate == null) {
                    taskLog("No template found for: %s\t%s\t%s%n",
                            registration.getPerson().getUsername(),
                            registration.getDegree().getSigla(),
                            registration.getRegistrationProtocol().isAlien() ? "Alien" : "Normal");
                } else {
                    registration.setEventTemplate(eventTemplate);
                }
            }

            if (registration.isInMobilityState()) {
                final ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();
                if (executionYear != null) {
                    RegistrationDataByExecutionYear.getOrCreateRegistrationDataByYear(registration, executionYear);
                }
            }
        });
    }

    private EventTemplate findBestTemplateFor(final Registration registration) {
        final RegistrationProtocol protocol = registration.getRegistrationProtocol();
        final Degree degree = registration.getDegree();
        final boolean isAlien = protocol.isAlien();
        final boolean isEmptyDegree = degree.isEmpty();
        final EventTemplateCode code;
        if (isEmptyDegree) {
            final boolean hasOtherRegistration = registration.getStudent().getRegistrationsSet().stream()
                    .filter(r -> r != registration)
                    .flatMap(r -> r.getStudentCurricularPlansSet().stream())
                    .flatMap(scp -> scp.getEnrolmentStream())
                    .anyMatch(enrolment -> enrolment.getExecutionYear().isCurrent());
            if (hasOtherRegistration) {
                code = isAlien ? EventTemplateCode.ISOLATED_COURSES_INTERNAL_INTERNATIONAL
                        : EventTemplateCode.ISOLATED_COURSES_INTERNAL;
            } else {
                code = EventTemplateCode.ISOLATED_COURSES_EXTERNAL;
            }
        } else if (protocol.isMilitaryAgreement()) {
            code = EventTemplateCode.MILITARY;
        } else if (isAlien) {
            code = degree.getSigla().equals("MOTU") ? EventTemplateCode.INTERNATIONAL_MOTU : EventTemplateCode.INTERNATIONAL;
        } else if (protocol.isMobilityAgreement()) {
            code = null;
        } else {
            code = EventConfig.degreeEventTemplateMap().get(degree);
        }
        return code == null ? null : code.eventTemplate();
    }

}
