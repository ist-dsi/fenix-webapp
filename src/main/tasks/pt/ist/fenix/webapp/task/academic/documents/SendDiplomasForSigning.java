package pt.ist.fenix.webapp.task.academic.documents;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.RegistrationProcessConfiguration;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.rest.JsonBodyReaderWriter;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;
import org.joda.time.DateTime;
import pt.ist.registration.process.handler.CandidacySignalHandler;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.UUID;

public class SendDiplomasForSigning extends ReadCustomTask {

    private final String DIR = "/afs/ist.utl.pt/ciist/fenix/fenix060/diplomas-alunos";

    @Override
    public void runTask() throws Exception {
        process(new File(DIR));
    }

    private void process(final File file) {
        if (file.isDirectory()) {
            for (final File child : file.listFiles()) {
                process(child);
            }
        } else {
            if (file.getName().endsWith(".pdf")) {
                try {
                    //processPDF(file);
                    sendFileToSigner(file);
                } catch (IOException e) {
                    taskLog("Error processing file: %s%n", file.getPath());
                }
            }
        }
    }

    private void processPDF(final File file) throws IOException {
        if (file.getName().indexOf("_ist") < 0) {
            final byte[] content = Files.readAllBytes(file.toPath());
            final String text = readTextFromPDF(content);

            final int i1 = text.indexOf("<ist_id>");
            final int i2 = text.indexOf("</ist_id>");

            if (i1 < 0 || i2 < 0 || i2 < i1) {
                taskLog("No TecnicoID tag found in file: %s%n", file.toPath());
            } else {
                final String id = text.substring(i1 + 8, i2).trim().replace(" ", "");
                if (id.isEmpty() || id.length() < 4) {
                    taskLog("No username found in file %s%n", file.getPath());
                } else {
                    final User user = findUserForId(id.toLowerCase());
                    if (user == null) {
                        taskLog("No user found for id %s : [%s]%n", file.getPath(), id);
                        throw new Error();
                    } else {
                        final String filename = toFileName(file.getPath().replace(' ', '_'), user.getUsername());
                        file.renameTo(new File(file.getParent() + File.separator + filename));
                        //taskLog("%s -> [%s]%n", id, filename);
                    }
                }
            }
        }
    }

    private String toFileName(final String path, final String username) {
        final int i = path.lastIndexOf('/');
        final int j = path.lastIndexOf('_');
        if (i < 0 || j < 0) {
            taskLog("Out of range for path %s%n", path);
        }
        return path.substring(i + 1, j + 1) + username + ".pdf";
    }

    private User findUserForId(final String id) {
        if (id.startsWith("ist")) {
            return User.findByUsername(id);
        }
        if (StringUtils.isNumeric(id)) {
            final Student student = Student.readStudentByNumber(new Integer(id));
            if (student != null) {
                return student.getPerson().getUser();
            }
        }
        return null;
    }

    private String readTextFromPDF(byte[] content) throws IOException {
        final PdfReader reader = new PdfReader(content);
        final PdfReaderContentParser parser = new PdfReaderContentParser(reader);
        final StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            final SimpleTextExtractionStrategy strategy = parser.processContent(i, new SimpleTextExtractionStrategy());
            builder.append(strategy.getResultantText());
        }
        reader.close();
        return builder.toString();
    }

    private void sendFileToSigner(final File file) throws IOException {
        final String path = file.getPath();
        if (path.indexOf(".sent.pdf") > 0) {
            return ;
        }
        final byte[] content = Files.readAllBytes(file.toPath());
        final ByteArrayInputStream stream = new ByteArrayInputStream(content);
        final int i = path.lastIndexOf('/');
        final String filename = path.substring(i + 1);

        final String title = filename.substring(0, filename.length() - 4);

        sendDocumentToBeSigned("oIK0zjNu", title, title, filename, stream, UUID.randomUUID().toString(), "ist24439");

        final String newFilename = filename.replace(".pdf", ".sent.pdf");
        file.renameTo(new File(file.getParent() + File.separator + newFilename));
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
            //formDataMultiPart.bodyPart(new FormDataBodyPart("signatureField", CandidacySignalHandler.SIGNATURE_FIELD));

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

}