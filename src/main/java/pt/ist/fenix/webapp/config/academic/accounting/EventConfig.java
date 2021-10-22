package pt.ist.fenix.webapp.config.academic.accounting;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.EventTemplate;

import java.util.HashMap;
import java.util.Map;

public class EventConfig {

    public enum EventTemplateCode {

        CYCLE_1_NORMAL,
        CYCLE_2_CONTINUATION,
        CYCLE_2_NORMAL,
        CYCLE_2_MICROBIOLOGY,
        CYCLE_2_MOTU,
        CYCLE_2_PHARMACEUTICAL,
        CYCLE_2_ADVANCED,

        INTERNATIONAL,
        INTERNATIONAL_MOTU,

        ISOLATED_COURSES_INTERNAL,
        ISOLATED_COURSES_INTERNAL_INTERNATIONAL,
        ISOLATED_COURSES_EXTERNAL,

        MILITARY;

        public EventTemplate eventTemplate() {
            return EventTemplate.readByCode(name());
        }

    }

    public static final Map<Degree, EventTemplateCode> degreeEventTemplateMap() {
        final Map<Degree, EventTemplateCode> degreeApplicationMap = new HashMap<>();

        ExecutionYear.readCurrentExecutionYear().getExecutionDegreesSet().stream()
                .map(executionDegree -> executionDegree.getDegreeCurricularPlan().getDegree())
                .filter(degree -> degree.isFirstCycle())
                .forEach(degree -> degreeApplicationMap.put(degree, EventConfig.EventTemplateCode.CYCLE_1_NORMAL));

        degreeApplicationMap.put(Degree.readBySigla("MEAer21"), EventConfig.EventTemplateCode.CYCLE_2_CONTINUATION);
        degreeApplicationMap.put(Degree.readBySigla("MEBiol21"), EventConfig.EventTemplateCode.CYCLE_2_CONTINUATION);
        degreeApplicationMap.put(Degree.readBySigla("MEBiom21"), EventConfig.EventTemplateCode.CYCLE_2_CONTINUATION);
        degreeApplicationMap.put(Degree.readBySigla("MEC21"), EventConfig.EventTemplateCode.CYCLE_2_CONTINUATION);
        degreeApplicationMap.put(Degree.readBySigla("MEMat"), EventConfig.EventTemplateCode.CYCLE_2_CONTINUATION);
        degreeApplicationMap.put(Degree.readBySigla("MERC"), EventConfig.EventTemplateCode.CYCLE_2_CONTINUATION);
        degreeApplicationMap.put(Degree.readBySigla("MEAmb"), EventConfig.EventTemplateCode.CYCLE_2_CONTINUATION);
        degreeApplicationMap.put(Degree.readBySigla("MEGI"), EventConfig.EventTemplateCode.CYCLE_2_CONTINUATION);
        degreeApplicationMap.put(Degree.readBySigla("MEE"), EventConfig.EventTemplateCode.CYCLE_2_CONTINUATION);
        degreeApplicationMap.put(Degree.readBySigla("MEEC21"), EventConfig.EventTemplateCode.CYCLE_2_CONTINUATION);
        degreeApplicationMap.put(Degree.readBySigla("MEFT21"), EventConfig.EventTemplateCode.CYCLE_2_CONTINUATION);
        degreeApplicationMap.put(Degree.readBySigla("MEGM"), EventConfig.EventTemplateCode.CYCLE_2_CONTINUATION);
        degreeApplicationMap.put(Degree.readBySigla("MEIC-A"), EventConfig.EventTemplateCode.CYCLE_2_CONTINUATION);
        degreeApplicationMap.put(Degree.readBySigla("MEIC-T"), EventConfig.EventTemplateCode.CYCLE_2_CONTINUATION);
        degreeApplicationMap.put(Degree.readBySigla("MEMec21"), EventConfig.EventTemplateCode.CYCLE_2_CONTINUATION);
        degreeApplicationMap.put(Degree.readBySigla("MEAN"), EventConfig.EventTemplateCode.CYCLE_2_CONTINUATION);
        degreeApplicationMap.put(Degree.readBySigla("MEQ21"), EventConfig.EventTemplateCode.CYCLE_2_CONTINUATION);
        degreeApplicationMap.put(Degree.readBySigla("MMA"), EventConfig.EventTemplateCode.CYCLE_2_CONTINUATION);

        degreeApplicationMap.put(Degree.readBySigla("MBMRP21"), EventConfig.EventTemplateCode.CYCLE_2_NORMAL);
        degreeApplicationMap.put(Degree.readBySigla("MBioNano"), EventConfig.EventTemplateCode.CYCLE_2_NORMAL);
        degreeApplicationMap.put(Degree.readBySigla("MBiotec"), EventConfig.EventTemplateCode.CYCLE_2_NORMAL);
        degreeApplicationMap.put(Degree.readBySigla("MCTPC"), EventConfig.EventTemplateCode.CYCLE_2_NORMAL);
        degreeApplicationMap.put(Degree.readBySigla("MEP"), EventConfig.EventTemplateCode.CYCLE_2_NORMAL);
        degreeApplicationMap.put(Degree.readBySigla("MEGE"), EventConfig.EventTemplateCode.CYCLE_2_NORMAL);
        degreeApplicationMap.put(Degree.readBySigla("MISE"), EventConfig.EventTemplateCode.CYCLE_2_NORMAL);
        degreeApplicationMap.put(Degree.readBySigla("MPSR"), EventConfig.EventTemplateCode.CYCLE_2_NORMAL);
        degreeApplicationMap.put(Degree.readBySigla("MST"), EventConfig.EventTemplateCode.CYCLE_2_NORMAL);
        degreeApplicationMap.put(Degree.readBySigla("MQ"), EventConfig.EventTemplateCode.CYCLE_2_NORMAL);

        degreeApplicationMap.put(Degree.readBySigla("MicroBio"), EventConfig.EventTemplateCode.CYCLE_2_MICROBIOLOGY);

        degreeApplicationMap.put(Degree.readBySigla("MOTU"), EventConfig.EventTemplateCode.CYCLE_2_MOTU);

        degreeApplicationMap.put(Degree.readBySigla("MEFarm"), EventConfig.EventTemplateCode.CYCLE_2_PHARMACEUTICAL);

        degreeApplicationMap.put(Degree.readBySigla("MECD"), EventConfig.EventTemplateCode.CYCLE_2_ADVANCED);
        degreeApplicationMap.put(Degree.readBySigla("MEGIE"), EventConfig.EventTemplateCode.CYCLE_2_ADVANCED);
        degreeApplicationMap.put(Degree.readBySigla("MSIDC"), EventConfig.EventTemplateCode.CYCLE_2_ADVANCED);

        return degreeApplicationMap;
    }

}
