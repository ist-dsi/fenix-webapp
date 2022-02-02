package pt.ist.fenix.webapp.task;

import org.fenixedu.bennu.core.signals.DomainObjectEvent;
import org.fenixedu.bennu.core.signals.Signal;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.joda.time.DateTime;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.payments.domain.SibsPayment;
import pt.ist.payments.domain.SibsPaymentProgressStatus;
import pt.ist.payments.domain.SibsPaymentSystem;

@Task(englishTitle = "Recover lost SIBS payments", readOnly = true)
public class RecoverMissingSIBSTransactions extends CronTask {

    @Override
    public void runTask() throws Exception {
        SibsPaymentSystem.getInstance().getSibsPaymentSet().stream()
                .filter(sibsPayment -> sibsPayment.getSettlement() != null)
                .filter(sibsPayment -> sibsPayment.getAccountingTransaction() == null)
                .filter(sibsPayment -> sibsPayment.getEvent() != null)
                .forEach(this::fix);
    }

    private void fix(final SibsPayment sibsPayment) {
        FenixFramework.atomic(() -> {
            if (sibsPayment.getSettlement() != null
                    && sibsPayment.getAccountingTransaction() == null
                    && sibsPayment.getEvent() != null) {
                final String dateValueField = sibsPayment.getSettlement().split(",")[33];
                final DateTime timestamp = new DateTime(
                        Integer.valueOf(dateValueField.substring(0, 4)),
                        Integer.valueOf(dateValueField.substring(4, 6)),
                        Integer.valueOf(dateValueField.substring(6, 8)),
                        Integer.valueOf(dateValueField.substring(8, 10)),
                        Integer.valueOf(dateValueField.substring(10, 12)),
                        Integer.valueOf(dateValueField.substring(12, 14)));
                sibsPayment.setStatus(SibsPaymentProgressStatus.SUCCESSFUL);
                sibsPayment.setSibsLastTimestamp(timestamp);
                Signal.emit(SibsPayment.SUCCESSFUL_PAYMENT, new DomainObjectEvent<>(sibsPayment));
            }
        });
    }

}
