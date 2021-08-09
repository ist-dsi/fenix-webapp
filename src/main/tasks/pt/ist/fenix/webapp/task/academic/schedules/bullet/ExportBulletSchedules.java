package pt.ist.fenix.webapp.task.academic.schedules.bullet;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.CurricularSemester;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Department;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.Lesson;
import org.fenixedu.academic.domain.SchoolClass;
import org.fenixedu.academic.domain.ShiftType;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseLoad;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.organizationalStructure.DepartmentUnit;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.spaces.domain.Space;
import org.joda.time.DateTime;
import pt.ist.fenixedu.bullet.domain.BulletCharacteristic;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExportBulletSchedules extends ReadCustomTask {

    public static final int consecutiveClassHoursLimit = 5;
    private static final BigDecimal slotsPerHour = new BigDecimal(2), minClassSlots = slotsPerHour, maxClassSlots = slotsPerHour.multiply(new BigDecimal(consecutiveClassHoursLimit));

    @Override
    public void runTask() throws Exception {
        final ExecutionSemester executionSemester = ExecutionSemester.readActualExecutionSemester();
        //process(executionSemester);
        process(executionSemester.getNextExecutionPeriod());
        process(executionSemester.getNextExecutionPeriod().getNextExecutionPeriod());
        //process(executionSemester.getNextExecutionPeriod().getNextExecutionPeriod().getNextExecutionPeriod());
    }

    private void process(final ExecutionSemester executionSemester) throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Bullet");
        {
            final Spreadsheet.Row row = spreadsheet.addRow();
            row.setCell("Year", executionSemester.getExecutionYear().getYear());
            row.setCell("Semester", executionSemester.getSemester());
            row.setCell("Report Date", new DateTime().toString("yyyy-MM-dd HH:mm"));
        }

        final Spreadsheet caracteristicas = spreadsheet.addSpreadsheet("Caracteristicas");
        final Spreadsheet zonas = caracteristicas.addSpreadsheet("Zonas");
        final Spreadsheet edificios = zonas.addSpreadsheet("Edificios");
        final Spreadsheet pisos = edificios.addSpreadsheet("Pisos");
        final Spreadsheet salas = pisos.addSpreadsheet("Salas");

        final Set<String> caracteristicsSet = new HashSet<>();
        final Set<String> levelsSet = new HashSet<>();
        for (final Space space : Bennu.getInstance().getSpaceSet()) {
            if (isSpaceType(space, "Campus")) {
                final Spreadsheet.Row row = zonas.addRow();
                row.setCell("Nome", space.getName());
            } else if (isSpaceType(space, "Building", "Edifício")) {
                final Spreadsheet.Row row = edificios.addRow();
                row.setCell("Nome", space.getName());
                row.setCell("Zona", campusFor(space).getName());
            } else if (isSpaceType(space, "Floor", "Piso")) {
                levelsSet.add(levelName(space));
            }
        }
        Stream.of(executionSemester.getPreviousExecutionPeriod().getPreviousExecutionPeriod(), executionSemester.getPreviousExecutionPeriod())
                .flatMap(es -> es.getAssociatedExecutionCoursesSet().stream())
                .flatMap(ec -> ec.getCourseLoadsSet().stream())
                .flatMap(cl -> cl.getShiftsSet().stream())
                .flatMap(s -> s.getAssociatedLessonsSet().stream())
                .flatMap(l -> roomStream(l))
                .filter(room -> room != null)
                .distinct()
                .forEach(room -> {
                    BulletCharacteristic.getCharacteristics(room).forEach(s -> caracteristicsSet.add(s));

                    final Spreadsheet.Row row = salas.addRow();
                    row.setCell("Nome", room.getName());
                    row.setCell("Edificio", buildingFor(room).getName());
                    row.setCell("Piso", levelName(levelFor(room)));
                    row.setCell("Sala", roomFullPath(room));
                    row.setCell("Capacidade", String.valueOf(room.getAllocatableCapacity()));
                    row.setCell("CapacidadeExame", String.valueOf(room.getMetadata("examCapacity").orElse(0)));
                    row.setCell("MargemAceitacao", String.valueOf(room.getAllocatableCapacity() / 10));
                    row.setCell("Caracteristica", BulletCharacteristic.getCharacteristics(room).collect(Collectors.joining(", ")));

                });
        caracteristicsSet.forEach(c -> caracteristicas.addRow().setCell("Nome", c));
        levelsSet.forEach(c -> pisos.addRow().setCell("Nome", c));

        final Spreadsheet topologias = salas.addSpreadsheet("Tipologias");
        for (final ShiftType shiftType : ShiftType.values()) {
            final Spreadsheet.Row row = topologias.addRow();
            row.setCell("Nome", shiftType.getSiglaTipoAula());
            row.setCell("Descricao", shiftType.getFullNameTipoAula());
        }

        final Spreadsheet areasCientificas = topologias.addSpreadsheet("AreasCientificas");
        for (final Department department : Bennu.getInstance().getDepartmentsSet()) {
            if (!department.getName().contains("Bolonha")) {
                final Spreadsheet.Row row = areasCientificas.addRow();
                row.setCell("Nome", department.getName());
            }
        }

        final Spreadsheet cursos = areasCientificas.addSpreadsheet("Cursos");
        degreeStream(executionSemester)
                .forEach(degree -> {
                    final Spreadsheet.Row row = cursos.addRow();
                    row.setCell("Nome", degree.getNameI18N(executionSemester.getExecutionYear()).getContent());
                    row.setCell("Sigla", degree.getSigla());
                    row.setCell("Codigo", degree.getSigla());
                });

        final Spreadsheet disciplinas = cursos.addSpreadsheet("Disciplinas");
        final Spreadsheet cargasSemanais = disciplinas.addSpreadsheet("CargasSemanais");
//        if (executionSemester.getAssociatedExecutionCoursesSet().isEmpty() || true) {
            curricularCourseStream(executionSemester)
                    .forEach(curricularCourse -> {
                        {
                            final Spreadsheet.Row row = disciplinas.addRow();
                            row.setCell("Nome", curricularCourse.getName());
                            row.setCell("Sigla", curricularCourse.getAcronym(executionSemester));
                            row.setCell("Codigo", curricularCourse.getExternalId());
                            row.setCell("Importancia", 1);
                            row.setCell("AreaCientifica", departmentFor(curricularCourse, executionSemester));
                            row.setCell("ECTS", creditsFor(curricularCourse, executionSemester));

                            final CompetenceCourse competenceCourse = curricularCourse.getCompetenceCourse();
                            if (competenceCourse != null) {
                                for (final CompetenceCourseLoad load : competenceCourse.getCompetenceCourseLoads(executionSemester)) {
                                    addLoad(cargasSemanais, curricularCourse, load, ShiftType.TEORICA, load.getTheoreticalHours());
                                    addLoad(cargasSemanais, curricularCourse, load, ShiftType.PROBLEMS, load.getProblemsHours());
                                    addLoad(cargasSemanais, curricularCourse, load, ShiftType.LABORATORIAL, load.getLaboratorialHours());
                                    addLoad(cargasSemanais, curricularCourse, load, ShiftType.TRAINING_PERIOD, load.getTrainingPeriodHours());
                                    addLoad(cargasSemanais, curricularCourse, load, ShiftType.FIELD_WORK, load.getFieldWorkHours());
                                    addLoad(cargasSemanais, curricularCourse, load, ShiftType.TUTORIAL_ORIENTATION, load.getTutorialOrientationHours());
                                    addLoad(cargasSemanais, curricularCourse, load, ShiftType.SEMINARY, load.getSeminaryHours());
                                }
                            }
                        }
                    });
//        } else {
/*
            executionSemester.getAssociatedExecutionCoursesSet().stream()
                    .filter(ec -> isFirstOrSecondCycle(ec))
                    .forEach(ec -> {
                        {
                            final Spreadsheet.Row row = disciplinas.addRow();
                            row.setCell("Nome", ec.getName());
                            row.setCell("Sigla", ec.getSigla());
                            row.setCell("Codigo", ec.getSigla());
                            row.setCell("Importancia", 1);
                            row.setCell("AreaCientifica", departmentFor(ec));
                            row.setCell("ECTS", creditsFor(ec));
                        }
                        ec.getShiftTypes().stream()
                                .map(ec::getCourseLoadByShiftType)
                                .filter(l -> l.getTotalQuantity().compareTo(BigDecimal.ZERO) != 0)
                                .forEach(load -> {
                                    final ShiftType type = load.getType();
                                    BigDecimal totalQuantity = load.getTotalQuantity(), unitQuantity = load.getUnitQuantity();
                                    if (unitQuantity == null || unitQuantity.compareTo(BigDecimal.ZERO) == 0 || unitQuantity.compareTo(totalQuantity) > 0) { // Unit quantity not defined or erroneous, look into base semester's lessons
                                        Set<BigDecimal> quantities = load.getShiftsSet().stream()
                                                .flatMap(s -> s.getAssociatedLessonsSet().stream())
                                                .map(Lesson::getUnitHours).collect(Collectors.toSet());
                                        if (quantities.size() == 1) { // All lessons agree in duration, use that value
                                            unitQuantity = quantities.iterator().next();
                                        } else { // Varying lesson hours, estimate a uniform duration and frequency that fills weekly hours estimate with minimal overhead [if total quantity is erroneous will at least account for minimal lesson time]
                                            BigDecimal minClassSlots = quantities.stream().map(q -> q.multiply(slotsPerHour).setScale(0, RoundingMode.CEILING)).min(Comparator.naturalOrder()).orElse(ExportBulletSchedules.minClassSlots);
                                            BigDecimal maxClassSlots = quantities.stream().map(q -> q.multiply(slotsPerHour).setScale(0, RoundingMode.CEILING)).max(Comparator.naturalOrder()).orElse(ExportBulletSchedules.maxClassSlots);

                                            BigDecimal slotsPerWeek = minClassSlots.max(load.getWeeklyHours().multiply(ExportBulletSchedules.slotsPerHour));

                                            Set<BigDecimal> repeatCandidates = Stream.iterate(minClassSlots, s -> s.add(BigDecimal.ONE))
                                                    .limit(maxClassSlots.subtract(minClassSlots).intValue() + 1)
                                                    .map(n -> slotsPerWeek.divide(n, RoundingMode.UP))
                                                    .collect(Collectors.toSet());
                                            Optional<BigDecimal> bestFitRepeat = repeatCandidates.stream()
                                                    .filter(n -> n.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0)
                                                    .min(Comparator.naturalOrder());
                                            if (!bestFitRepeat.isPresent()) {
                                                bestFitRepeat = repeatCandidates.stream()
                                                        .min(Comparator.comparing((BigDecimal n) -> n.remainder(BigDecimal.ONE)).reversed().thenComparing(Comparator.naturalOrder()));
                                            }
                                            unitQuantity = load.getWeeklyHours().divide(bestFitRepeat.get().setScale(0, RoundingMode.UP), RoundingMode.UP);
                                        }
                                    }
                                    String weeks = "";
                                    int slotNumber, repeats, shifts;
                                    slotNumber = unitQuantity.multiply(ExportBulletSchedules.slotsPerHour).setScale(0, RoundingMode.CEILING).intValue();
                                    repeats = load.getWeeklyHours().divide(unitQuantity, RoundingMode.UP).setScale(0, RoundingMode.CEILING).intValue();
                                    //XXX At least one shift when no shifts are defined, it's probably more correct to calculate this
                                    // from an estimate of the number of students enrolled. Bullet Class has similar issues, as it's based on shifts as well.
                                    shifts = Math.max(1, ec.getNumberOfShifts(type));

                                    final Spreadsheet.Row row = cargasSemanais.addRow();
                                    row.setCell("CodigoDisciplina", ec.getSigla());
                                    row.setCell("NomeCargaSemanal", type.getFullNameTipoAula());
                                    row.setCell("NomeTipologia", type.getSiglaTipoAula());
                                    row.setCell("NumSlots", String.valueOf(slotNumber));
                                    row.setCell("Repeticao", String.valueOf(repeats));
                                    row.setCell("NumTurnos", String.valueOf(shifts));
                                    row.setCell("Semanas", weeks);
                                });
                    });
 */
//        }

        final Spreadsheet planosCurriculares = cargasSemanais.addSpreadsheet("PlanosCurriculares");
        degreeCurricularPlanStream(executionSemester)
                .forEach(degreeCurricularPlan -> {
                    Bennu.getInstance().getCurricularSemestersSet().stream()
                            .filter(cs -> executionSemester.getSemester().equals(cs.getSemester()))
                            .forEach(curricularSemester -> {
                                final String courses = getTargetExecutions(degreeCurricularPlan, executionSemester, curricularSemester)
                                        .map(CurricularCourse::getExternalId).collect(Collectors.joining(","));
                                if (!courses.isEmpty()) {
                                    final Spreadsheet.Row row = planosCurriculares.addRow();
                                    final String siglaDCP = degreeCurricularPlan.getName();
                                    final String siglaCurso = degreeCurricularPlan.getDegree().getSigla();
                                    row.setCell("Nome", String.join(" - ",
                                            siglaDCP,
                                            curricularSemester.getCurricularYear().getYear() + "",
                                            curricularSemester.getSemester() + ""));
                                    row.setCell("Codigo", String.join("/",
                                            siglaDCP,
                                            curricularSemester.getCurricularYear().getYear().toString(),
                                            curricularSemester.getSemester().toString()));
                                    row.setCell("CodigoCurso", siglaCurso);
                                    row.setCell("CodigoDisciplinaObrigatorio", courses);
                                    row.setCell("CodigoDisciplinaOpcional", "");
                                }
                            });
                });

        final Spreadsheet turmas = planosCurriculares.addSpreadsheet("Turmas");
        for (final SchoolClass schoolClass : executionSemester.getSchoolClassesSet()) {
            final DegreeCurricularPlan degreeCurricularPlan = schoolClass.getExecutionDegree().getDegreeCurricularPlan();
            if (considerDegree(degreeCurricularPlan)) {
                final int curricularYear = schoolClass.getAnoCurricular();
                final int semester = schoolClass.getExecutionPeriod().getSemester();

                final Spreadsheet.Row row = turmas.addRow();
                row.setCell("Nome", schoolClass.getNome());
                row.setCell("CodigoPlanoCurricular", degreeCurricularPlan.getName() + "/" + curricularYear + "/" + semester);
                row.setCell("NumeroAlunos", Long.toString(schoolClass.getAssociatedShiftsSet().stream()
                        .flatMap(shift -> shift.getStudentsSet().stream())
                        .filter(registration -> registration.getDegree() == degreeCurricularPlan.getDegree())
                        .map(registration -> registration.getStudent())
                        .distinct()
                        .count()));
                row.setCell("LimiteMaximo", 8);
                row.setCell("LimiteConsecutivo", 5);
            }
        }

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(stream);
        output("bullet_" + executionSemester.getExecutionYear().getYear() + "_" + executionSemester.getSemester() + ".xls", stream.toByteArray());
    }

    private void addLoad(final Spreadsheet spreadsheet, final CurricularCourse curricularCourse,
                         final CompetenceCourseLoad load, final ShiftType type, final Double value) {
        if (value != null && value.doubleValue() > 0d) {
            final Spreadsheet.Row row = spreadsheet.addRow();
            row.setCell("CodigoDisciplina", curricularCourse.getExternalId());
            row.setCell("NomeCargaSemanal", type.getFullNameTipoAula());
            row.setCell("NomeTipologia", type.getSiglaTipoAula());
            row.setCell("NumSlots", "");
            row.setCell("Repeticao", "");
            row.setCell("NumTurnos", "");
            row.setCell("Semanas", "");
            row.setCell("Carga Semanal", value.toString());
        }
    }

    private Stream<Degree> degreeStream(final ExecutionSemester executionSemester) {
        final Stream<Degree> stream1 = executionSemester.getExecutionYear().getExecutionDegreesSet().stream()
                .map(ed -> ed.getDegree());
        final Stream<Degree> stream2 = Bennu.getInstance().getDegreeCurricularPlansSet().stream()
//                .filter(dcp -> dcp.getCurricularStage() == CurricularStage.DRAFT)
                .map(dcp -> dcp.getDegree());
        return Stream.concat(stream1, stream2)
                .filter(d -> considerDegree(d))
                .distinct();
    }

    private Stream<DegreeCurricularPlan> degreeCurricularPlanStream(final ExecutionSemester executionSemester) {
        final Stream<DegreeCurricularPlan> stream1 = executionSemester.getExecutionYear().getExecutionDegreesSet()
                .stream().map(ed -> ed.getDegreeCurricularPlan());
        final Stream<DegreeCurricularPlan> stream2 = Bennu.getInstance().getDegreeCurricularPlansSet().stream();
//                .filter(dcp -> dcp.getCurricularStage() == CurricularStage.DRAFT);
        return Stream.concat(stream1, stream2)
                .filter(d -> considerDegree(d))
                .distinct();
    }

    private Stream<CurricularCourse> curricularCourseStream(final ExecutionSemester executionSemester) {
        final Stream<DegreeCurricularPlan> stream1 = executionSemester.getExecutionYear().getExecutionDegreesSet()
                .stream().map(ed -> ed.getDegreeCurricularPlan());
        final Stream<DegreeCurricularPlan> stream2 = Bennu.getInstance().getDegreeCurricularPlansSet().stream();
//                .filter(dcp -> dcp.getCurricularStage() == CurricularStage.DRAFT);
        return Stream.concat(stream1, stream2)
                .filter(d -> considerDegree(d))
                .distinct()
                .map(dcp -> dcp.getRoot())
                .flatMap(cg -> curricularCourseStream(executionSemester, cg))
                .distinct();
    }

    private Stream<CurricularCourse> curricularCourseStream(final ExecutionSemester executionSemester, final CourseGroup courseGroup) {
        return courseGroup.getChildContextsSet().stream()
                .filter(context -> context.isOpen(executionSemester) && (context.getCurricularPeriod() == null || context.containsSemester(executionSemester.getSemester())))
                .flatMap(context -> curricularCourseStream(executionSemester, context));
    }

    private Stream<CurricularCourse> curricularCourseStream(final ExecutionSemester executionSemester, final Context context) {
        final DegreeModule degreeModule = context.getChildDegreeModule();
        return degreeModule.isCourseGroup() ? curricularCourseStream(executionSemester, (CourseGroup) degreeModule)
                : Stream.of((CurricularCourse) degreeModule);
    }

    public Stream<CurricularCourse> getTargetExecutions(final DegreeCurricularPlan degreeCurricularPlan,
                                                        final ExecutionSemester executionSemester,
                                                        final CurricularSemester curricularSemester) {
        final CourseGroup courseGroup = degreeCurricularPlan.getRoot();
        return courseGroup == null ? Stream.empty() : getTargetExecutions(courseGroup, executionSemester, curricularSemester);
    }

    public Stream<CurricularCourse> getTargetExecutions(final CourseGroup courseGroup,
                                                        final ExecutionSemester executionSemester,
                                                        final CurricularSemester curricularSemester) {
        return courseGroup.getChildContextsSet().stream()
                .filter(context -> context.isOpen(executionSemester) && (context.getCurricularPeriod() == null || context.containsSemester(executionSemester.getSemester())))
                .filter(context -> context.getCurricularPeriod() == null || context.getCurricularYear().intValue() == curricularSemester.getCurricularYear().getYear().intValue())
                .filter(context -> context.getCurricularPeriod() == null || context.getCurricularPeriod().getChildOrder().intValue() == curricularSemester.getSemester().intValue())
                .flatMap(context -> getTargetExecutions(context, executionSemester, curricularSemester));
    }

    public Stream<CurricularCourse> getTargetExecutions(final Context context,
                                                        final ExecutionSemester executionSemester,
                                                        final CurricularSemester curricularSemester) {
        final DegreeModule degreeModule = context.getChildDegreeModule();
        return degreeModule.isCourseGroup() ? getTargetExecutions((CourseGroup) degreeModule, executionSemester, curricularSemester)
                : Stream.of((CurricularCourse) degreeModule);
    }

    private boolean isFirstOrSecondCycle(final ExecutionCourse executionCourse) {
        return executionCourse.getAssociatedCurricularCoursesSet().stream()
                .filter(cc -> cc.isActive(executionCourse.getExecutionPeriod()))
                .map(cc -> cc.getDegree())
                .anyMatch(d -> considerDegree(d));
    }

    private boolean isSpaceType(final Space space, final String... types) {
        try {
            final String name = space.getClassification().getName().getContent();
            for (final String type : types) {
                if (type.equals(name)) {
                    return true;
                }
            }
        } catch (NoSuchElementException ex) {
        }
        return false;
    }

    private Stream<Space> roomStream(final Lesson lesson) {
        return Stream.concat(lesson.getLessonInstancesSet().stream().map(i -> i.getRoom()).filter(r -> r != null),
                Stream.of(lesson).map(l -> l.getRoomOccupation()).filter(o -> o != null).map(o -> o.getRoom()));
    }

    private Space campusFor(final Space space) {
        return space == null || isSpaceType(space, "Campus") ? space : campusFor(space.getParent());
    }

    private Space buildingFor(final Space space) {
        return space == null || isSpaceType(space, "Building", "Edifício") ? space : buildingFor(space.getParent());
    }

    private Space levelFor(final Space space) {
        return space == null || isSpaceType(space, "Floor", "Piso") ? space : levelFor(space.getParent());
    }

    protected String levelName(final Space space) {
        return space == null ? "" : levelName(space.getParent(), space.getName());
    }

    private String levelName(final Space space, final String childName) {
        if (space == null) {
            return childName;
        }
        final String name = isSpaceType(space, "Floor", "Piso") ? space.getName() + '.' + childName : childName;
        return levelName(space.getParent(), name);
    }

    private String roomFullPath(Space room) {
        return campusFor(room).getName()
                + " > " + buildingFor(room).getName()
                + " > " + levelName(levelFor(room))
                + " > " + room.getName();
    }

    private String creditsFor(final ExecutionCourse course) {
        try {
            return course.getEctsCredits() == null ? "0" : course.getEctsCredits().toString();
        } catch (final DomainException ex) {
            return "Data is not consistent";
        }
    }

    private String creditsFor(final CurricularCourse curricularCourse, final ExecutionSemester executionSemester) {
        try {
            final Double credits = curricularCourse.getEctsCredits(executionSemester);
            return credits == null ? "0" : credits.toString();
        } catch (final DomainException ex) {
            return "Data is not consistent";
        }
    }

    private String departmentFor(final ExecutionCourse executionCourse) {
        final Set<Department> departments = executionCourse.getAssociatedCurricularCoursesSet().stream()
                .filter(cc -> cc.isActive(executionCourse.getExecutionPeriod()))
                .map(cc -> cc.getCompetenceCourse())
                .filter(cc -> cc != null)
                .map(cc -> cc.getDepartmentUnit(executionCourse.getExecutionPeriod()))
                .filter(du -> du != null)
                .map(du -> du.getDepartment())
                .filter(d -> d != null)
                .collect(Collectors.toSet());
        return departments.stream().map(d -> d.getName()).findAny().orElse("");
    }

    private String departmentFor(final CurricularCourse curricularCourse, final ExecutionSemester executionSemester) {
        final CompetenceCourse competenceCourse = curricularCourse.getCompetenceCourse();
        if (competenceCourse != null) {
            final DepartmentUnit departmentUnit = competenceCourse.getDepartmentUnit(executionSemester);
            if (departmentUnit != null) {
                final Department department = departmentUnit.getDepartment();
                if (department != null) {
                    return department.getName();
                }
            }
        }
        return "";
    }

    private boolean considerDegree(final Degree degree) {
        return degree.isFirstCycle() || degree.isSecondCycle() || degree.getDegreeType().getMinor();
    }

    private boolean considerDegree(final DegreeCurricularPlan dcp) {
        return considerDegree(dcp.getDegree());
    }

}