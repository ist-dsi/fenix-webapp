package pt.ist.fenix.webapp.task;

import com.google.common.io.ByteStreams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.BarcodeQRCode;
import com.itextpdf.text.pdf.PdfAnnotation;
import com.itextpdf.text.pdf.PdfFormField;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfString;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.bennu.RegistrationProcessConfiguration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.rest.JsonBodyReaderWriter;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.papyrus.domain.SignatureFieldSettings;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.commons.stream.StreamUtils;
import org.fenixedu.connect.domain.Account;
import org.fenixedu.connect.domain.ConnectSystem;
import org.fenixedu.connect.domain.Identity;
import org.fenixedu.connect.domain.identification.IdentificationDocument;
import org.fenixedu.connect.domain.identification.PersonalInformation;
import org.fenixedu.connect.domain.identification.TaxInformation;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;
import org.joda.time.DateTime;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.papyrus.PapyrusClient;
import pt.ist.registration.process.handler.CandidacySignalHandler;
import pt.ist.standards.geographic.Country;
import pt.ist.standards.geographic.Planet;

import javax.imageio.ImageIO;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

@Task(englishTitle = "Auto Generate Declarations for International Students", readOnly = true)
public class GenerateAdmissionsDocumentForSigning extends CronTask {

    private static final String TEMPLATE_ID = "admissions-international-students-admitted";
    private static final String SIGNING_QUEUE = "xNCC1IYd";
    private static final String LOG_FILE = "/afs/ist.utl.pt/ciist/fenix/fenix036/admissions-international-students-admitted.log";

    private static final Locale PT = new Locale("pt");
    private static final Locale EN = new Locale("en");

    @Override
    public void runTask() throws Exception {
        final Set<String> processed = load(LOG_FILE);
        try {
            AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                    .flatMap(admissionProcess -> admissionProcess.getAdmissionProcessTargetSet().stream())
                    .flatMap(admissionProcessTarget -> admissionProcessTarget.getApplicationSet().stream())
                    .filter(application -> application.getAdmitted())
                    .filter(application -> isPayed(application))
                    .forEach(application -> {
                        final String hash = calculateHashFor(application);
                        if (!processed.contains(hash)) {
                            try {
                                process(application);
                                processed.add(hash);
                            } catch (final Throwable t) {
                                final ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                final PrintStream printStream = new PrintStream(stream);
                                t.printStackTrace(printStream);
                                taskLog("Failled to process: %s : %s%n    %s%n", application.getExternalId(), t.getMessage(), new String(stream.toByteArray()));
                            }
                        }
                    });
        } finally {
            write(LOG_FILE, processed);
        }
    }

    private String calculateHashFor(final Application application) {
        final PersonalInformation personalInformation = ConnectSystem.getPersonalInformationFor(application.getAccount());
        final IdentificationDocument identificationDocument = personalInformation.getIdentificationDocument();
        final StringBuilder builder = new StringBuilder();
        builder.append(application.getExternalId());
        builder.append(personalInformation.getFullName());
        builder.append(personalInformation.getDateOfBirth().toString("yyyy-MM-dd"));
        builder.append(identificationDocument.getDocumentNumber());
        builder.append(personalInformation.getNationalityCountryCode());
        return Base64.getEncoder().encodeToString(builder.toString().getBytes());
    }

    private boolean isPayed(final Application application) {
        final JsonObject data = application.getDataObject();
        final JsonElement e = data.get("gratuityEvent");
        if (e != null && !e.isJsonNull()) {
            final Event event = FenixFramework.getDomainObject(e.getAsString()) ;
            final DebtInterestCalculator calculator = event.getDebtInterestCalculator(new DateTime());
            final BigDecimal paidDebtAmount = calculator.getPaidDebtAmount();
            final AdmissionProcessTarget target = application.getAdmissionProcessTarget();
            final Degree degree = FenixFramework.getDomainObject(target.getOutcomeConfigJson().get("degree").getAsString());
            final BigDecimal min = degree.getSigla().equals("MOTU") ? new BigDecimal("1000") : new BigDecimal("2000");
            return paidDebtAmount.doubleValue() >= min.doubleValue();
        }
        return false;
    }

    private void process(final Application application) {
        final String uuid = UUID.randomUUID().toString();
        final byte[] document = generate(TEMPLATE_ID, data(application, uuid), PT);
        final String title = titleFor(application);
        final User user = user(application);
        sendDocumentToBeSigned(SIGNING_QUEUE, title, title, title + ".pdf", new ByteArrayInputStream(document), uuid, user == null
                ? "ist24439" : user.getUsername());
        FenixFramework.atomic(() -> {
            final JsonObject data = application.getDataObject();
            JsonArray documents = data.getAsJsonArray("documents");
            if (documents == null || documents.isJsonNull()) {
                documents = new JsonArray();
                data.add("documents", documents);
            }
            final JsonObject jdoc = new JsonObject();
            documents.add(jdoc);
            jdoc.addProperty("url", "https://certifier.tecnico.ulisboa.pt/" + uuid + "/download");
            jdoc.addProperty("title", "Admission Declaration");
            application.setData(data.toString());
        });
    }

