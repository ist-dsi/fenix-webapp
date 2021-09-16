package pt.ist.fenix.webapp.task;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.academic.domain.studentCurriculum.CycleCurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.RootCurriculumGroup;
import org.fenixedu.academic.transitions.domain.DegreeCurricularTransitionPlan;
import org.fenixedu.academic.transitions.domain.StudentDegreeCurricularTransitionPlan;
import org.fenixedu.academic.transitions.service.TransitionService;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.groups.NamedGroup;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.messaging.core.domain.Message;
import org.joda.time.DateTime;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Task(englishTitle = "Transition Student Curricular Plans", readOnly = true)
public class TransitionSCPs extends CronTask {

    private Map<Registration, Set<StudentDegreeCurricularTransitionPlan>> studentMap = null;

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {
        User user = Authenticate.getUser();
        Authenticate.mock(User.findByUsername("ist24439"), "Transition Script Runner");

        FenixFramework.atomic(() -> {
            Bennu.getInstance().getStudentsSet().stream()
                    .flatMap(student -> student.getStudentDegreeCurricularTransitionPlanSet().stream())
                    .filter(plan -> isConcluded(plan))
                    .forEach(plan -> plan.delete());

            final HashMap<DegreeCurricularTransitionPlan, Set<User>> coordinatorMap = new HashMap<>();
            Bennu.getInstance().getStudentsSet().stream()
                    .flatMap(student -> student.getStudentDegreeCurricularTransitionPlanSet().stream())
                    .filter(studentPlan -> !hasDestination(studentPlan.getDegreeCurricularTransitionPlan()
                            .getDestinationDegreeCurricularPlan(), studentPlan.getStudent()))
                    .filter(plan -> plan.changedAfterUpdate())
                    .distinct()
                    .forEach(plan -> {
                        final User studentUser = plan.getStudent().getPerson().getUser();
                        if (plan.getConfirmTransitionInstant() != null) {
                            Message.fromSystem()
                                    .to(Group.users(studentUser))
                                    .subject("Transição Curricular em Revisão")
                                    .textBody("Caro(a) aluno(a),\n\n" +
                                            "Não foi possível proceder à sua transição conforme estava planeada devido " +
                                            "a alterações no seu plano curricular, realizadas após a elaboração inicial desse plano.\n" +
                                            "Este terá que ser revisto pelo coordenador do seu curso, e só após essa validação " +
                                            "estará novamente disponível para a sua confirmação.\n" +
                                            "\n" +
                                            "Os melhores cumprimentos,\n" +
                                            "A Equipa FenixEdu")
                                    .wrapped().send();
                        }
                        if (plan.getFreezeInstant() != null) {
                            coordinatorMap.computeIfAbsent(plan.getDegreeCurricularTransitionPlan(),
                                    (v) -> new HashSet<>()).add(studentUser);
                        }
                        plan.delete();
                    });
            coordinatorMap.forEach((plan, set) -> {
                final String usernames = set.stream().map(u -> u.getUsername()).collect(Collectors.joining("\n"));
                final Stream<User> coordinators = plan.getDestinationDegreeCurricularPlan().getExecutionDegreesSet().stream()
                        .filter(ed -> ed.getExecutionYear() == plan.getDestinationExecutionYear())
                        .flatMap(ed -> ed.getCoordinatorsListSet().stream())
                        .map(coordinator -> coordinator.getPerson().getUser());
                final Group bcc = new NamedGroup(new LocalizedString(new Locale("pt", "PT"), "Coordenadores do "
                        + plan.getDestinationDegreeCurricularPlan().getDegree().getSigla()),
                        Group.users(User.findByUsername("ist24439"), User.findByUsername("ist24616")));
                Message.fromSystem()
                        .to(Group.users(coordinators))
                        .bcc(bcc)
                        .subject("Transição Curricular em Revisão")
                        .textBody("Caro(a) coordenador(a),\n\n" +
                                "Foi necessário proceder ao descongelamento dos planos de transição dos seguintes alunos, " +
                                "consequência de terem sido efetuadas alterações ao currículo do aluno depois do plano de " +
                                "transição ter sido elaborado.\n" +
                                "\n" +
                                usernames +
                                "\n" +
                                "\n" +
                                "Os melhores cumprimentos,\n" +
                                "A Equipa FenixEdu")
                        .wrapped().send();
            });
        });

        studentMap = new HashMap<>();
        DegreeCurricularPlan.readBolonhaDegreeCurricularPlans().stream()
                .flatMap(dcp -> dcp.getDestinationTransitionPlanSet().stream())
//                .filter(dcp -> dcp.getDestinationDegreeCurricularPlan().getDegree().getSigla().equals("MEEC21"))
                .flatMap(transitionPlan -> transitionPlan.getStudentDegreeCurricularTransitionPlanSet().stream())
                .filter(studentPlan -> studentPlan.getConfirmTransitionInstant() != null)
                .filter(studentPlan -> studentPlan.getFreezeInstant() != null)
//                .filter(studentPlan -> studentPlan.getStudent().getPerson().getUsername().equals("ist426311"))
                .filter(studentPlan -> !exclude(studentPlan.getStudent().getPerson().getUsername()))
                .filter(studentPlan -> !hasDestination(studentPlan.getDegreeCurricularTransitionPlan()
                        .getDestinationDegreeCurricularPlan(), studentPlan.getStudent()))
                .forEach(studentPlan -> {
                    final Student student = studentPlan.getStudent();
                    final DegreeCurricularTransitionPlan degreeCurricularTransitionPlan = studentPlan.getDegreeCurricularTransitionPlan();
                    final StudentCurricularPlan scp = TransitionService.getStudentCurricularPlanToTransition(student.getPerson().getUser(), degreeCurricularTransitionPlan);
                    if (scp != null) { //if the registration is not active (concluded or other)
                        final Set<CycleType> cycleTypesToTransition = TransitionService.getCyclesToTransition(degreeCurricularTransitionPlan.getCycleTypesToTransition(), scp);
                        if (!cycleTypesToTransition.isEmpty()) {
                            studentMap.computeIfAbsent(scp.getRegistration(), v -> new HashSet<>()).add(studentPlan);
                        }
                    }
                });

        for (Registration registration : studentMap.keySet()) {
            try {
                transition(registration);
            } catch (Throwable e) {
                taskLog("  --> Problem transitioning student\t%s\t%s%n", registration.getPerson().getUsername(), e.getMessage());
                e.printStackTrace();
            }
        }
        Authenticate.mock(user, "Restore User Transition Script");
    }

