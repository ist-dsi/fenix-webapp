package pt.ist.fenix.webapp.task.institutional.banks.santander;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fenixedu.academic.domain.DomainOperationLog;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcess;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.connect.domain.Identity;
import org.fenixedu.connect.domain.identification.IdentificationDocument;
import org.fenixedu.connect.domain.identification.PersonalInformation;
import org.fenixedu.connect.domain.identification.TaxInformation;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;
import pt.ist.fenixedu.integration.domain.CardDataAuthorizationLog;

import java.io.ByteArrayOutputStream;

public class ExportDataForSantander extends ReadCustomTask {

    private final DateTime THREASHHOLD = new DateTime(2020, 9, 1, 0, 0, 0);

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Data");
        Bennu.getInstance().getUserSet().stream()
                .filter(user -> user.getPerson() != null)
                .filter(user-> allowedExport(user))
                .forEach(user -> {
                    final Spreadsheet.Row row = spreadsheet.addRow();
                    row.setCell("Tecnico ID", user.getUsername());
                    row.setCell("Nome", user.getProfile().getFullName());
                    row.setCell("e-mail", user.getEmail());
                    row.setCell("Telefone/Telemóvel", mobileFor(user));
                    row.setCell("Relação com a instituição", relation(user));
                    row.setCell("Nome Completo", user.getProfile().getFullName());
                    row.setCell("Número de documento de identificação", documentNumber(user));
                    row.setCell("Nacionalidade", nationality(user));
                    row.setCell("Morada de residência", address(user));
                    row.setCell("Ano lectivo de inscrição", executionYearStart(user));
                    row.setCell("Tipo e Nome do Curso", degreeName(user));
                });
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(stream);
        output("data.xls", stream.toByteArray());
    }

    private String degreeName(final User user) {
        final Student student = user.getPerson().getStudent();
        if (student != null) {
            final Registration registration = student.getRegistrationsSet().stream()
                    .filter(r -> r.isActive())
                    .filter(r -> r.hasAnyEnrolmentsIn(ExecutionYear.readCurrentExecutionYear()))
                    .findAny().orElse(null);
            if (registration != null) {
                return registration.getDegree().getPresentationName();
            }
        }
        final PhdIndividualProgramProcess phd = user.getPerson().getPhdIndividualProgramProcessesSet().stream()
                .filter(p -> p.isActive(ExecutionYear.readCurrentExecutionYear().getAcademicInterval().toInterval()))
                .findAny().orElse(null);
        if (phd != null) {
            return phd.getPhdProgram().getPresentationName(ExecutionYear.readCurrentExecutionYear());
        }
        return "";
    }

    private String executionYearStart(final User user) {
        final Student student = user.getPerson().getStudent();
        if (student != null) {
            final Registration registration = student.getRegistrationsSet().stream()
                    .filter(r -> r.isActive())
                    .filter(r -> r.hasAnyEnrolmentsIn(ExecutionYear.readCurrentExecutionYear()))
                    .findAny().orElse(null);
            if (registration != null) {
                return registration.getStartExecutionYear().getName();
            }
        }
        final PhdIndividualProgramProcess phd = user.getPerson().getPhdIndividualProgramProcessesSet().stream()
                .filter(p -> p.isActive(ExecutionYear.readCurrentExecutionYear().getAcademicInterval().toInterval()))
                .findAny().orElse(null);
        if (phd != null) {
            final LocalDate localDate = phd.getWhenStartedStudies();
            if (localDate != null) {
                return ExecutionYear.getExecutionYearByDate(new YearMonthDay(localDate.getYear(), localDate.getMonthOfYear(), localDate.getDayOfMonth()))
                        .getName();
            }
        }
        return "";
    }

    private String relation(final User user) {
        final StringBuilder builder = new StringBuilder();
        if (Group.parse("activeStudents").isMember(user)
                || Group.parse("activePhdProcessesGroup").isMember(user)) {
            builder.append("Aluno");
        }
        if (Group.parse("activeTeachers").isMember(user)) {
            builder.append("Docente");
        }
        if (Group.parse("activeEmployees").isMember(user)
                || Group.parse("activeGrantOwner").isMember(user)
                || Group.parse("activeResearchers").isMember(user)) {
            builder.append("Colaborador");
        }
        return builder.toString();
    }

    private String mobileFor(final User user) {
        final Identity identity = user.getIdentity();
        if (identity != null) {
            final String result = identity.getAccountSet().stream()
                    .map(account -> account.getMobile())
                    .filter(mobile -> mobile != null)
                    .findAny().orElse(null);
            if (result != null) {
                return result;
            }
        }
        return user.getPerson().getPartyContactsSet().stream()
                .filter(pc -> pc.isMobile() || pc.isPhone())
                .map(pc -> pc.getPresentationValue())
                .findAny().orElse("");
    }

    private String documentNumber(User user) {
        final Identity identity = user.getIdentity();
        if (identity != null) {
            final PersonalInformation personalInformation = identity.getPersonalInformation();
            if (personalInformation != null) {
                final IdentificationDocument identificationDocument = personalInformation.getIdentificationDocument();
                if (identificationDocument != null) {
                    return identificationDocument.getDocumentNumber();
                }
            }
        }
        return user.getPerson().getDocumentIdNumber();
    }

    private String nationality(final User user) {
        final Identity identity = user.getIdentity();
        if (identity != null) {
            final PersonalInformation personalInformation = identity.getPersonalInformation();
            if (personalInformation != null) {
                return personalInformation.getNationalityCountryCode();
            }
        }
        return user.getPerson().getNationality().getCode();
    }

    private String address(final User user) {
        final Identity identity = user.getIdentity();
        if (identity != null) {
            final PersonalInformation personalInformation = identity.getPersonalInformation();
            if (personalInformation != null) {
                final TaxInformation taxInformation = personalInformation.getTaxInformation();
                if (taxInformation != null) {
                    final String addressData = taxInformation.getAddressData();
                    if (addressData != null && !addressData.isEmpty()) {
                        final JsonObject address = new JsonParser().parse(addressData).getAsJsonObject();
                        final StringBuilder result = new StringBuilder();
                        final String firstLine = address.get("firstLine").getAsString();
                        result.append(firstLine);
                        final String secondLine = address.get("secondLine").getAsString();
                        result.append(", ");
                        result.append(secondLine);
                        final String zipCode = address.get("zipCode").getAsString();
                        result.append(", ");
                        result.append(zipCode);
                        final String location = address.get("location").getAsString();
                        result.append(", ");
                        result.append(location);
                        final String countryCode = address.get("countryCode").getAsString();
                        result.append(", ");
                        result.append(countryCode);
                        if (result.length() != 0) {
                            return result.toString();
                        }
                    }
                }
            }
        }
        return user.getPerson().getAddress();
    }

    private boolean allowedExport(User user) {
        final String answer = user.getPerson().getDomainOperationLogsSet().stream()
                .filter(CardDataAuthorizationLog.class::isInstance)
                .map(CardDataAuthorizationLog.class::cast)
                .filter(log -> "Santander - abertura de conta".equals(log.getTitle()))
                .sorted(DomainOperationLog.COMPARATOR_BY_WHEN_DATETIME)
                .filter(log -> log.getWhenDateTime().isAfter(THREASHHOLD))
                .map(log -> log.getAnswer())
                .findFirst().orElse(null);
        return "Sim".equals(answer);
    }
}