    private User user(final Application application) {
        final Identity identity = application.getAccount().getIdentity();
        return identity.getUser();
    }

    private String titleFor(final Application application) {
        final User user = user(application);
        final String id = user == null ? "nau" : user.getUsername();
        return id + "_" + application.getExternalId();
    }

    private JsonObject data(final Application application, final String uuid) {
        final Account account = application.getAccount();
        final PersonalInformation personalInformation = ConnectSystem.getPersonalInformationFor(account);
        final IdentificationDocument identificationDocument = personalInformation.getIdentificationDocument();
        final TaxInformation taxInformation = personalInformation.getTaxInformation();
        final Country nationality = Planet.getEarth().getByAlfa2(personalInformation.getNationalityCountryCode());
        final AdmissionProcessTarget target = application.getAdmissionProcessTarget();
        final Degree degree = FenixFramework.getDomainObject(target.getOutcomeConfigJson().get("degree").getAsString());
        final ExecutionYear executionYear = FenixFramework.getDomainObject(target.getOutcomeConfigJson().get("year").getAsString());
        final LocalizedString degreeName = degree.getPresentationNameI18N(executionYear);

        final JsonObject result = new JsonObject();
        result.addProperty("degreePT", degreeName.getContent(PT));
        result.addProperty("degreeEN", degreeName.getContent(EN));
        result.addProperty("name", personalInformation.getFullName());
        result.addProperty("dateOfBirth", personalInformation.getDateOfBirth().toString("yyyy-MM-dd"));
        result.addProperty("gender", personalInformation.getGender() == null ? "" : personalInformation.getGender().name());
        result.addProperty("nationalityPT", nationality.getNationality(PT));
        result.addProperty("nationalityEN", nationality.getNationality(EN));
        result.addProperty("documentTypePT", getIdentificationDocumentName(identificationDocument, PT));
        result.addProperty("documentTypeEN", getIdentificationDocumentName(identificationDocument, EN));
        result.addProperty("docUmentCountry", Planet.getEarth().getByAlfa2(identificationDocument.getCountryCode()).getLocalizedName(EN));
        result.addProperty("documentNumber", identificationDocument.getDocumentNumber());
        result.addProperty("documenExpirationDate", identificationDocument.getExpirationDate().toString("yyyy-MM-dd"));
        result.addProperty("tin", taxInformation.getTin());

        result.addProperty("uuid", uuid);
        result.addProperty("qrcodeImage", generateURIBase64QRCode(uuid));

        return result;
    }

    public String getIdentificationDocumentName(final IdentificationDocument identificationDocument, final Locale locale) {
        return identificationDocument.getIdentificationDocumentName().getContent(locale);
    }

    private String generateURIBase64QRCode(final String uuid) {
        return "data:image/png;base64," + Base64.getEncoder().encodeToString((generateQRCode(uuid, 300, 300)));
    }

    private byte[] generateQRCode(final String identifier, final int width, final int height) {
        try (final ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            final BarcodeQRCode qrcode = new BarcodeQRCode("https://certifier.tecnico.ulisboa.pt/" + identifier, width, height, null);
            final Image awtImage = qrcode.createAwtImage(Color.BLACK, Color.WHITE);
            final BufferedImage buffer =
                    new BufferedImage(awtImage.getWidth(null), awtImage.getHeight(null), BufferedImage.TYPE_INT_RGB);
            buffer.getGraphics().drawImage(awtImage, 0, 0, null);
            ImageIO.write(buffer, "png", bytes);
            return bytes.toByteArray();
        } catch (final IOException e) {
            throw new NullPointerException("Error while generating qr code for identifier " + identifier);
        }
    }

    private JsonObject toJson(final AdmissionProcessTarget target, final Locale locale) {
        final JsonObject result = new JsonObject();
        result.addProperty("target", target.getName().getContent(locale));
        result.add("admitted", target.getApplicationSet().stream()
                .filter(application -> application.getAdmitted())
                .map(application -> toJson(application))
                .collect(StreamUtils.toJsonArray()));
        result.add("notAdmitted", target.getApplicationSet().stream()
                .filter(application -> application.getLockInstant() != null)
                .filter(application -> application.getAccepted() != null && application.getAccepted().booleanValue())
                .filter(application -> !application.getAdmitted())
                .map(application -> toJson(application))
                .collect(StreamUtils.toJsonArray()));
        result.add("rejected", target.getApplicationSet().stream()
                .filter(application -> application.getLockInstant() != null)
                .filter(application -> application.getAccepted() != null && !application.getAccepted().booleanValue())
                .map(application -> toJson(application))
                .collect(StreamUtils.toJsonArray()));
        return result;
    }

    private JsonObject toJson(final Application application) {
        final JsonObject result = new JsonObject();
        result.addProperty("name", application.getAccount().getIdentity().getPersonalInformation().getFullName());
        return result;
    }

