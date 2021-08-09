package pt.ist.fenix.webapp.task.accounting.report;

import org.fenixedu.academic.domain.accounting.ResidenceEvent;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;

public class ReportLatestPaymentsFromPreviousYear extends CustomTask {
    @Override
    public void runTask() throws Exception {
        final Spreadsheet report = new Spreadsheet("Report");
        Bennu.getInstance().getAccountingTransactionsSet().stream()
                .filter(tx -> tx.getWhenRegistered().getYear() == 2020)
                .filter(tx -> tx.getWhenProcessed().getYear() == 2021)
                .filter(tx -> !tx.isAdjustingTransaction())
                .filter(tx -> tx.getAmountWithAdjustment().isPositive())
                .filter(tx -> !(tx.getEvent() instanceof ResidenceEvent))
                .forEach(tx -> {
                    final Spreadsheet.Row row = report.addRow();
                    row.setCell("Event", "https://fenix.tecnico.ulisboa.pt/accounting-management/" + tx.getEvent().getExternalId() + "/details");
                    row.setCell("Event Date", tx.getEvent().getWhenOccured().toString("yyyy-MM-dd"));
                    row.setCell("Payment Date", tx.getWhenRegistered().toString("yyyy-MM-dd"));
                    row.setCell("When Registered", tx.getWhenProcessed().toString("yyyy-MM-dd HH:mm"));
                    row.setCell("Amount", tx.getAmountWithAdjustment().toPlainString());
                    row.setCell("Via", tx.getPaymentMethod().getLocalizedName());
                    row.setCell("User", user(tx.getResponsibleUser()));
                });
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        report.exportToXLSSheet(stream);
        output("report.xls", stream.toByteArray());

    }

    private String user(final User user) {
        return user == null ? "" : (user.getProfile().getDisplayName() + "(" + user.getUsername() + ")");
    }

}
