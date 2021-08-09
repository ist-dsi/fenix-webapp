package pt.ist.fenix.webapp.task.institutional.declarations;

import com.google.common.io.ByteStreams;
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
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.WrittenEvaluation;
import org.fenixedu.academic.domain.accessControl.ActiveTeachersGroup;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcess;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcessState;
import org.fenixedu.bennu.RegistrationProcessConfiguration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.rest.JsonBodyReaderWriter;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.papyrus.domain.SignatureFieldSettings;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;
import org.joda.time.DateTime;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveEmployees;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveGrantOwner;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveResearchers;
import pt.ist.papyrus.PapyrusClient;
import pt.ist.registration.process.handler.CandidacySignalHandler;
import pt.ist.registration.process.ui.service.exception.ProblemsGeneratingDocumentException;

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
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GenerateTravelDeclarationsForSigning extends ReadCustomTask {

    private static final Locale PT = new Locale("pt");

    private static final String TEMPLATE_COLLABORATOR = "declaracao-deslocacoes-colaborador";
    private static final String TEMPLATE_COLLABORATOR_DETAILED = "declaracao-deslocacoes-colaborador-detailed";
    private static final String TEMPLATE_STUDENT = "declaracao-deslocacoes-aluno";

    private static final String QUEUE_COLLABORATOR = "duk6flLV";
    private static final String QUEUE_STUDENT = "dcBiQYF4";

    private static final String FILENAME_INFIX_COLLABORATOR = "_collab_decl_deslocacao_20201205";
    private static final String FILENAME_INFIX_COLLABORATOR_DETAILED = "_collab_esp_decl_deslocacao_2021_emerg";
    private static final String FILENAME_INFIX_STUDENT = "_student_decl_deslocacao_20210228";

    private static final String LOG_PREFIX = "/afs/ist.utl.pt/ciist/fenix/fenix036/travelDeclaration";
    private static final String LOG_COLLABORATOR = "/afs/ist.utl.pt/ciist/fenix/fenix036/travelDeclarationCollaborators" + FILENAME_INFIX_COLLABORATOR + ".txt";
    private static final String LOG_COLLABORATOR_DETAILED = "/afs/ist.utl.pt/ciist/fenix/fenix036/travelDeclarationCollaboratorsDetalhadosEmerg202101.txt";
    private static final String LOG_STUDENT = "/afs/ist.utl.pt/ciist/fenix/fenix036/travelDeclarationStudents" + FILENAME_INFIX_STUDENT + ".txt";

    @Override
    public void runTask() throws Exception {
/*
        final String students = allStudents().map(u -> u.getUsername())
                .collect(Collectors.joining("\n"));
        Files.write(new File("/afs/ist.utl.pt/ciist/fenix/fenix036/travel.test").toPath(), students.getBytes(), StandardOpenOption.CREATE);
 */

//        batch(allTeachers(), LOG_PREFIX + "Teachers_202101.txt", QUEUE_STUDENT, TEMPLATE_COLLABORATOR, "_docente_decl_deslocacao_202101");
        //batch(allCollaborators(), LOG_COLLABORATOR, QUEUE_COLLABORATOR, TEMPLATE_COLLABORATOR, FILENAME_INFIX_COLLABORATOR);
        batch(allStudents(), LOG_PREFIX + "Students_20210326.txt", QUEUE_STUDENT, TEMPLATE_STUDENT, "_student_decl_deslocacao_20210326");
        //batch(detailCollaborators(), LOG_COLLABORATOR_DETAILED, QUEUE_COLLABORATOR, TEMPLATE_COLLABORATOR_DETAILED, FILENAME_INFIX_COLLABORATOR_DETAILED);
        //process("ist24956", QUEUE_COLLABORATOR, TEMPLATE_COLLABORATOR, FILENAME_INFIX_COLLABORATOR);

        //batch(specificTeachers(), LOG_COLLABORATOR, QUEUE_COLLABORATOR, TEMPLATE_COLLABORATOR, FILENAME_INFIX_COLLABORATOR);
//        batch(allStudents(), LOG_STUDENT, QUEUE_STUDENT, TEMPLATE_STUDENT, FILENAME_INFIX_STUDENT);
    }

    private void batch(final Stream<User> users, final String logFilename, final String queue, final String template, final String filenameInfix) {
        final Set<String> processed = load(logFilename);
        try {
            users.filter(user -> !processed.contains(user.getUsername()))
                    .distinct()
                    .forEach(user -> {
                        final String username = user.getUsername();
                        try {
                            process(username, queue, template, filenameInfix);
                            processed.add(username);
                        } catch (final Throwable t) {
                            taskLog("Failled to process: %s : %s%n", username, t.getMessage());
                        }
                    });
        } finally {
            write(logFilename, processed);
        }
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

    private Stream<User> specificTeachers() {
        final Stream<User> stream1 = Stream.empty();
        /*
                ExecutionSemester.readActualExecutionSemester().getTeacherAuthorizationStream()
                .filter(authorization -> authorization.getDepartment().getExternalId().equals("811748818946"))
                .map(authorization -> authorization.getTeacher().getPerson().getUser());
         */
        final Set<String> specificUsers = new HashSet<>();
        specificUsers.add("TODO");
        return Stream.concat(stream1, specificUsers.stream().map(s -> User.findByUsername(s)));
    }

    private Stream<User> allTeachers() {
        Stream<User> result = Stream.empty();
        result = Stream.concat(result, new ActiveTeachersGroup().getMembers());
        return result;
    }

    private Stream<User> allCollaborators() {
        Stream<User> result = Stream.empty();
        result = Stream.concat(result, new ActiveTeachersGroup().getMembers());
        result = Stream.concat(result, new ActiveResearchers().getMembers());
        result = Stream.concat(result, new ActiveEmployees().getMembers());
        result = Stream.concat(result, new ActiveGrantOwner().getMembers());
        return result;
    }

    private Stream<User> detailCollaborators() {
        return Stream.of(
            "TODO"
        ).map(username -> User.findByUsername(username));
    }

    private Stream<User> allStudents() {
        Stream<User> result = Stream.empty();
//        result = Stream.concat(result, currentPHDs());
//        result = Stream.concat(result, currentEnrolments());
//        result = Stream.concat(result, masterThesisStudents());
        //result = Stream.concat(result, withScheduledEvaluations());
        result = Stream.concat(result, specificStudents());
        //result = Stream.concat(result, Stream.of(User.findByUsername("ist198207")));
        return result;
    }

    private Stream<User> specificStudents() {
        final Set<String> usernames = new HashSet<>();

        usernames.add("TODO");

        return usernames.stream().map(u -> User.findByUsername(u))
                .peek(u -> {
                    if (u == null) {
                        taskLog("Null user");
                    }
                });
    }

    private Stream<User> withScheduledEvaluations() {
        return ExecutionSemester.readActualExecutionSemester().getAssociatedExecutionCoursesSet().stream()
                .filter(this::hasScheduledEvaluation)
                .flatMap(ec -> ec.getAttendsSet().stream())
                .map(a -> a.getRegistration().getStudent().getPerson().getUser())
                .distinct();
    }

    private boolean hasScheduledEvaluation(final ExecutionCourse executionCourse) {
        return executionCourse.getAssociatedEvaluationsSet().stream()
                .filter(WrittenEvaluation.class::isInstance)
                .map(WrittenEvaluation.class::cast)
                .filter(this::isForDay)
                .peek(writtenEvaluation -> taskLog("%s : %s%n",
                        writtenEvaluation.getAssociatedExecutionCoursesSet().iterator().next().getDegreePresentationString(),
                        writtenEvaluation.getAssociatedExecutionCoursesSet().iterator().next().getName()
                        ))
                .findAny().orElse(null) != null;
    }

    private boolean isForDay(final WrittenEvaluation writtenEvaluation) {
        final DateTime begin = writtenEvaluation.getBeginningDateTime();
        return begin.getYear() == 2020 && begin.getMonthOfYear() == 12 && begin.getDayOfMonth() == 5;
    }

    private Stream<User> currentPHDs() {
        return Bennu.getInstance().getProcessesSet().stream()
                .filter(PhdIndividualProgramProcess.class::isInstance)
                .map(PhdIndividualProgramProcess.class::cast)
                .filter(programProcess -> isActive(programProcess))
                .map(programProcess -> programProcess.getPerson().getUser());
    }

    private boolean isActive(final PhdIndividualProgramProcess programProcess) {
        final PhdIndividualProgramProcessState state = programProcess.getActiveState();
        return state == PhdIndividualProgramProcessState.WORK_DEVELOPMENT || state == PhdIndividualProgramProcessState.THESIS_DISCUSSION;
    }

    private Stream<User> currentEnrolments() {
        return ExecutionSemester.readActualExecutionSemester().getEnrolmentsSet().stream()
                .map(enrolment -> enrolment.getRegistration())
                .map(registration -> registration.getStudent().getPerson().getUser());
    }

    private Stream<User> masterThesisStudents() {
        return ExecutionYear.readCurrentExecutionYear().getPreviousExecutionYear().getExecutionPeriodsSet().stream()
                .flatMap(semester -> semester.getEnrolmentsSet().stream())
                .filter(enrolment -> enrolment.getThesis() != null)
                .filter(enrolment -> enrolment.isEnroled() && (enrolment.getGrade() == null || enrolment.getGrade().isEmpty()))
                .map(enrolment -> enrolment.getRegistration())
                .map(registration -> registration.getStudent().getPerson().getUser());
    }

    private void process(final String username, final String queue, final String templateID, final String filenameInfix) {
        final DateTime now = new DateTime();
        final User user = User.findByUsername(username);

        final JsonObject data = new JsonObject();
        final String uuid = UUID.randomUUID().toString();
        data.addProperty("fullName", user.getProfile().getFullName());
        data.addProperty("idDocumentNumber", user.getPerson().getDocumentIdNumber());
        data.addProperty("uuid", uuid);
        data.addProperty("qrcodeImage", generateURIBase64QRCode(uuid));

        final String title = user.getUsername() + filenameInfix + ".pdf";

        final byte[] document = generate(templateID, data);
        sendDocumentToBeSigned(queue, title, title, title, new ByteArrayInputStream(document), uuid, user.getUsername());
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

    public byte[] generateDocumentWithSignatureField(final InputStream fileStream, final SignatureFieldSettings settings)
            throws ProblemsGeneratingDocumentException {
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
            throw new ProblemsGeneratingDocumentException(e);
        }
    }

    private byte[] generate(final String templateID, final JsonObject data) {
        final PapyrusClient papyrusClient = createPapyrusClien();
        final InputStream inputStream = papyrusClient.render(templateID, PT, data);
        try {
            final byte[] document = ByteStreams.toByteArray(inputStream);
            final SignatureFieldSettings settings = new SignatureFieldSettings(100, 450, 500, 350, "signatureField", 1);
            return generateDocumentWithSignatureField(new ByteArrayInputStream(document), settings);
        } catch (final IOException ex) {
            throw new Error(ex);
        } catch (final ProblemsGeneratingDocumentException ex) {
            throw new Error(ex);
        }
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

}