    private boolean isConcluded(final StudentDegreeCurricularTransitionPlan studentPlan) {
        final DegreeCurricularTransitionPlan degreePlan = studentPlan.getDegreeCurricularTransitionPlan();
        final DegreeCurricularPlan destinationPlan = degreePlan.getDestinationDegreeCurricularPlan();
        final Set<CycleType> cycleTypes = destinationPlan.getRoot().getCycleCourseGroups().stream()
                .map(group -> group.getCycleType())
                .collect(Collectors.toSet());
        final Student student = studentPlan.getStudent();
        return (!destinationPlan.getDegreeType().isIntegratedMasterDegree()) && student.getRegistrationsSet().stream()
                .flatMap(registration -> registration.getStudentCurricularPlansSet().stream())
                .flatMap(scp -> scp.getCycleCurriculumGroups().stream())
                .filter(group -> cycleTypes.contains(group.getCycleType()))
                .filter(group -> group.getDegreeModule().getDegree() == degreePlan.getOriginDegreeCurricularPlan().getDegree())
                .anyMatch(group -> group.isConcluded());
    }

    private static boolean hasDestination(final DegreeCurricularPlan destinationPlan, final Student student) {
        return student.getRegistrationsSet().stream()
                .flatMap(registration -> registration.getStudentCurricularPlansSet().stream())
                .anyMatch(scp -> scp.getDegreeCurricularPlan() == destinationPlan);
    }

