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
import java.util.stream.Stream;

public class GenerateDocumentsFromSheet extends ReadCustomTask {

    private static final Locale PT = new Locale("pt");

    @Override
    public void runTask() throws Exception {
        final StringBuilder log = new StringBuilder();

        final String content = "";
        int i = 0;
        for (final String line : content.split("\n")) {
            i++;
            final String[] parts = splitLine(line);
            final String template = parts[0];
            final String queue = parts[1];
            final String filenameInfix = parts[2];
            final String username = parts[3];

            try {
                process(username, queue, template, filenameInfix);
            } catch (final Throwable t) {
//                log.append("Failled to process line: " + i + " username: " + username);
                taskLog("Failled to process line: " + i + " username: " + username + " : " + t.getMessage());
            }
        }

    }

    private String[] splitLine(final String line) {
        final String seperator = line.indexOf('\t') > 0 ? "\t"
                : line.indexOf(',') > 0 ? ","
                : line.indexOf(';') > 0 ? ";"
                : " ";
        return line.split(seperator);
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
