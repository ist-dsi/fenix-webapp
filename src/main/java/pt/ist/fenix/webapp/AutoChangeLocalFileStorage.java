package pt.ist.fenix.webapp;

import org.apache.commons.io.IOUtils;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.io.domain.LocalFileSystemStorage;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.Collator;
import java.util.stream.Stream;

@Task(englishTitle = "Automatically change local file storage when it is full")
public class AutoChangeLocalFileStorage extends CronTask {

    private static final int PCT_THREASHOLD = 99;

    @Override
    public void runTask() throws Exception {
        localFileStorages()
                .filter(lfs -> lfs.getConfigurationSet().size() > 0)
                .forEach(this::process);
    }

    private Stream<LocalFileSystemStorage> localFileStorages() {
        return Bennu.getInstance().getFileSupport().getFileStorageSet().stream()
                .filter(fs -> fs instanceof LocalFileSystemStorage)
                .map(fs -> (LocalFileSystemStorage) fs)
                .sorted((lfs1, lfs2) -> Collator.getInstance().compare(lfs1.getPath(), lfs2.getPath()));
    }

    private void process(final LocalFileSystemStorage lfs) {
        final String absolutePath = getRealPath(lfs);
        final int availablePct = getAvailablePct(absolutePath);
        if (availablePct >= PCT_THREASHOLD) {
            final int usages = lfs.getConfigurationSet().size();
            final String name = lfs.getName();
            final String path = lfs.getPath();
            taskLog("%s : %s : %s : %s : %s%n", name, path, absolutePath == null ? "Offline" : "Ok", usages, availablePct);

            final LocalFileSystemStorage next = findNextAvailableStore(lfs.getName());
            if (next == null) {
                notify("Store " + lfs.getName() + " is full - No other store is available!");
            } else {
                next.getConfigurationSet().addAll(lfs.getConfigurationSet());
                notify("Store " + lfs.getName() + " is full - switched to new store " + next.getName());
            }
        }
    }

    private LocalFileSystemStorage findNextAvailableStore(final String name) {
        return localFileStorages()
                .filter(lfs -> Collator.getInstance().compare(lfs.getName(), name) > 0)
                .filter(lfs -> getAvailablePct(getRealPath(lfs)) == 0)
                .findFirst().orElse(null);
    }

    private int getAvailablePct(final String absolutePath) {
        if (absolutePath == null) {
            return 0;
        }
        try {
            final Process process = Runtime.getRuntime().exec(String.format("fs lq -path %s", absolutePath));
            try (final InputStream inputStream = process.getInputStream()) {
                process.waitFor();
                final String result = extractPct(IOUtils.toString(inputStream, Charset.defaultCharset()));
                return Integer.parseInt(result);
            } finally {
                process.destroy();
            }
        } catch (final IOException | InterruptedException e) {
            throw new Error(e);
        }
    }

    private String extractPct(final String s) {
        final int i1 = s.indexOf("\n");
        final String[] parts = s.substring(i1 + 1).replaceAll("\\s+", " ").trim().split(" ");
        final String part = parts[3];
        final int i2 = part.indexOf('%');
        return part.substring(0, i2);
    }

    private String getRealPath(final LocalFileSystemStorage lfs) {
        try {
            return lfs.getAbsolutePath();
        } catch (final RuntimeException ex) {
            return null;
        }
    }

    private static void notify(final String subject)  {
        org.fenixedu.messaging.core.domain.Message.fromSystem()
                .replyToSender()
                .bcc(Group.managers())
                .singleTos("si@tecnico.ulisboa.pt")
                .subject(subject)
                .textBody(subject)
                .send();
    }

}