    private void transition(final Registration registration) {
        taskLog("Running transition for %s = %s", registration.getPerson().getUsername(), registration.getDegree().getSigla());
        final Person person = registration.getPerson();
        final Set<StudentDegreeCurricularTransitionPlan> studentTransitionPlans = studentMap.get(registration);
        if (studentTransitionPlans.stream().anyMatch(plan -> plan.changedAfterUpdate())) {
            throw new Error("Plan changed after freeze.");
        }
        if (registration.getStudentCurricularPlansSet().stream()
            .flatMap(scp -> scp.getEnrolmentStream())
            .filter(enrolment -> enrolment.isEnroled())
            .anyMatch(enrolment -> studentTransitionPlans.stream()
                    .map(p -> p.getDegreeCurricularTransitionPlan().getOriginExecutionYear())
                    .anyMatch(executionYear -> executionYear == enrolment.getExecutionYear()))) {
            throw new Error("Missing grades");
        }
        if (studentTransitionPlans.size() == 1) {
            final StudentDegreeCurricularTransitionPlan studentPlan = studentTransitionPlans.iterator().next();
            final StudentCurricularPlan scp = TransitionService.getStudentCurricularPlanToTransition(registration.getPerson().getUser(),
                    studentPlan.getDegreeCurricularTransitionPlan());
            if (!scp.getDegree().isSecondCycle() && scp.getSecondCycle() != null) { //tem 2º ciclo em avanço
                if (scp.getFirstCycle() != null && scp.getFirstCycle().isConcluded()) {
                    //tudo ok - é o plano de transição para o 2º ciclo
                } else {
                    taskLog();
                    return; //não tem plano especializado para o 2º ciclo, só tem para o 1º
                }
            } else if (scp.getDegree().isSecondCycle() && scp.getDegree().isFirstCycle()) { //mestrado integrado
                if (scp.hasConcludedCycle(CycleType.FIRST_CYCLE)) {
                    //tudo ok - o 1º ciclo já está concluido, é só para migrar o 2º
                } else if (studentPlan.getDegreeCurricularTransitionPlan().getDestinationDegreeCurricularPlan().getDegree().getCycleTypes().size() == 2) {
                    //tudo ok - o plano de destino contém os 2 ciclos portanto o aluno só tem um plano de transição
                } else if (scp.getCycleCurriculumGroups().size() == 1) {
                    //tudo ok - é um mestrado integrado mas o aluno só tem um ciclo aberto no scp
                } else if (scp.getSecondCycle() != null && scp.getSecondCycle().getAprovedEctsCredits().doubleValue() == 0.0d) {
                    //tudo ok - é um mestrado integrado com os 2 ciclos abertos mas o 2º não tem lá nada
                } else {
                    taskLog();
                    return;
                }
            }
            TransitionService.run(studentPlan.getDegreeCurricularTransitionPlan(), person.getUser(), false, false, true, true);
            Authenticate.mock(User.findByUsername("ist24439"), "Transition Script Runner");
            taskLog(" > ok for 1");

            FenixFramework.atomic(() -> {
                final StudentCurricularPlan newSCP = scp.getRegistration().getLastStudentCurricularPlan();
                newSCP.getCycleCurriculumGroups().stream()
                        .filter(group -> group.getAprovedEctsCredits().doubleValue() == 0d && group.getCurriculumLines().isEmpty())
                        .forEach(group -> group.deleteRecursive());
            });
        } else {
            studentTransitionPlans.stream()
                    .sorted((p1, p2) -> CycleType.COMPARATOR_BY_LESS_WEIGHT.compare(cycleTypeFor(p1), cycleTypeFor(p2)))
                    .forEach(studentPlan -> {
                        taskLog("%n   running for %s", studentPlan.getDegreeCurricularTransitionPlan().getDestinationDegreeCurricularPlan().getName());
                        TransitionService.run(studentPlan.getDegreeCurricularTransitionPlan(), person.getUser(), false, false, true, true);
                        Authenticate.mock(User.findByUsername("ist24439"), "Transition Script Runner");
                        FenixFramework.atomic(() -> {
                            studentTransitionPlans.forEach(plan -> plan.setUpdateInstant(new DateTime()));
                        });
                        taskLog("   ok.");
                    });
            FenixFramework.atomic(() -> {
                final StudentDegreeCurricularTransitionPlan studentPlan = studentTransitionPlans.iterator().next();
                final StudentCurricularPlan scp = TransitionService.getStudentCurricularPlanToTransition(person.getUser(),
                        studentPlan.getDegreeCurricularTransitionPlan());

                Registration firstCycleRegistration = findCycleRegistration(scp.getRegistration().getStudent(), studentTransitionPlans, CycleType.FIRST_CYCLE);
                Registration secondCycleRegistration = findCycleRegistration(scp.getRegistration().getStudent(), studentTransitionPlans, CycleType.SECOND_CYCLE);
                final StudentCurricularPlan firstSCP = firstCycleRegistration.getLastStudentCurricularPlan();
                final RootCurriculumGroup firstCycleRoot = firstSCP.getRoot();
                final CycleCurriculumGroup secondCycle = secondCycleRegistration.getLastStudentCurricularPlan().getSecondCycle();
                secondCycle.setCurriculumGroup(firstCycleRoot);
                secondCycleRegistration.getStudentCurricularPlansSet().stream()
                        .flatMap(scp1 -> scp1.getCreditsSet().stream())
                        .forEach(credits -> credits.setStudentCurricularPlan(firstSCP));
                secondCycleRegistration.delete();

            });
            taskLog("   ok after second cycle delete check.");
        }

        FenixFramework.atomic(() -> {
            if (!isSameDCP(registration, studentTransitionPlans)) {
                RegistrationState.createRegistrationState(registration, person, new DateTime(), RegistrationStateType.TRANSITED);
            }

            final Registration destinationRegistration = getDestinationRegistration(registration.getStudent());
            if (destinationRegistration.getActiveState().getStateType().equals(RegistrationStateType.TRANSITED)) {
                //it was transitioned to an existing registration that was transitioned to another one previously and it needs to be active
                RegistrationState.createRegistrationState(destinationRegistration, person,new DateTime(), RegistrationStateType.REGISTERED);
            }

            Message.fromSystem()
                    .to(Group.users(registration.getPerson().getUser()))
                    .subject("Transição Curricular Completa")
                    .textBody("Caro(a) aluno(a),\n\n" +
                            "A sua transicção para o novo plano curricular foi concluído com sucesso.\n" +
//                            "Lamentamos o atraso na conclusão desta operação.\n" +
                            "Pode agora proceder às suas inscrições no portal do aluno.\n" +
                            "\n" +
                            "Os melhores cumprimentos,\n" +
                            "A Equipa FenixEdu")
                    .wrapped().send();
        });
    }

