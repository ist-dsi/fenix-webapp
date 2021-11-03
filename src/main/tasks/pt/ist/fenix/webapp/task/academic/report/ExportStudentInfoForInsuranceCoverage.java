package pt.ist.fenix.webapp.task.academic.report;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.connect.domain.Account;
import org.fenixedu.connect.domain.Identity;
import org.fenixedu.connect.domain.identification.Gender;
import org.fenixedu.connect.domain.identification.IdentificationDocument;
import org.fenixedu.connect.domain.identification.PersonalInformation;
import org.fenixedu.connect.domain.identification.TaxInformation;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import pt.ist.standards.geographic.Country;

import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.stream.Stream;

public class ExportStudentInfoForInsuranceCoverage extends ReadCustomTask {

    private static final Locale PT = new Locale("pt", "PT");
    private static final LocalDate RANDOM_DATE = new LocalDate(2020, 9, 29);

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("students.xls");

        final ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();

        Stream.concat(executionYear.getPhdIndividualProgramProcessesSet().stream().map(phd -> phd.getPerson()),
                executionYear.getExecutionPeriodsSet().stream()
                        .flatMap(executionSemester -> executionSemester.getEnrolmentsSet().stream())
                        .map(enrolment -> enrolment.getRegistration().getStudent().getPerson()))
                .distinct()
//                .filter(person -> isAfterRandomDate(person))
                .peek(person -> {
                    if (person.getUser() == null || person.getUser().getIdentity() == null) {
                        taskLog("Skipping person with no identity: %s = %s%n", person.getUsername(), person.getName());
                    }
                })
                .filter(person -> person.getUser() != null && person.getUser().getIdentity() != null)
                .forEach(person -> {
                    final Identity identity = person.getUser().getIdentity();
                    final PersonalInformation personalInformation = identity.getPersonalInformation();
                    final TaxInformation taxInformation = personalInformation.getTaxInformation();
                    final String tin = taxInformation == null ? person.getSocialSecurityNumber() : taxInformation.getTin();
                    final LocalDate dateOfBirth = personalInformation.getDateOfBirth();
                    final Gender gender = personalInformation.getGender();
                    final String addressData = taxInformation == null ? null : taxInformation.getAddressData();
                    final IdentificationDocument identificationDocument = personalInformation.getIdentificationDocument();
                    final Country country = personalInformation.getNationalityCountry();;

                    final Spreadsheet.Row row = spreadsheet.addRow();
                    row.setCell("Nome", personalInformation.getFullName());
                    row.setCell("NIF", tin != null && tin.startsWith("PT") ? tin.substring(2) : "");
                    row.setCell("Data Nascimento", dateOfBirth == null ? "" : dateOfBirth.toString("yyyy-MM-dd"));
                    row.setCell("Género", gender == Gender.FEMALE ? "F" : gender == Gender.MALE ? "M" : "-");
                    row.setCell("Morada", address(addressData));
                    row.setCell("Código Postal", zipCode(addressData));
                    row.setCell("Localidade", location(addressData));
                    row.setCell("País de Origem", country == null ? "" : country.getLocalizedName(PT));
                    row.setCell("Passaporte", identificationDocument == null ? "" : identificationDocument.getDocumentNumber());
                    row.setCell("NIF (País de Origem)", tin == null || tin.startsWith("PT") ? "" : tin.substring(2));
                    row.setCell("Telmóvel", findPhone(identity));
                    row.setCell("E-mail", findEmail(identity));
                });

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(stream);
        output("students.xls", stream.toByteArray());
    }

    private boolean isAfterRandomDate(final Person person) {
        final Student student = person.getStudent();
        if (student != null) {
            final DateTime firstEnrolment = student.getRegistrationsSet().stream()
                    .flatMap(registration -> registration.getEnrolments(ExecutionYear.readCurrentExecutionYear()).stream())
                    .map(enrolment -> enrolment.getCreationDateDateTime())
                    .min((dt1, dt2) -> dt1.compareTo(dt2))
                    .orElse(null);
            if (firstEnrolment != null && firstEnrolment.toLocalDate().isAfter(RANDOM_DATE)) {
                return true;
            }
        }
        if (person.getPhdIndividualProgramProcessesSet().stream()
                .map(phd -> phd.getWhenStartedStudies())
                .anyMatch(when -> when != null && when.isAfter(RANDOM_DATE))) {
            return true;
        }
        return false;
    }

    private String address(String addressData) {
        final StringBuilder result = new StringBuilder();
        if (addressData != null && !"TODO".equals(addressData)) {
            final JsonObject address = new JsonParser().parse(addressData).getAsJsonObject();

            final String firstLine = JsonUtils.get(address, "firstLine");
            if (firstLine != null) {
                result.append(firstLine);
                final String secondLine = JsonUtils.get(address, "secondLine");
                if (secondLine != null && !secondLine.isEmpty()) {
                    result.append(", ");
                    result.append(secondLine);
                }
            }
        }
        return result.toString();
    }

    private String zipCode(String addressData) {
        if (addressData != null && !"TODO".equals(addressData)) {
            final JsonObject address = new JsonParser().parse(addressData).getAsJsonObject();
            return JsonUtils.get(address, "zipCode");
        }
        return "";
    }

    private String location(String addressData) {
        if (addressData != null && !"TODO".equals(addressData)) {
            final JsonObject address = new JsonParser().parse(addressData).getAsJsonObject();
            return JsonUtils.get(address, "location");
        }
        return "";
    }

    private String findPhone(final Identity identity) {
        return identity.getAccountSet().stream()
                .map(account -> account.getMobile())
                .filter(phone -> phone != null)
                .findAny().orElse("");
    }

    private String findEmail(final Identity identity) {
        final Account account = identity.getUser().getAccount();
        if (account != null && !account.getEmail().startsWith("ist")) {
            return account.getEmail();
        }
        return identity.getAccountSet().stream()
                .map(a -> a.getEmail())
                .filter(phone -> phone != null)
                .findAny().orElse("");
    }

    private PhysicalAddress findPhysicalAddress(final Person person) {
        final PhysicalAddress physicalAddress = person.getDefaultPhysicalAddress();
        return physicalAddress == null ? person.getPhysicalAddresses().stream().findAny().orElse(null)
                : physicalAddress;
    }

}
