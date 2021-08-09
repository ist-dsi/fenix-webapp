package pt.ist.fenix.webapp.task.accounting.report;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.calculator.CreditEntry;
import org.fenixedu.academic.domain.accounting.calculator.DebtExemption;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.domain.accounting.events.EventExemptionJustificationType;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.giaf.invoices.ClientMap;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixedu.giaf.invoices.Utils;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DiffReportOpenEvents extends CustomTask {

    private final DateTime instant = new DateTime(2021, 1, 1, 0, 0, 0, 0);
    private final LocalDate instantDate = instant.toLocalDate();
    private final DateTime now = new DateTime();

    @Override
    public void runTask() throws Exception {
        final Set<Object[]> result = Bennu.getInstance().getAccountingEventsSet().stream().parallel()
                .map(this::process)
                .filter(o -> o != null)
                .collect(Collectors.toSet());
        final Spreadsheet spreadsheet = new Spreadsheet("OpenDebts" + instant.getYear());
        result.forEach(o -> {
            final Spreadsheet.Row row = spreadsheet.addRow();
            final String product = (String) o[5];
            final BigDecimal due = (BigDecimal) o[2];
            final BigDecimal sapDue = (BigDecimal) o[3];
            final String academicYear = (String) o[14];
            row.setCell("Event", (String) o[0]);
            row.setCell("ClientID", (String) o[1]);
            row.setCell("Amount Due", due);
            row.setCell("SAP Amount Due", sapDue);
            row.setCell("Interest Due", (BigDecimal) o[4]);
            row.setCell("Product", product);
            row.setCell("Created", (String) o[6]);
            row.setCell("DueDate", (String) o[7]);
            row.setCell("Initial Debt", (BigDecimal) o[8]);
            row.setCell("Exempt", (BigDecimal) o[9]);
            row.setCell("Payed", (BigDecimal) o[10]);
            row.setCell("Payed Interest", (BigDecimal) o[11]);
            row.setCell("user", (String) o[12]);
            row.setCell("name", (String) o[13]);
            row.setCell("Event Description", (String) o[14]);
            row.setCell("Academic Year", academicYear);
            row.setCell("Is Canceled", (String) o[16]);
            row.setCell("Error", (String) o[17]);
            row.setCell("Nationality", (String) o[18]);
        });
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(stream);
        output("OpenDebts_" + instant.getYear() + ".xls", stream.toByteArray());
    }

    private Stream<Object[]> process(final Object[] eventLine) {
        try {
            return FenixFramework.getTransactionManager().withTransaction(() -> {
                final Event event = FenixFramework.getDomainObject((String) eventLine[0]);
                return event.getSapRequestSet().stream()
                        .filter(sr -> sr.isInitialization() || documentDateFor(sr).getYear() < instant.getYear())
                        .map(sr -> export(sr))
                        .collect(Collectors.toSet()).stream();
            }, new AtomicInstance(Atomic.TxMode.READ, true));
        } catch (final Exception e) {
            throw new Error(e);
        }
    }

    private DateTime documentDateFor(final SapRequest sapRequest) {
        final SapRequestType sapRequestType = sapRequest.getRequestType();
        final DateTime documentDate;
        if (sapRequestType == SapRequestType.DEBT || sapRequestType == SapRequestType.DEBT_CREDIT) {
            documentDate = documentDateFor(sapRequest, "workingDocument", "documentDate");
        } else if (sapRequestType == SapRequestType.INVOICE || sapRequestType == SapRequestType.INVOICE_INTEREST) {
            documentDate = documentDateFor(sapRequest, "workingDocument", "documentDate");
        } else if (sapRequestType == SapRequestType.CREDIT) {
            documentDate = documentDateFor(sapRequest, "workingDocument", "documentDate");
        } else if (sapRequestType == SapRequestType.PAYMENT || sapRequestType == SapRequestType.PAYMENT_INTEREST
                || sapRequestType == SapRequestType.ADVANCEMENT || sapRequestType == SapRequestType.CLOSE_INVOICE) {
            documentDate = documentDateFor(sapRequest, "paymentDocument", "paymentDate");
        } else if (sapRequestType == SapRequestType.REIMBURSEMENT) {
            documentDate = documentDateFor(sapRequest, "paymentDocument", "paymentDate");
        } else {
            throw new Error("unreachable code");
        }
        return documentDate;
    }

    private DateTime documentDateFor(final SapRequest sr, final String workingDocument, final String documentDate) {
        try {
            final String s = sr.getRequestAsJson().get(workingDocument).getAsJsonObject().get(documentDate).getAsString();
            return new DateTime(Integer.parseInt(s.substring(0, 4)), Integer.parseInt(s.substring(5, 7)), Integer.parseInt(s.substring(8, 10)),
                    Integer.parseInt(s.substring(11, 13)), Integer.parseInt(s.substring(14, 15)), Integer.parseInt(s.substring(17, 19)));
        } catch (NullPointerException ex) {
            taskLog("NPE: %s %s %s %n", sr.getEvent().getExternalId(), sr.getDocumentNumber(), sr.getRequest());
            throw ex;
        }
    }

    private Object[] export(final SapRequest sr) {
        return new Object[] {
                sr.getEvent().getExternalId(),
                sr.getSapDocumentNumber(),
                sr.getDocumentNumber(),
                sr.getRequestType().name(),
                sr.getValue().toPlainString(),
                sr.getAdvancement().toPlainString(),
                Boolean.toString(sr.isInitialization()),
                Boolean.toString(sr.getIntegrated()),
                Boolean.toString(sr.getOriginalRequest() != null),
        };
    }

    private Object[] process(final Event event) {
        try {
            return FenixFramework.getTransactionManager().withTransaction(() -> {
                final DateTime eventDate = event.getWhenOccured();
                if (eventDate.isBefore(instant)) {
                    final DebtInterestCalculator calculator = event.getDebtInterestCalculator(now);
                    final BigDecimal payedDebt = calculator.getPayments()
                            .filter(p -> p.getDate().isBefore(instantDate))
                            .map(p -> p.getUsedAmountInDebts())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    final BigDecimal interest = calculator.getPayments()
                            .filter(p -> p.getDate().isBefore(instantDate))
                            .map(p -> p.getUsedAmountInFines().add(p.getUsedAmountInInterests()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    final BigDecimal exempt = calculator.getCreditEntries().stream()
                            .filter(ce -> ce instanceof DebtExemption)
                            .filter(ce -> EventExemptionJustificationType.CUSTOM_PAYMENT_PLAN.name().equals(ce.getDescription()))
                            .filter(ce -> ce.getDate().isBefore(instantDate) || isNA0(event, ce))
                            .map(ce -> ce.getUsedAmountInDebts())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    final BigDecimal debt = calculator.getDebtAmount();
                    final BigDecimal dueAmount = debt.subtract(payedDebt).subtract(exempt);
                    final DebtInterestCalculator instantCalculator = event.getDebtInterestCalculator(instant);
                    final BigDecimal dueInterest = instantCalculator.getInterestAmount().add(instantCalculator.getFineAmount())
                            .subtract(interest);

                    if (dueAmount.signum() > 0) {
                        final String description = event.getDescription().toString();
                        final LocalDate dueDate = new LocalDate(Utils.getDueDate(event));
                        final Money sapDebt = event.getSapRequestSet().stream()
                                .filter(sr -> sr.getRequestType() == SapRequestType.INVOICE)
                                .filter(sapRequest -> sapRequest.getDocumentDate().getYear() < instant.getYear())
                                .map(sr -> sr.getValue())
                                .reduce(Money.ZERO, Money::add);
                        final Money sapCredit = event.getSapRequestSet().stream()
                                .filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT
                                        || sr.getRequestType() == SapRequestType.ADVANCEMENT
                                        || sr.getRequestType() == SapRequestType.CREDIT)
                                .filter(sapRequest -> sapRequest.getDocumentDate().getYear() < instant.getYear())
                                .map(sr -> sr.getValue())
                                .reduce(Money.ZERO, Money::add);
                        final Money sapOpenDebt = sapDebt.subtract(sapCredit);
                        if (!sapOpenDebt.getAmount().equals(dueAmount)) {
                            return new Object[] {
                                    event.getExternalId(),
                                    ClientMap.uVATNumberFor(event.getParty()),
                                    dueAmount,
                                    sapOpenDebt.getAmount(),
                                    dueInterest,
                                    SapEvent.mapToProduct(event, description, false, false, false, false).getValue(),
                                    eventDate.toString("yyyy-MM-dd"),
                                    dueDate.toString("yyyy-MM-dd"),
                                    debt,
                                    exempt,
                                    payedDebt,
                                    interest,
                                    username(event),
                                    event.getParty().getName(),
                                    description,
                                    Utils.executionYearOf(event).getYear(),
                                    Boolean.toString(event.isCancelled()),
                                    " ",
                                    nationalityFor(event.getParty())
                            };
                        }
                    }
                }

                return null;
            }, new AtomicInstance(Atomic.TxMode.READ, true));
        } catch (final Exception e) {
            return new Object[] {
                    event.getExternalId(),
                    " ",
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    " ",
                    " ",
                    " ",
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    " ",
                    " ",
                    " ",
                    " ",
                    " ",
                    e.getMessage() + " " + e.getClass().getSimpleName(),
                    " "
            };
        }
    }

    private String nationalityFor(final Party party) {
        if (party != null) {
            final Country country = party.getNationality();
            if (country != null) {
                return country.getCode();
            }
        }
        return " ";
    }

    private boolean isNA0(final Event event, final CreditEntry ce) {
        final String creditId = ce.getId();
        return event.getSapRequestSet().stream()
                .anyMatch(sr -> sr.isInitialization() && sr.getRequestType() == SapRequestType.CREDIT
                        && creditId.equals(sr.getCreditId()));
    }

    private String username(final Event event) {
        final Person person = event.getPerson();
        final User user = person == null ? null : person.getUser();
        return user == null ? " " : user.getUsername();
    }

}