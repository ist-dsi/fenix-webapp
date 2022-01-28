package pt.ist.fenix.webapp.task;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.jsonwebtoken.SignatureAlgorithm;
import kong.unirest.HttpResponse;
import kong.unirest.MultipartBody;
import kong.unirest.Unirest;
import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.EntryPhase;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.SapSdkConfiguration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.io.domain.DriveAPIStorage;
import org.fenixedu.bennu.io.domain.FileSupport;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.connect.domain.ConnectSystem;
import org.fenixedu.connect.domain.identification.IdentificationDocument;
import org.fenixedu.connect.domain.identification.PersonalInformation;
import org.fenixedu.connect.domain.identification.PortugueseCitizenCard;
import org.fenixedu.connect.domain.identification.PortugueseIdentityCard;
import org.fenixedu.connect.domain.identification.TaxInformation;
import org.fenixedu.jwt.Tools;
import pt.ist.fenixedu.contracts.domain.Employee;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.standards.geographic.Planet;
import pt.ist.standards.geographic.PostalCode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

@Task(englishTitle = "Script for dumping raw data to drive directory", readOnly = true)
public class DumpRawData extends CronTask {

    private static final String REPO_NODE_ID = "1414452990237579";

    @Override
    public void runTask() throws Exception {
        dumpIdentifiers();
        dumpDegrees();
        dumpRegistrations();
    }

