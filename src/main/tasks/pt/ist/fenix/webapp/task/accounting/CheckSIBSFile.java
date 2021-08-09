package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.AccountingTransactionDetail;
import org.fenixedu.academic.domain.accounting.ResidenceEvent;
import org.fenixedu.academic.domain.accounting.accountingTransactions.detail.SibsTransactionDetail;
import org.fenixedu.academic.util.Money;
import org.fenixedu.academic.util.sibs.incomming.SibsIncommingPaymentFileDetailLine;
import org.fenixedu.academic.util.sibs.incomming.SibsIncommingPaymentFileHeader;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.joda.time.YearMonthDay;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;

import java.io.ByteArrayOutputStream;
import java.util.stream.Collectors;

public class CheckSIBSFile extends ReadCustomTask {
    @Override
    public void runTask() throws Exception {
        final Money sibsValue = Bennu.getInstance().getAccountingTransactionsSet().stream()
                .filter(tx -> tx.getWhenRegistered().getYear() == 2020)
                .filter(tx -> tx.getWhenRegistered().getMonthOfYear() == 12)
                .filter(tx -> tx.getTransactionDetail() instanceof SibsTransactionDetail)
                .filter(tx -> matchesDay(tx))
                .filter(tx -> !(tx.getEvent() instanceof ResidenceEvent))
                .map(tx -> tx.getOriginalAmount())
                .reduce(Money.ZERO, Money::add);
        final Money sibsResidenceValue = Bennu.getInstance().getAccountingTransactionsSet().stream()
                .filter(tx -> tx.getWhenRegistered().getYear() == 2020)
                .filter(tx -> tx.getWhenRegistered().getMonthOfYear() == 12)
                .filter(tx -> tx.getTransactionDetail() instanceof SibsTransactionDetail)
                .filter(tx -> matchesDay(tx))
                .filter(tx -> tx.getEvent() instanceof ResidenceEvent)
                .map(tx -> tx.getOriginalAmount())
                .reduce(Money.ZERO, Money::add);

        taskLog("SIBS: %s%n", sibsValue.toPlainString());
        taskLog("SIBS Residence: %s%n", sibsResidenceValue.toPlainString());

        final Spreadsheet report = new Spreadsheet("Report");
        Bennu.getInstance().getAccountingTransactionsSet().stream()
                .filter(tx -> tx.getWhenRegistered().getYear() == 2020)
//                .filter(tx -> tx.getWhenRegistered().getMonthOfYear() == 12)
//              .filter(tx -> tx.getTransactionDetail() instanceof SibsTransactionDetail)
//                .filter(tx -> matchesDay(tx))
                .filter(tx -> !(tx.getEvent() instanceof ResidenceEvent))
                .forEach(tx -> {
                    final Money diff = tx.getOriginalAmount().subtract(tx.getSapRequestSet().stream()
                            .filter(sr -> isPayment(sr))
                            .map(sr -> sr.getValue().add(sr.getAdvancement()))
                            .reduce(Money.ZERO, Money::add));
                    if (diff.isPositive()) {
                        final Spreadsheet.Row row = report.addRow();
                        row.setCell("Event", "https://fenix.tecnico.ulisboa.pt/accounting-management/" + tx.getEvent().getExternalId() + "/details");
                        row.setCell("TX", tx.getExternalId());
                        row.setCell("Is Adjusted", Boolean.toString(tx.isAdjustingTransaction() || !tx.getAdjustmentTransactionsSet().isEmpty()));
                        row.setCell("Original Amount", tx.getOriginalAmount().toPlainString());
                        row.setCell("Adjusted Amount", tx.getAmountWithAdjustment().toPlainString());
                        row.setCell("SAP Value", tx.getSapRequestSet().stream()
                                .filter(sr -> isPayment(sr))
                                .map(sr -> sr.getValue().add(sr.getAdvancement()))
                                .reduce(Money.ZERO, Money::add).toPlainString());
                        row.setCell("SAP Document(s)", tx.getSapRequestSet().stream()
                                .filter(sr -> isPayment(sr))
                                .map(sr -> sr.getDocumentNumber())
                                .collect(Collectors.joining(", ")));
                        row.setCell("Diff", diff.toPlainString());
                    }
                });
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        report.exportToXLSSheet(stream);
        output("checkSibsDay.xls", stream.toByteArray());
    }

    private boolean isPayment(final SapRequest sr) {
        final SapRequestType type = sr.getRequestType();
        return type == SapRequestType.PAYMENT || type == SapRequestType.PAYMENT_INTEREST || type == SapRequestType.ADVANCEMENT;
    }

    private boolean matchesDay(final AccountingTransaction tx) {
        final YearMonthDay sibDate = sibsDate(tx.getTransactionDetail());
        return sibDate != null && sibDate.getYear() == 2020 && sibDate.getMonthOfYear() == 12 && sibDate.getDayOfMonth() == 23;
    }

    private YearMonthDay sibsDate(final AccountingTransactionDetail transactionDetail) {
        final SibsTransactionDetail sibsTx = (SibsTransactionDetail) transactionDetail;
        final SibsIncommingPaymentFileDetailLine line = sibsTx.getSibsLine();
        if (line == null) {
            taskLog("No sibs line for transaction detail: %s%n", transactionDetail.getExternalId());
            return null;
        } else {
            final SibsIncommingPaymentFileHeader header = line.getHeader();
            return header.getWhenProcessedBySibs();
        }
    }

}
