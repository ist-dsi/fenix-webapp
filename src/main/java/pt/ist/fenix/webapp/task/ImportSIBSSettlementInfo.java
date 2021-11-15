package pt.ist.fenix.webapp.task;

import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import pt.ist.payments.util.OmniChannalSettlementReport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Task(englishTitle = "Process SIBS file to import settlement info", readOnly = true)
public class ImportSIBSSettlementInfo extends CronTask {

    private static final String BASE_DIR = "/afs/ist.utl.pt/ciist/fenix/fenix_sibs001";
    private static final String INPUT_DIR = BASE_DIR + "/new";
    private static final String OUTPUT_DIR = BASE_DIR + "/processed";

    @Override
    public void runTask() throws Exception {
        final File input = new File(INPUT_DIR);
        if (input.exists()) {
            for (final File file : input.listFiles()) {
                if (file.getName().endsWith(".zip")) {
                    unzip(file, input);
                    file.delete();
                }
            }

            final File output = new File(OUTPUT_DIR);
            if (!output.exists()) {
                output.mkdirs();
            }

            for (final File file : input.listFiles()) {
                if (file.getName().endsWith(".csv")) {
                    OmniChannalSettlementReport.importFromReport(file.toPath());
                    final File dest = new File(output, file.getName());
                    Files.move(file.toPath(), dest.toPath(),
                            StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.COPY_ATTRIBUTES,
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    public static void unzip(final File zipFile, final File dir) throws IOException {
        final byte[] buffer = new byte[1024];
        try (final ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            for (ZipEntry zipEntry = zis.getNextEntry(); zipEntry != null; zipEntry = zis.getNextEntry()) {
                final File newFile =  new File(dir, zipEntry.getName());
                try (final FileOutputStream fos = new FileOutputStream(newFile)) {
                    for (int len; (len = zis.read(buffer)) > 0; fos.write(buffer, 0, len)) {

                    }
                }
            }
            zis.closeEntry();
        }
    }

}