    private void dumpIdentifiers() {
        final Spreadsheet spreadsheetUsernames = new Spreadsheet("Usernames");
        final Spreadsheet spreadsheetRegistrations = spreadsheetUsernames.addSpreadsheet("Registrations");
        final Spreadsheet spreadsheetOtherUsernames = spreadsheetRegistrations.addSpreadsheet("OtherUsernames");

        final Spreadsheet spreadsheetIdentificationDocuments = new Spreadsheet("IdDocuments");
        final Spreadsheet spreadsheetTaxInformation = new Spreadsheet("TaxInformation");

        process(ConnectSystem.getInstance().getIdentitySet(), identity -> {
            final User user = identity.getUser();
            final Person person = user == null ? null : user.getPerson();
            final Student student = person == null ? null : person.getStudent();
            final Employee employee = person == null ? null : person.getEmployee();
            final String username = user == null ? null : user.getUsername();
            final String[] sap = employee == null ? null : SapSdkConfiguration.usernameProvider().toSapNumbers(username);

            final Spreadsheet.Row rowUsernames = row(spreadsheetUsernames);
            rowUsernames.setCell("identity", identity.getExternalId());
            rowUsernames.setCell("username", username);
            rowUsernames.setCell("student", student == null ? null : student.getNumber());
            rowUsernames.setCell("collaborator", employee == null ? null : employee.getEmployeeNumber());
            rowUsernames.setCell("sapIST", sap == null ? null : sap[0]);
            rowUsernames.setCell("sapIST-ID", sap == null ? null : sap[1]);
            rowUsernames.setCell("sapADIST", sap == null ? null : sap[2]);
            rowUsernames.setCell("sapIDMEC", sap == null ? null : sap[3]);

            if (student != null) {
                student.getRegistrationsSet().forEach(registration -> {
                    final Spreadsheet.Row rowRegistrations = row(spreadsheetRegistrations);
                    rowRegistrations.setCell("username", username);
                    rowRegistrations.setCell("student", student.getNumber());
                    rowRegistrations.setCell("registration", registration.getNumber());
                });
            }

            identity.getAccountSet().stream()
                    .map(account -> account.getUser())
                    .filter(u -> u != null)
                    .map(u -> u.getUsername())
                    .filter(u -> !u.equals(username))
                    .forEach(otherUsername -> {
                        final Spreadsheet.Row rowOthers = row(spreadsheetOtherUsernames);
                        rowOthers.setCell("username", username);
                        rowOthers.setCell("otherUsername", otherUsername);
                    });

            final PersonalInformation personalInformation = identity.getPersonalInformation();
            final IdentificationDocument identificationDocument = personalInformation == null ? null
                    : personalInformation.getIdentificationDocument();
            if (identificationDocument != null) {
                final Spreadsheet.Row rowDocument = row(spreadsheetIdentificationDocuments);
                rowDocument.setCell("identity", identity.getExternalId());
                rowDocument.setCell("country", identificationDocument.getCountryCode());
                rowDocument.setCell("type", identificationDocument.getIdentificationDocumentName().getContent());
                rowDocument.setCell("number", identificationDocument.getDocumentNumber());
                if (identificationDocument instanceof PortugueseIdentityCard) {
                    final PortugueseIdentityCard identityCard = (PortugueseIdentityCard) identificationDocument;
                    rowDocument.setCell("extraDigit", identityCard.getExtraDigit());
                    if (identityCard instanceof PortugueseCitizenCard) {
                        final PortugueseCitizenCard citizenCard = (PortugueseCitizenCard) identityCard;
                        rowDocument.setCell("versionNumber", citizenCard.getVersionNumber());
                        rowDocument.setCell("secondExtraDigit", citizenCard.getSecondExtraDigit());
                    } else {
                        rowDocument.setCell("versionNumber", "");
                        rowDocument.setCell("secondExtraDigit", "");
                    }
                } else {
                    rowDocument.setCell("extraDigit", "");
                }
            }

            final TaxInformation taxInformation = personalInformation == null ? null
                    : personalInformation.getTaxInformation();
            if (taxInformation != null) {
                final Spreadsheet.Row rowTaxInfo = row(spreadsheetTaxInformation);
                rowTaxInfo.setCell("identity", identity.getExternalId());
                final String tin = taxInformation.getTin();
                if (tin == null || tin.isEmpty()) {
                    rowTaxInfo.setCell("country", "");
                    rowTaxInfo.setCell("tin", "");
                } else {
                    rowTaxInfo.setCell("country", tin.substring(0, 2));
                    rowTaxInfo.setCell("tin", tin.substring(2));
                }
                if (taxInformation.getAddressData() != null && !taxInformation.getAddressData().isEmpty()) {
                    final JsonObject addressData = JsonUtils.parse(taxInformation.getAddressData());
                    if (addressData != null) {
                        final String countryCode = JsonUtils.get(addressData, "countryCode");
                        final Country addressCountry = countryCode == null ? null : Country.readByTwoLetterCode(countryCode);
                        if (addressCountry != null) {
                            final String zipCode = JsonUtils.get(addressData, "zipCode");
                            if (zipCode != null) {
                                final String line1 = JsonUtils.get(addressData, "firstLine");
                                if (line1 != null && !line1.isEmpty()) {
                                    final String line2 = JsonUtils.get(addressData, "secondLine");
                                    final String address = line2 == null ? line1 : (line1 + ", " + line2);
                                    final String location = JsonUtils.get(addressData, "location");

                                    rowTaxInfo.setCell("address", address);
                                    rowTaxInfo.setCell("zipCode", zipCode);
                                    rowTaxInfo.setCell("location", location == null ? "" : location);
                                    rowTaxInfo.setCell("countryCode", countryCode);

                                    if ("PT".equals(countryCode)) {
                                        final PostalCode postalCode = Planet.getEarth().getByAlfa2(countryCode)
                                                .getPostalCode(zipCode);
                                        final JsonObject info = postalCode == null ? null : postalCode.getDetails();
                                        if (info != null) {
                                            rowTaxInfo.setCell("freguesia", JsonUtils.get(info, "Freguesia"));
                                            rowTaxInfo.setCell("concelho", JsonUtils.get(info, "Concelho"));
                                            rowTaxInfo.setCell("distrito", JsonUtils.get(info, "Distrito"));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });

        upload(spreadsheetUsernames, "identifiers.xlsx");
        upload(spreadsheetIdentificationDocuments, "identificationDocument.xlsx");
        upload(spreadsheetTaxInformation, "taxInformation.xlsx");
    }

    private void dumpDegrees() {
        final Spreadsheet spreadsheet = new Spreadsheet("Degrees");

        process(Bennu.getInstance().getDegreesSet(), degree -> {
            final Spreadsheet.Row row = row(spreadsheet);
            row.setCell("Code", degree.getSigla());
            row.setCell("Tyoe", degree.getDegreeType().getName().getContent());
            row.setCell("Name", degree.getNameI18N().getContent());
            row.setCell("MinistryCode", degree.getMinistryCode());
        });

        upload(spreadsheet, "degrees.xlsx");
    }

    private void dumpRegistrations() {
        final Spreadsheet spreadsheet = new Spreadsheet("Registrations");
        final Spreadsheet byYear = spreadsheet.addSpreadsheet("ByYear");
        final Spreadsheet states = byYear.addSpreadsheet("States");
        final Spreadsheet plans = states.addSpreadsheet("Plans");

        process(Bennu.getInstance().getRegistrationsSet(), registration -> {
            final Student student = registration.getStudent();
            final Person person = student.getPerson();
            final User user = person.getUser();
            final RegistrationProtocol protocol = registration.getRegistrationProtocol();
            final IngressionType ingressionType = registration.getIngressionType();
            final EntryPhase entryPhase = registration.getEntryPhase();

            final Spreadsheet.Row row = row(spreadsheet);
            row.setCell("registrationId", registration.getExternalId());
            row.setCell("username", user.getUsername());
            row.setCell("student", student.getNumber());
            row.setCell("registration", registration.getNumber());
            row.setCell("degree", registration.getDegree().getSigla());
            row.setCell("protocolCode", protocol == null ? "" : protocol.getCode());
            row.setCell("protocol", protocol == null ? "" : protocol.getDescription().getContent());
            row.setCell("ingressionTypeCode", ingressionType == null ? "" : ingressionType.getCode());
            row.setCell("ingressionType", ingressionType == null ? "" : ingressionType.getDescription().getContent());
            row.setCell("entryPhase", entryPhase == null ? "" : entryPhase.getLocalizedName());
            row.setCell("startDate", registration.getStartDate() == null ? "" : registration.getStartDate().toString("yyyy-MM-dd"));
            row.setCell("studiesStartDate", registration.getStudiesStartDate() == null ? "" : registration.getStudiesStartDate().toString("yyyy-MM-dd"));
            row.setCell("eventTemplate", registration.getEventTemplate() == null ? ""
                    : registration.getEventTemplate().getTitle().getContent());

            registration.getRegistrationDataByExecutionYearSet().forEach(dataByExecutionYear -> {
                final ExecutionYear executionYear = dataByExecutionYear.getExecutionYear();
                final Spreadsheet.Row rowByYear = row(byYear);
                rowByYear.setCell("registrationId", registration.getExternalId());
                rowByYear.setCell("executionYear", executionYear.getYear());
                rowByYear.setCell("enrolmentDate", dataByExecutionYear.getEnrolmentDate() == null ? ""
                        : dataByExecutionYear.getEnrolmentDate().toString("yyyy-MM-dd"));
                rowByYear.setCell("maxCreditsPerYear", dataByExecutionYear.getMaxCreditsPerYear());
                rowByYear.setCell("allowedSemesterForEnrolments", dataByExecutionYear.getAllowedSemesterForEnrolments() == null ? ""
                        : dataByExecutionYear.getAllowedSemesterForEnrolments().getQualifiedName());
                rowByYear.setCell("eventTemplate", dataByExecutionYear.getEventTemplate() == null ? ""
                        : dataByExecutionYear.getEventTemplate().getTitle().getContent());
                rowByYear.setCell("enrolmentModel", dataByExecutionYear.getEnrolmentModel() == null ? ""
                        : dataByExecutionYear.getEnrolmentModel().getLocalizedName());
                rowByYear.setCell("isReingression", Boolean.toString(dataByExecutionYear.getReingression()));
                rowByYear.setCell("reingressionDate", dataByExecutionYear.getReingressionDate() == null ? ""
                        : dataByExecutionYear.getReingressionDate().toString("yyyy-MM-dd"));
                rowByYear.setCell("enrolledCourses", Long.toString(registration.getStudentCurricularPlansSet().stream()
                        .flatMap(scp -> scp.getEnrolmentStream())
                        .filter(enrolment -> enrolment.getExecutionYear() == executionYear)
                        .count()));
                rowByYear.setCell("approvedCourses", Long.toString(registration.getStudentCurricularPlansSet().stream()
                        .flatMap(scp -> scp.getEnrolmentStream())
                        .filter(enrolment -> enrolment.getExecutionYear() == executionYear)
                        .filter(enrolment -> enrolment.isApproved())
                        .count()));
                rowByYear.setCell("enrolledCredits", Double.toString(registration.getStudentCurricularPlansSet().stream()
                        .flatMap(scp -> scp.getEnrolmentStream())
                        .filter(enrolment -> enrolment.getExecutionYear() == executionYear)
                        .mapToDouble(enrolment -> enrolment.getEctsCreditsForCurriculum().doubleValue())
                        .sum()));
                rowByYear.setCell("approvedCredits", Double.toString(registration.getStudentCurricularPlansSet().stream()
                        .flatMap(scp -> scp.getEnrolmentStream())
                        .filter(enrolment -> enrolment.getExecutionYear() == executionYear)
                        .filter(enrolment -> enrolment.isApproved())
                        .mapToDouble(enrolment -> enrolment.getEctsCreditsForCurriculum().doubleValue())
                        .sum()));
            });

            registration.getRegistrationStatesSet().forEach(registrationState -> {
                final Spreadsheet.Row rowState = row(states);
                rowState.setCell("registrationId", registration.getExternalId());
                rowState.setCell("executionYear", registrationState.getExecutionYear() == null ? "" : registrationState.getExecutionYear().getYear());
                rowState.setCell("stateDate", registrationState.getStateDate() == null ? "" : registrationState.getStateDate().toString("yyyy-MM-dd"));
                rowState.setCell("endDate", registrationState.getEndDate() == null ? "" : registrationState.getEndDate().toString("yyyy-MM-dd"));
                rowState.setCell("stateType", registrationState.getStateType().getName());
                rowState.setCell("state", registrationState.getStateType().getDescription());
                rowState.setCell("isActive", Boolean.toString(registrationState.isActive()));
            });

            registration.getStudentCurricularPlansSet().forEach(studentCurricularPlan -> {
                final DegreeCurricularPlan degreeCurricularPlan = studentCurricularPlan.getDegreeCurricularPlan();

                final Spreadsheet.Row rowPlan = row(plans);
                rowPlan.setCell("registrationId", registration.getExternalId());
                rowPlan.setCell("degreeCurricularPlan", degreeCurricularPlan.getName());
                rowPlan.setCell("startDate", studentCurricularPlan.getStartDateYearMonthDay() == null ? ""
                        : studentCurricularPlan.getStartDateYearMonthDay().toString("yyyy-MM-dd"));
                rowPlan.setCell("dismissalCredits", Double.toString(studentCurricularPlan.getRoot().getCurriculumLineStream()
                        .filter(curriculumLine -> !curriculumLine.isEnrolment())
                        .mapToDouble(line -> line.getEctsCreditsForCurriculum().doubleValue())
                        .sum()));
            });
        });

        upload(spreadsheet, "registrations.xlsx");
    }

    private Spreadsheet.Row row(final Spreadsheet spreadsheet) {
        synchronized (spreadsheet) {
            return spreadsheet.addRow();
        }
    }

    private <T> void process(final Set<T> set, final Consumer<T> consumer) {
        set.stream().parallel().forEach(t -> processAtomic(t, consumer));
    }

    private <T> void processAtomic(final T t, final Consumer<T> consumer) {
        try {
            FenixFramework.atomic(() -> consumer.accept(t));
        } catch (final Exception e) {
            throw new Error(e);
        }
    }

    private void upload(final Spreadsheet spreadsheet, final String filename) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            spreadsheet.exportToXLSSheet(baos);
        } catch (final IOException e) {
            throw new Error(e);
        }
        upload("", filename, baos.toByteArray());
    }

    private void upload(final String path, final String filename, final byte[] content) {
        final FileSupport fileSupport = FileSupport.getInstance();
        final DriveAPIStorage driveAPIStorage = fileSupport.getFileStorageSet().stream()
                .filter(DriveAPIStorage.class::isInstance)
                .map(DriveAPIStorage.class::cast)
                .findAny().orElseThrow(() -> new Error());

        final MultipartBody request = Unirest.post(driveAPIStorage.getDriveUrl() + "/api/drive/directory/" + REPO_NODE_ID)
                .header("Authorization", "Bearer " + getAccessToken(driveAPIStorage))
                .header("X-Requested-With", "XMLHttpRequest")
                .field("path", path);
        final Function<MultipartBody, MultipartBody> fileSetter = b -> b.field("file", content, filename);
        final HttpResponse<String> response = fileSetter.apply(request).asString();
        final JsonObject result = new JsonParser().parse(response.getBody()).getAsJsonObject();
        final JsonElement id = result.get("id");
        if (id == null || id.isJsonNull()) {
            throw new Error(result.toString());
        }
    }

    private transient String accessToken = null;
    private transient long accessTokenValidUnit = System.currentTimeMillis() - 1;

    private String getAccessToken(final DriveAPIStorage driveAPIStorage) {
        if (accessToken == null || System.currentTimeMillis() >= accessTokenValidUnit) {
            synchronized (this) {
                if (accessToken == null || System.currentTimeMillis() >= accessTokenValidUnit) {
                    final JsonObject claim = new JsonObject();
                    claim.addProperty("username", driveAPIStorage.getRemoteUsername());
                    accessToken = Tools.sign(SignatureAlgorithm.RS256, CoreConfiguration.getConfiguration().jwtPrivateKeyPath(), claim);
                }
            }
        }
        return accessToken;
    }

}