    private CycleType cycleTypeFor(final StudentDegreeCurricularTransitionPlan plan) {
        return plan.getDegreeCurricularTransitionPlan().getCycleTypesToTransition().stream()
                .sorted(CycleType.COMPARATOR_BY_LESS_WEIGHT).findFirst().orElseThrow(() -> new Error());
    }

    private Registration findCycleRegistration(final Student student, final Set<StudentDegreeCurricularTransitionPlan> studentTransitionPlans, final CycleType cycleType) {
        return student.getRegistrationsSet().stream()
                .filter(r -> r.getDegree().getCycleTypes().contains(cycleType))
                .filter(r -> isSameDCP(r, studentTransitionPlans))
                .findAny().get();
    }

    private Registration getDestinationRegistration(final Student student) {
        return student.getRegistrationStream()
                .filter(r -> isSameDCP(r, r.getStudent().getStudentDegreeCurricularTransitionPlanSet()))
                .findAny().get();
    }

    private boolean isSameDCP(Registration registration, Set<StudentDegreeCurricularTransitionPlan> studentTransitionPlans) {
        return studentTransitionPlans.stream()
                .map(stp -> stp.getDegreeCurricularTransitionPlan().getDestinationDegreeCurricularPlan())
                .anyMatch(dcp -> registration.getDegreeCurricularPlans().contains(dcp));
    }

    private static final String[] exclusions = new String[]{
            "ist190709",
            "ist189516",
            "ist186978",
            "ist423305",
            "ist1100693",
            "ist196252",
            "ist194150",
            "ist196295",
            "ist196147",
            "ist1100073",
            "ist199945",
            "ist196265",
            "ist196289",
            "ist1100059",
            "ist199969",
            "ist199881",
            "ist199987",
            "ist199911",
            "ist1100003",
            "ist1100000",
            "ist199980",
            "ist199937",
            "ist199942",
            "ist1100035",
            "ist196319",
            "ist179130",
            "ist189895",
            "ist163240",
            "ist169749"
    };

    private static boolean exclude(final String username) {
        for (final String s : exclusions) {
            if (s.equals(username)) {
                return true;
            }
        }
        return false;
    }

}
