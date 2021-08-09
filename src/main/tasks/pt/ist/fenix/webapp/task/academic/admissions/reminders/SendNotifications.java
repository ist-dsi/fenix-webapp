package pt.ist.fenix.webapp.task.academic.admissions.reminders;

import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.connect.domain.Account;
import org.fenixedu.messaging.core.domain.Message;

import java.util.stream.Stream;

public class SendNotifications extends CustomTask {
    @Override
    public void runTask() throws Exception {
        sendNotValidatedNotifications(applicationStream()
                .filter(application -> application.getAccount().getIdentity() == null)
                .map(application -> application.getAccount().getEmail()));
        sendNotSubmittedNotifications(applicationStream()
                .filter(application -> application.getAccount().getIdentity() != null)
                .map(application -> application.getAccount())
                .distinct());
    }

    private Stream<Application> applicationStream() {
        return AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .flatMap(admissionProcess -> admissionProcess.getAdmissionProcessTargetSet().stream())
                .flatMap(admissionProcessTarget -> admissionProcessTarget.getApplicationSet().stream())
                .filter(application -> application.getLockInstant() == null);
    }

    private void sendNotValidatedNotifications(final Stream<String> emails) {
        emails.forEach(email -> {
            Message.fromSystem()
                    .subject("Application Pending Submission / Candidatura Por Submeter")
                    .textBody("Dear user,\n" +
                            "\n" +
                            "We noticed your intention of applying to Técnico Lisboa, but your application has not yet" +
                            "been submitted or is incomplete. The deadline for submitting applications is approaching. " +
                            "If you need help with any question please contact us at admissions@tecnico.ulisboa.pt \n" +
                            "\n" +
                            "Best Regards, The FenixEdu Team" +
                            "\n" +
                            "\n" +
                            "---\n" +
                            "\n" +
                            "Caro(a) utilizador(a),\n" +
                            "\n" +
                            "Damos conta da intenção de se candidatar ao Técnico Lisboa, mas a candidatura encontra-se " +
                            "por submeter ou está ainda incompleta. Aproxima-se o prazo de submissão de candidaturas. " +
                            "Se precisar de ajuda ou se tiver qualquer questão não hesite em nos cantactar pelo endereço " +
                            "admissions@tecnico.ulisboa.pt \n" +
                            "\n" +
                            "Os melhores cumprimentos, A Equipa FenixEdu" +
                            "\n" +
                            "")
                    .singleTos(email)
                    .send();
        });
    }

    private void sendNotSubmittedNotifications(final Stream<Account> accounts) {
        accounts.forEach(account -> {
            final String email = account.getEmail();
            Message.fromSystem()
                    .subject("Application Pending Submission / Candidatura Por Submeter")
                    .textBody("Dear " + account.getIdentity().getPersonalInformation().getFullName() + ",\n" +
                            "\n" +
                            "We noticed your intention of applying to Técnico Lisboa, but your application has not yet" +
                            "been submitted or is incomplete. The deadline for submitting applications is approaching. " +
                            "If you need help with any question please contact us at admissions@tecnico.ulisboa.pt \n" +
                            "\n" +
                            "Best Regards, The FenixEdu Team" +
                            "\n" +
                            "\n" +
                            "---\n" +
                            "\n" +
                            "Caro(a) " + account.getIdentity().getPersonalInformation().getFullName() + ",\n" +
                            "\n" +
                            "Damos conta da intenção de se candidatar ao Técnico Lisboa, mas a candidatura encontra-se " +
                            "por submeter ou está ainda incompleta. Aproxima-se o prazo de submissão de candidaturas. " +
                            "Se precisar de ajuda ou se tiver qualquer questão não hesite em nos cantactar pelo endereço " +
                            "admissions@tecnico.ulisboa.pt \n" +
                            "\n" +
                            "Os melhores cumprimentos, A Equipa FenixEdu" +
                            "\n" +
                            "")
                    .singleTos(email)
                    .send();
        });
    }
}