    private byte[] generate(final String templateID, final JsonObject data, final Locale locale) {
        final PapyrusClient papyrusClient = createPapyrusClien();
        final InputStream inputStream = papyrusClient.render(templateID, locale, data);
        try {
            final byte[] document = ByteStreams.toByteArray(inputStream);
            final SignatureFieldSettings settings = new SignatureFieldSettings(150, 320, 550, 220, "signatureField", 1);
            return generateDocumentWithSignatureField(new ByteArrayInputStream(document), settings);
        } catch (final IOException ex) {
            throw new Error(ex);
        }
    }

    public byte[] generateDocumentWithSignatureField(final InputStream fileStream, final SignatureFieldSettings settings) {
        if (fileStream == null) {
            return null;
        }
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            PdfReader original = new PdfReader(fileStream);
            PdfStamper stp = new PdfStamper(original, bos);
            PdfFormField sig = PdfFormField.createSignature(stp.getWriter());
            sig.setWidget(new com.itextpdf.text.Rectangle(settings.getLlx(), settings.getLly(), settings.getUrx(), settings.getUry()), null);
            sig.setFlags(PdfAnnotation.FLAGS_PRINT);
            sig.put(PdfName.DA, new PdfString("/Helv 0 Tf 0 g"));
            sig.setFieldName(settings.getName());
            sig.setPage(settings.getPage());
            stp.addAnnotation(sig, settings.getPage());

            stp.getOverContent(settings.getPage());
            stp.close();
            return bos.toByteArray();
        } catch (IOException | DocumentException e) {
            throw new Error(e);
        }
    }

    private PapyrusClient createPapyrusClien() {
        final Properties properties = loadProperties();
        return new PapyrusClient(properties.getProperty("papyrus.url"), properties.getProperty("papyrus.token"));
    }

    private Properties loadProperties() {
        try (final InputStream input = Bennu.class.getClassLoader().getResourceAsStream("configuration.properties")) {
            final Properties properties = new Properties();
            if (input == null) {
                return null;
            }
            properties.load(input);
            return properties;
        } catch (final IOException ex) {
            throw new Error(ex);
        }
    }

    public void sendDocumentToBeSigned(final String queue, final String title, final String description,
                                       final String filename, final InputStream contentStream,
                                       final String uuid, final String username) {
        final String compactJws = Jwts.builder()
                .setSubject(RegistrationProcessConfiguration.getConfiguration().signerJwtUser())
                .setExpiration(DateTime.now().plusHours(6).toDate())
                .signWith(SignatureAlgorithm.HS512, RegistrationProcessConfiguration.signerJwtSecret()).compact();

        try (final FormDataMultiPart formDataMultiPart = new FormDataMultiPart()) {
            final StreamDataBodyPart streamDataBodyPart = new StreamDataBodyPart("file", contentStream, filename, new MediaType("application", "pdf"));
            formDataMultiPart.bodyPart(streamDataBodyPart);
            formDataMultiPart.bodyPart(new FormDataBodyPart("queue", queue));
            formDataMultiPart.bodyPart(new FormDataBodyPart("creator", "Sistema FenixEdu"));
            formDataMultiPart.bodyPart(new FormDataBodyPart("filename", filename));
            formDataMultiPart.bodyPart(new FormDataBodyPart("title", title));
            formDataMultiPart.bodyPart(new FormDataBodyPart("description", description));
            formDataMultiPart.bodyPart(new FormDataBodyPart("externalIdentifier", uuid));
            formDataMultiPart.bodyPart(new FormDataBodyPart("signatureField", CandidacySignalHandler.SIGNATURE_FIELD));

            final String nounce = Jwts.builder().setSubject(uuid).signWith(SignatureAlgorithm.HS512, RegistrationProcessConfiguration.signerJwtSecret()).compact();

            formDataMultiPart.bodyPart(new FormDataBodyPart("callbackUrl", CoreConfiguration.getConfiguration().applicationUrl()
                    + "/adhock-document/store/" + username + "/" + filename + "?nounce=" + nounce));
            final Client client = ClientBuilder.newClient();
            client.register(MultiPartFeature.class);
            client.register(JsonBodyReaderWriter.class);
            client.target(RegistrationProcessConfiguration.getConfiguration().signerUrl()).path("sign-requests")
                    .request().header("Authorization", "Bearer " + compactJws)
                    .post(Entity.entity(formDataMultiPart, MediaType.MULTIPART_FORM_DATA_TYPE), String.class);
        } catch (final IOException e) {
            throw new Error(e);
        }
    }

    private Set<String> load(final String logFilename) {
        final Set<String> result = new HashSet<>();
        final File file = new File(logFilename);
        if (file.exists()) {
            try {
                for (final String line : Files.readAllLines(file.toPath())) {
                    result.add(line);
                }
            } catch (final IOException e) {
                throw new Error(e);
            }
        }
        taskLog("Read %s users from file: %s%n", result.size(), logFilename);
        return result;
    }

    private void write(final String logFilename, final Set<String> processed) {
        final StringBuilder builder = new StringBuilder();
        processed.forEach(username -> builder.append(username).append("\n"));
        try {
            Files.write(new File(logFilename).toPath(), builder.toString().getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (final IOException e) {
            throw new Error(e);
        }
    }

}
