package pt.ist.fenix.webapp.task.academic.enrolment;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.events.EnrolmentEvaluationEvent;
import org.fenixedu.academic.domain.accounting.events.EventExemptionJustificationType;
import org.fenixedu.academic.dto.accounting.CreateExemptionBean;
import org.fenixedu.academic.ui.spring.service.AccountingManagementService;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;

public class ExempltExtraordinarySeasonEnrolments extends CustomTask {

    private final AccountingManagementService accountingManagementService = new AccountingManagementService();

    @Override
    public void runTask() throws Exception {
        ExecutionYear.readCurrentExecutionYear().getExecutionPeriodsSet().stream()
                .peek(es -> taskLog("Processing %s%n", es.getQualifiedName()))
                .flatMap(es -> es.getEnrolmentsSet().stream())
                .flatMap(enrolment -> enrolment.getEvaluationsSet().stream())
                .filter(ee -> ee.getEvaluationSeason().isExtraordinary())
                //.peek(ee -> taskLog("Checking evaluation %s%n", ee.getExternalId()))
                .map(ee -> ee.getEnrolmentEvaluationEvent())
                .filter(event -> event != null)
                .forEach(this::fix);
    }

    private void fix(final EnrolmentEvaluationEvent event) {
        final EnrolmentEvaluation evaluation = event.getEnrolmentEvaluation();
        final Enrolment enrolment = evaluation.getEnrolment();
        final Event otherEvent = enrolment.getEvaluationsSet().stream()
                .filter(e -> e != evaluation)
                .map(e -> e.getEnrolmentEvaluationEvent())
                .filter(e -> e != null && !e.isCancelled())
                .findAny().orElse(null);

        if (event.getAccountingTransactionsSet().isEmpty()) {
            if (otherEvent == null) {
                taskLog("Keeping event for %s : %s%n",
                        enrolment.getRegistration().getNumber(),
                        enrolment.getCurricularCourse().getName());
            } else {
                taskLog("Exempting event for %s : %s%n",
                        enrolment.getRegistration().getNumber(),
                        enrolment.getCurricularCourse().getName());
                final CreateExemptionBean bean = new CreateExemptionBean();
                bean.setExemptionType(CreateExemptionBean.ExemptionType.DEBT);
                bean.setJustificationType(EventExemptionJustificationType.DIRECTIVE_COUNCIL_AUTHORIZATION);
                bean.setDispatchDate(new DateTime());
                bean.setReason("Valor já emitido na época especial");
                bean.setValue(event.getOriginalAmountToPay());
                accountingManagementService.exemptEvent(event, User.findByUsername("ist24439").getPerson(), bean);
            }
        } else {
            if (otherEvent == null) {
                // All is ok
            } else {
                taskLog("Refunding event for %s : %s%n",
                        enrolment.getRegistration().getNumber(),
                        enrolment.getCurricularCourse().getName());
                accountingManagementService.refundEvent(event, User.findByUsername("ist24439"),
                        EventExemptionJustificationType.DIRECTIVE_COUNCIL_AUTHORIZATION,
                        "Valor já emitido na época especial", event.getOriginalAmountToPay().getAmount());
            }
        }
    }

}
