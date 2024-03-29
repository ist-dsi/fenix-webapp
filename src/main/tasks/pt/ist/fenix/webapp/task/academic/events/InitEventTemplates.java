package pt.ist.fenix.webapp.task.academic.events;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.EventTemplate;
import org.fenixedu.academic.domain.accounting.EventTemplateConfig;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.joda.time.DateTime;
import pt.ist.fenix.webapp.config.academic.accounting.EventConfig;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.Collator;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class InitEventTemplates extends CustomTask {

    private static final Locale PT = new Locale("pt", "PT");
    private static final Locale EN = new Locale("en", "GB");

    private static final DateTime APPLY_FROM = new DateTime(2021, 9, 13, 0, 0, 0, 0);
    private static final DateTime APPLY_UNTIL = new DateTime(2022, 8, 31, 23, 59, 59, 0);

    @Override
    public void runTask() throws Exception {
        createEventTemplate(EventConfig.EventTemplateCode.CYCLE_1_NORMAL.name(),
                ls("1º Ciclo Normal", "1st Cycle Normal"),
                ls("Plano de Pagamentos para alunos normais de 1º Ciclo", "Payment plan for normal 1st Cycle students"),
                tuitionMap -> {
                    tuitionMap.addProperty("11/11/2021", "69.70");
                    tuitionMap.addProperty("11/12/2021", "69.70");
                    tuitionMap.addProperty("11/01/2022", "69.70");
                    tuitionMap.addProperty("11/02/2022", "69.70");
                    tuitionMap.addProperty("11/03/2022", "69.70");
                    tuitionMap.addProperty("11/04/2022", "69.70");
                    tuitionMap.addProperty("11/05/2022", "69.70");
                    tuitionMap.addProperty("11/06/2022", "69.70");
                    tuitionMap.addProperty("11/07/2022", "69.70");
                    tuitionMap.addProperty("11/08/2022", "69.70");
                }, insuranceMap -> {
                    insuranceMap.addProperty("11/11/2021", "2.03");
                }, adminFeesMap -> {
                    adminFeesMap.addProperty("31/12/2021", "30");
                }, adminFeesPenaltyMap -> {
                    adminFeesPenaltyMap.addProperty("31/12/2021", "15");
                }, "0027", "PROPINAS 1 CICLO");

        createEventTemplate(EventConfig.EventTemplateCode.CYCLE_INTEGRATED_NORMAL.name(),
                ls("Mestrado Integrado Normal", "Mestrado Integrado Normal"),
                ls("Plano de Pagamentos para alunos normais de Mestrado Integrado", "Payment plan for normal students of integrated master degrees"),
                tuitionMap -> {
                    tuitionMap.addProperty("11/11/2021", "69.70");
                    tuitionMap.addProperty("11/12/2021", "69.70");
                    tuitionMap.addProperty("11/01/2022", "69.70");
                    tuitionMap.addProperty("11/02/2022", "69.70");
                    tuitionMap.addProperty("11/03/2022", "69.70");
                    tuitionMap.addProperty("11/04/2022", "69.70");
                    tuitionMap.addProperty("11/05/2022", "69.70");
                    tuitionMap.addProperty("11/06/2022", "69.70");
                    tuitionMap.addProperty("11/07/2022", "69.70");
                    tuitionMap.addProperty("11/08/2022", "69.70");
                }, insuranceMap -> {
                    insuranceMap.addProperty("11/11/2021", "2.03");
                }, adminFeesMap -> {
                    adminFeesMap.addProperty("31/12/2021", "30");
                }, adminFeesPenaltyMap -> {
                    adminFeesPenaltyMap.addProperty("31/12/2021", "15");
                }, "0030", "PROPINAS MESTRADO INTEGRADO");

        createEventTemplate(EventConfig.EventTemplateCode.CYCLE_2_CONTINUATION.name(),
                ls("2º Ciclo Continuidade", "2nd Cycle Continuation"),
                ls("Plano de Pagamentos para alunos de 2º Ciclo em cursos de continuidade", "Payment plan 2nd Cycle students in continuation degrees"),
                tuitionMap -> {
                    tuitionMap.addProperty("11/11/2021", "82.50");
                    tuitionMap.addProperty("11/12/2021", "82.50");
                    tuitionMap.addProperty("11/01/2022", "82.50");
                    tuitionMap.addProperty("11/02/2022", "82.50");
                    tuitionMap.addProperty("11/03/2022", "82.50");
                    tuitionMap.addProperty("11/04/2022", "82.50");
                    tuitionMap.addProperty("11/05/2022", "82.50");
                    tuitionMap.addProperty("11/06/2022", "82.50");
                    tuitionMap.addProperty("11/07/2022", "82.50");
                    tuitionMap.addProperty("11/08/2022", "82.50");
                }, insuranceMap -> {
                    insuranceMap.addProperty("11/11/2021", "2.03");
                }, adminFeesMap -> {
                    adminFeesMap.addProperty("31/12/2021", "30");
                }, adminFeesPenaltyMap -> {
                    adminFeesPenaltyMap.addProperty("31/12/2021", "15");
                }, "0028", "PROPINAS 2 CICLO");

        createEventTemplate(EventConfig.EventTemplateCode.CYCLE_2_NORMAL.name(),
                ls("2º Ciclo Normal", "2nd Cycle Normal"),
                ls("Plano de Pagamentos para alunos de 2º Ciclo normais", "Payment plan 2nd Cycle students"),
                tuitionMap -> {
                    tuitionMap.addProperty("11/11/2021", "106");
                    tuitionMap.addProperty("11/12/2021", "106");
                    tuitionMap.addProperty("11/01/2022", "106");
                    tuitionMap.addProperty("11/02/2022", "106");
                    tuitionMap.addProperty("11/03/2022", "106");
                    tuitionMap.addProperty("11/04/2022", "106");
                    tuitionMap.addProperty("11/05/2022", "106");
                    tuitionMap.addProperty("11/06/2022", "106");
                    tuitionMap.addProperty("11/07/2022", "106");
                    tuitionMap.addProperty("11/08/2022", "106");
                }, insuranceMap -> {
                    insuranceMap.addProperty("11/11/2021", "2.03");
                }, adminFeesMap -> {
                    adminFeesMap.addProperty("31/12/2021", "30");
                }, adminFeesPenaltyMap -> {
                    adminFeesPenaltyMap.addProperty("31/12/2021", "15");
                }, "0028", "PROPINAS 2 CICLO");

        createEventTemplate(EventConfig.EventTemplateCode.CYCLE_2_MICROBIOLOGY.name(),
                ls("2º Ciclo Microbiologia", "2nd Cycle Microbiology"),
                ls("Plano de Pagamentos para alunos de 2º Ciclo de Microbiologia", "Payment plan 2nd Cycle students of Microbiology"),
                tuitionMap -> {
                    tuitionMap.addProperty("11/11/2021", "106.41");
                    tuitionMap.addProperty("11/12/2021", "106.34");
                    tuitionMap.addProperty("11/01/2022", "106.34");
                    tuitionMap.addProperty("11/02/2022", "106.34");
                    tuitionMap.addProperty("11/03/2022", "106.34");
                    tuitionMap.addProperty("11/04/2022", "106.34");
                    tuitionMap.addProperty("11/05/2022", "106.34");
                    tuitionMap.addProperty("11/06/2022", "106.34");
                    tuitionMap.addProperty("11/07/2022", "106.34");
                    tuitionMap.addProperty("11/08/2022", "106.34");
                }, insuranceMap -> {
                    insuranceMap.addProperty("11/11/2021", "2.03");
                }, adminFeesMap -> {
                    adminFeesMap.addProperty("31/12/2021", "30");
                }, adminFeesPenaltyMap -> {
                    adminFeesPenaltyMap.addProperty("31/12/2021", "15");
                }, "0028", "PROPINAS 2 CICLO");

        createEventTemplate(EventConfig.EventTemplateCode.CYCLE_2_MOTU.name(),
                ls("2º Ciclo MOTU", "2nd Cycle MOTU"),
                ls("Plano de Pagamentos para alunos de 2º Ciclo de MOTU", "Payment plan 2nd Cycle students of MOTU"),
                tuitionMap -> {
                    tuitionMap.addProperty("11/11/2021", "120");
                    tuitionMap.addProperty("11/12/2021", "120");
                    tuitionMap.addProperty("11/01/2022", "120");
                    tuitionMap.addProperty("11/02/2022", "120");
                    tuitionMap.addProperty("11/03/2022", "120");
                    tuitionMap.addProperty("11/04/2022", "120");
                    tuitionMap.addProperty("11/05/2022", "120");
                    tuitionMap.addProperty("11/06/2022", "120");
                    tuitionMap.addProperty("11/07/2022", "120");
                    tuitionMap.addProperty("11/08/2022", "120");
                }, insuranceMap -> {
                    insuranceMap.addProperty("11/11/2021", "2.03");
                }, adminFeesMap -> {
                    adminFeesMap.addProperty("31/12/2021", "30");
                }, adminFeesPenaltyMap -> {
                    adminFeesPenaltyMap.addProperty("31/12/2021", "15");
                }, "0028", "PROPINAS 2 CICLO");

        createEventTemplate(EventConfig.EventTemplateCode.CYCLE_2_PHARMACEUTICAL.name(),
                ls("2º Ciclo Farmacêutica", "2nd Cycle Farmacêutica"),
                ls("Plano de Pagamentos para alunos de 2º Ciclo de Farmacêutica", "Payment plan 2nd Cycle students of Farmacêutica"),
                tuitionMap -> {
                    tuitionMap.addProperty("11/11/2021", "212");
                    tuitionMap.addProperty("11/12/2021", "212");
                    tuitionMap.addProperty("11/01/2022", "212");
                    tuitionMap.addProperty("11/02/2022", "212");
                    tuitionMap.addProperty("11/03/2022", "212");
                    tuitionMap.addProperty("11/04/2022", "212");
                    tuitionMap.addProperty("11/05/2022", "212");
                    tuitionMap.addProperty("11/06/2022", "212");
                    tuitionMap.addProperty("11/07/2022", "212");
                    tuitionMap.addProperty("11/08/2022", "212");
                }, insuranceMap -> {
                    insuranceMap.addProperty("11/11/2021", "2.03");
                }, adminFeesMap -> {
                    adminFeesMap.addProperty("31/12/2021", "30");
                }, adminFeesPenaltyMap -> {
                    adminFeesPenaltyMap.addProperty("31/12/2021", "15");
                }, "0028", "PROPINAS 2 CICLO");

        createEventTemplate(EventConfig.EventTemplateCode.CYCLE_2_ADVANCED.name(),
                ls("2º Ciclo Avançado", "2nd Cycle Advanced"),
                ls("Plano de Pagamentos para alunos de 2º Ciclo Avançado", "Payment plan 2nd Cycle students of Advanced"),
                tuitionMap -> {
                    tuitionMap.addProperty("11/11/2021", "250");
                    tuitionMap.addProperty("11/12/2021", "250");
                    tuitionMap.addProperty("11/01/2022", "250");
                    tuitionMap.addProperty("11/02/2022", "250");
                    tuitionMap.addProperty("11/03/2022", "250");
                    tuitionMap.addProperty("11/04/2022", "250");
                    tuitionMap.addProperty("11/05/2022", "250");
                    tuitionMap.addProperty("11/06/2022", "250");
                    tuitionMap.addProperty("11/07/2022", "250");
                    tuitionMap.addProperty("11/08/2022", "250");
                }, insuranceMap -> {
                    insuranceMap.addProperty("11/11/2021", "2.03");
                }, adminFeesMap -> {
                    adminFeesMap.addProperty("31/12/2021", "30");
                }, adminFeesPenaltyMap -> {
                    adminFeesPenaltyMap.addProperty("31/12/2021", "15");
                }, "0028", "PROPINAS 2 CICLO");

        createEventTemplate(EventConfig.EventTemplateCode.INTERNATIONAL.name(),
                ls("Estudantes Internacionais", "International Students"),
                ls("Plano de Pagamentos para alunos Internacionais", "Payment plan for International Students"),
                tuitionMap -> {
                    tuitionMap.addProperty("11/11/2021", "700");
                    tuitionMap.addProperty("11/12/2021", "700");
                    tuitionMap.addProperty("11/01/2022", "700");
                    tuitionMap.addProperty("11/02/2022", "700");
                    tuitionMap.addProperty("11/03/2022", "700");
                    tuitionMap.addProperty("11/04/2022", "700");
                    tuitionMap.addProperty("11/05/2022", "700");
                    tuitionMap.addProperty("11/06/2022", "700");
                    tuitionMap.addProperty("11/07/2022", "700");
                    tuitionMap.addProperty("11/08/2022", "700");
                }, insuranceMap -> {
                    insuranceMap.addProperty("11/11/2021", "2.03");
                }, adminFeesMap -> {
                    adminFeesMap.addProperty("31/12/2021", "30");
                }, adminFeesPenaltyMap -> {
                    adminFeesPenaltyMap.addProperty("31/12/2021", "15");
                }, "0075", "PROPINAS INTERNACIONAL");

        createEventTemplate(EventConfig.EventTemplateCode.INTERNATIONAL_MOTU.name(),
                ls("Estudantes Internacionais MOTU", "International Students MOTU"),
                ls("Plano de Pagamentos para alunos Internacionais MOTU", "Payment plan for International Students MOTU"),
                tuitionMap -> {
                    tuitionMap.addProperty("11/11/2021", "350");
                    tuitionMap.addProperty("11/12/2021", "350");
                    tuitionMap.addProperty("11/01/2022", "350");
                    tuitionMap.addProperty("11/02/2022", "350");
                    tuitionMap.addProperty("11/03/2022", "350");
                    tuitionMap.addProperty("11/04/2022", "350");
                    tuitionMap.addProperty("11/05/2022", "350");
                    tuitionMap.addProperty("11/06/2022", "350");
                    tuitionMap.addProperty("11/07/2022", "350");
                    tuitionMap.addProperty("11/08/2022", "350");
                }, insuranceMap -> {
                    insuranceMap.addProperty("11/11/2021", "2.03");
                }, adminFeesMap -> {
                    adminFeesMap.addProperty("31/12/2021", "30");
                }, adminFeesPenaltyMap -> {
                    adminFeesPenaltyMap.addProperty("31/12/2021", "15");
                }, "0075", "PROPINAS INTERNACIONAL");

        createEventTemplate(EventConfig.EventTemplateCode.ISOLATED_COURSES_INTERNAL.name(),
                ls("Unidades Curriculares Isoladas Normal", "Isolated Courses Normal"),
                ls("Plano de Pagamentos para aluno internos normais", "Payment plan for normal internal students"),
                tuitionMap -> {
                }, insuranceMap -> {
                }, adminFeesMap -> {
                }, adminFeesPenaltyMap -> {
                },14, "42", "0076", "PROPINAS OUTROS");

        createEventTemplate(EventConfig.EventTemplateCode.ISOLATED_COURSES_INTERNAL_INTERNATIONAL.name(),
                ls("Unidades Curriculares Isoladas Internacionais", "Isolated Courses International"),
                ls("Plano de Pagamentos para aluno internos internacionais", "Payment plan for international internal students"),
                tuitionMap -> {
                }, insuranceMap -> {
                }, adminFeesMap -> {
                }, adminFeesPenaltyMap -> {
                }, 14, "117", "0076", "PROPINAS OUTROS");

        createEventTemplate(EventConfig.EventTemplateCode.ISOLATED_COURSES_EXTERNAL.name(),
                ls("Unidades Curriculares Isoladas Externos", "Isolated Courses Externals"),
                ls("Plano de Pagamentos para aluno externos", "Payment plan for external students"),
                tuitionMap -> {
                }, insuranceMap -> {
                    insuranceMap.addProperty("11/11/2021", "2.03");
                }, adminFeesMap -> {
                    adminFeesMap.addProperty("31/12/2021", "30");
                }, adminFeesPenaltyMap -> {
                    adminFeesPenaltyMap.addProperty("31/12/2021", "15");
                }, 14, "125", "0076", "PROPINAS OUTROS");

/*
        Bennu.getInstance().getRegistrationProtocolsSet().stream()
                .filter(protocol -> protocol.isMobilityAgreement())
                .forEach(protocol -> {
                    createEventTemplate(protocol.getCode(),
                            ls("Mobilidade ", "Mobility ").append(protocol.getCode()),
                            ls("Plano de Pagamentos para alunos de mobilidade ", "Payment plan for mobility students ").append(protocol.getDescription()),
                            tuitionMap -> {
                            }, insuranceMap -> {
                            }, adminFeesMap -> {
                            }, adminFeesPenaltyMap -> {
                            });
                });
*/

        final Spreadsheet spreadsheet = new Spreadsheet("PlanosPagamento");
        final Spreadsheet dueDateSheet = spreadsheet.addSpreadsheet("PrazosPropinas");
        final Spreadsheet degreeSheet = dueDateSheet.addSpreadsheet("Cursos");
        final Spreadsheet partialSheet = degreeSheet.addSpreadsheet("PlanosPagamentoParciais");
        final Spreadsheet partialDueDateSheet = partialSheet.addSpreadsheet("PrazosPropinasParciais");
        Bennu.getInstance().getEventTemplateSet().stream()
                .filter(eventTemplate -> !eventTemplate.getAlternativeEventTemplateSet().isEmpty())
                .sorted((t1, t2) -> Collator.getInstance().compare(t1.getTitle().getContent(), t2.getTitle().getContent()))
                .forEach(eventTemplate -> {
                    report(spreadsheet, dueDateSheet, eventTemplate);
                    eventTemplate.getAlternativeEventTemplateSet().forEach(alt -> report(partialSheet, partialDueDateSheet, alt));
                });

        final Map<Degree, EventConfig.EventTemplateCode> degreeApplicationMap = EventConfig.degreeEventTemplateMap();

        ExecutionYear.readCurrentExecutionYear().getExecutionDegreesSet().stream()
                .map(executionDegree -> executionDegree.getDegreeCurricularPlan().getDegree())
                .filter(degree -> degree.isFirstCycle() || degree.isSecondCycle())
                .sorted(Degree.COMPARATOR_BY_DEGREE_TYPE_AND_NAME_AND_ID)
                .forEach(degree -> {
                    final EventConfig.EventTemplateCode code = degreeApplicationMap.get(degree);
                    final double tuition = Bennu.getInstance().getEventTemplateSet().stream()
                            .filter(eventTemplate -> code != null && code.name().equals(eventTemplate.getCode()))
                            .flatMap(eventTemplate -> eventTemplate.getEventTemplateConfigSet().stream())
                            .map(config -> config.getConfig().getAsJsonObject(EventTemplate.Type.TUITION.name()))
                            .flatMap(json -> json.getAsJsonObject("dueDateAmountMap").entrySet().stream())
                            .map(Map.Entry::getValue)
                            .mapToDouble(e -> Double.parseDouble(e.getAsString()))
                            .sum();

                    final Spreadsheet.Row degreeRow = degreeSheet.addRow();
                    degreeRow.setCell("Tipo Curse", degree.getDegreeType().getName().getContent());
                    degreeRow.setCell("Sigla", degree.getSigla());
                    degreeRow.setCell("Plano", code == null ? "" : code.name());
                    degreeRow.setCell("Propina", code == null ? "" : Double.toString(tuition));
                    degreeRow.setCell("Curso", degree.getPresentationName());
                });

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(stream);
        output("paymentPlans.xlsx", stream.toByteArray());
    }

    private void report(final Spreadsheet spreadsheet, final Spreadsheet dueDateSheet, final EventTemplate eventTemplate) {
        final EventTemplateConfig templateConfig = eventTemplate.getEventTemplateConfigSet().iterator().next();
        final JsonObject tuitionMap = templateConfig.getConfig().getAsJsonObject(EventTemplate.Type.TUITION.name())
                .getAsJsonObject("dueDateAmountMap");
        final JsonObject insuranceMap = templateConfig.getConfig().getAsJsonObject(EventTemplate.Type.INSURANCE.name())
                .getAsJsonObject("dueDateAmountMap");
        final JsonObject adminFeesMap = templateConfig.getConfig().getAsJsonObject(EventTemplate.Type.ADMIN_FEES.name())
                .getAsJsonObject("dueDateAmountMap");

        final Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell("Code", eventTemplate.getCode());
        row.setCell("Title PT", eventTemplate.getTitle().getContent(PT));
        row.setCell("Title EN", eventTemplate.getTitle().getContent(EN));
        row.setCell("Description PT", eventTemplate.getDescription().getContent(PT));
        row.setCell("Description EN", eventTemplate.getDescription().getContent(EN));
        row.setCell("Apply From", templateConfig.getApplyFrom().toString("yyyy-MM-dd HH:mm"));
        row.setCell("Apply Until", templateConfig.getApplyUntil().toString("yyyy-MM-dd HH:mm"));
        row.setCell("Valor Propina", tuitionMap.entrySet().stream()
                .map(Map.Entry::getValue)
                .mapToDouble(e -> Double.parseDouble(e.getAsString()))
                .sum());
        if (insuranceMap != null && !insuranceMap.entrySet().isEmpty()) {
            row.setCell("Data Limit Seguro", insuranceMap.entrySet().iterator().next().getKey());
            row.setCell("Valor Seguro", insuranceMap.entrySet().iterator().next().getValue().getAsString());
        }
        if (adminFeesMap != null && !adminFeesMap.entrySet().isEmpty()) {
            row.setCell("Data Limit Taxa Admin", adminFeesMap.entrySet().iterator().next().getKey());
            row.setCell("Valor Taxa Admin", adminFeesMap.entrySet().iterator().next().getValue().getAsString());
        }
        row.setCell("Imputação Propinas", "");
        row.setCell("Imputação Seguro", "");
        row.setCell("Imputação Taxas", "");

        final Spreadsheet.Row dueDateRow = dueDateSheet.addRow();
        dueDateRow.setCell("Code", eventTemplate.getCode());

        final JsonObject config = templateConfig.getConfig();
        config.getAsJsonObject(EventTemplate.Type.TUITION.name())
                .getAsJsonObject("dueDateAmountMap")
                .entrySet().forEach(e -> {
            dueDateRow.setCell(e.getKey(), e.getValue().getAsString());
        });
    }

    private JsonObject config(final Consumer<JsonObject> tuitionMapConsumer,
                              final Consumer<JsonObject> insuranceMapConsumer,
                              final Consumer<JsonObject> adminFeesMapConsumer,
                              final Consumer<JsonObject> adminFeesPenaltyMapConsumer,
                              final Integer tuitionByECTSDaysToPay, final String tuitionByECTSValue,
                              final Double maxCredits, final ExecutionSemester semester,
                              final String tuitionProductCode, final String tuitionProductDescription) {
        return JsonUtils.toJson(config -> {
            config.add(EventTemplate.Type.TUITION.name(), JsonUtils.toJson(typeConfig -> {
                typeConfig.add("dueDateAmountMap", JsonUtils.toJson(tuitionMapConsumer));
                if (tuitionByECTSDaysToPay != null && tuitionByECTSValue != null) {
                    typeConfig.add("byECTS", JsonUtils.toJson(byECTS -> {
                        byECTS.addProperty("daysToPay", tuitionByECTSDaysToPay);
                        byECTS.addProperty("value", tuitionByECTSValue);
                    }));
                }
                typeConfig.addProperty("productCode", tuitionProductCode);
                typeConfig.addProperty("productDescription", tuitionProductDescription);
                typeConfig.addProperty("accountId", "287762860391");
            }));
            config.add(EventTemplate.Type.INSURANCE.name(), JsonUtils.toJson(typeConfig -> {
                typeConfig.add("dueDateAmountMap", JsonUtils.toJson(insuranceMapConsumer));
                typeConfig.addProperty("productCode", "0034");
                typeConfig.addProperty("productDescription", "SEGURO ESCOLAR");
                typeConfig.addProperty("accountId", "287762860391");
            }));
            config.add(EventTemplate.Type.ADMIN_FEES.name(), JsonUtils.toJson(typeConfig -> {
                typeConfig.add("dueDateAmountMap", JsonUtils.toJson(adminFeesMapConsumer));
                typeConfig.add("penaltyAmountMap", JsonUtils.toJson(adminFeesPenaltyMapConsumer));
                typeConfig.addProperty("productCode", "0031");
                typeConfig.addProperty("productDescription", "TAXAS DE MATRICULA");
                typeConfig.addProperty("accountId", "287762860391");
            }));
            if (maxCredits != null) {
                config.addProperty("maxCredits", maxCredits);
            }
            if (semester != null) {
                config.addProperty("semester", semester.getExternalId());
            }
        });
    }

    private EventTemplate createEventTemplate(final String code, final LocalizedString title,
                                              final LocalizedString description,
                                              final Consumer<JsonObject> tuitionMapConsumer,
                                              final Consumer<JsonObject> insuranceMapConsumer,
                                              final Consumer<JsonObject> adminFeesMapConsumer,
                                              final Consumer<JsonObject> adminFeesPenaltyMapConsumer,
                                              final String tuitionProductCode, final String tuitionProductDescription) {
        return createEventTemplate(code, title, description, tuitionMapConsumer, insuranceMapConsumer, adminFeesMapConsumer,
                adminFeesPenaltyMapConsumer, null, null, tuitionProductCode, tuitionProductDescription);
    }

    private EventTemplate createEventTemplate(final String code, final LocalizedString title,
                                              final LocalizedString description,
                                              final Consumer<JsonObject> tuitionMapConsumer,
                                              final Consumer<JsonObject> insuranceMapConsumer,
                                              final Consumer<JsonObject> adminFeesMapConsumer,
                                              final Consumer<JsonObject> adminFeesPenaltyMapConsumer,
                                              final Integer tuitionByECTSDaysToPay, final String tuitionByECTSValue,
                                              final String tuitionProductCode, final String tuitionProductDescription) {
        final EventTemplate eventTemplate = new EventTemplate(code, title, description);
        eventTemplate.createConfig(APPLY_FROM, APPLY_UNTIL, config(tuitionMapConsumer, insuranceMapConsumer, adminFeesMapConsumer,
                adminFeesPenaltyMapConsumer, tuitionByECTSDaysToPay, tuitionByECTSValue, null, null,
                tuitionProductCode, tuitionProductDescription));

        if (tuitionByECTSDaysToPay == null && tuitionByECTSValue == null) {
            createEventTemplate(eventTemplate, code, title, description,
                    tuitionMapConsumer, insuranceMapConsumer, adminFeesMapConsumer, adminFeesPenaltyMapConsumer,
                    50, "15/12/2021", "31/05/2022", tuitionProductCode, tuitionProductDescription);
            createEventTemplate(eventTemplate, code, title, description,
                    tuitionMapConsumer, insuranceMapConsumer, adminFeesMapConsumer, adminFeesPenaltyMapConsumer,
                    70, "15/12/2021", "31/05/2022", tuitionProductCode, tuitionProductDescription);
        }

        return eventTemplate;
    }

    private void createEventTemplate(final EventTemplate parent, final String code,
                                     final LocalizedString title, final LocalizedString description,
                                     final Consumer<JsonObject> tuitionMapConsumer,
                                     final Consumer<JsonObject> insuranceMapConsumer,
                                     final Consumer<JsonObject> adminFeesMapConsumer,
                                     final Consumer<JsonObject> adminFeesPenaltyMapConsumer,
                                     final int percentage, final String date1, final String date2,
                                     final String tuitionProductCode, final String tuitionProductDescription) {

        final BigDecimal tuition = new BigDecimal(JsonUtils.toJson(tuitionMapConsumer).entrySet().stream()
                .map(Map.Entry::getValue)
                .mapToDouble(e -> Double.parseDouble(e.getAsString()))
                .sum());
        final double tuitionP = tuition.multiply(new BigDecimal("0." + (percentage / 2))).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
        final double tuitionF = tuition.multiply(new BigDecimal("0." + percentage)).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
        final double maxCredits = 60 * percentage / 100;
        final ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();

        final EventTemplate eventTemplate = new EventTemplate(code + percentage,
                title.append(" " + percentage + "%"),
                description.append(" " + percentage + "%").append(ls(
                        ".\nEste plano limita as inscrições no ano letivo a " + maxCredits + " ECTS.",
                        ".\nThis plan limits enrolments for the entire year to " + maxCredits + " ECTS.")));
        eventTemplate.createConfig(APPLY_FROM, APPLY_UNTIL, config(tuitionMap -> {
            tuitionMap.addProperty(date1, Double.toString(tuitionP));
            tuitionMap.addProperty(date2, Double.toString(tuitionP));
        }, insuranceMapConsumer, adminFeesMapConsumer, adminFeesPenaltyMapConsumer, null, null, maxCredits, null,
                tuitionProductCode, tuitionProductDescription));
        parent.addAlternativeEventTemplate(eventTemplate);

        final EventTemplate eventTemplateF1 = new EventTemplate(code + percentage + "S1",
                title.append(ls(" " + percentage + "% Semestre 1", " " + percentage + "% Semester 1")),
                description.append(ls(" " + percentage + "% Semestre 1", " " + percentage + "% Semester 1")).append(ls(
                        ".\nEste plano apenas permite inscrições no 1º semestre e limita as inscrições a um máximo de " + maxCredits + " ECTS.",
                        ".\nThis plan only allows enrolments during the 1st semester and limits enrolments to a maximum of " + maxCredits + " ECTS.")));
        eventTemplateF1.createConfig(APPLY_FROM, APPLY_UNTIL, config(tuitionMap -> {
            tuitionMap.addProperty(date1, Double.toString(tuitionF));
        }, insuranceMapConsumer, adminFeesMapConsumer, adminFeesPenaltyMapConsumer, null, null, maxCredits, executionYear.getFirstExecutionPeriod(),
                tuitionProductCode, tuitionProductDescription));
        parent.addAlternativeEventTemplate(eventTemplateF1);

        final EventTemplate eventTemplateF2 = new EventTemplate(code + percentage + "S2",
                title.append(ls(" " + percentage + "% Semestre 2", " " + percentage + "% Semester 2")),
                description.append(ls(" " + percentage + "% Semestre 2", " " + percentage + "% Semester 2")).append(ls(
                        ".\nEste plano apenas permite inscrições no 2º semestre e limita as inscrições a um máximo de " + maxCredits + " ECTS.",
                        ".\nThis plan only allows enrolments during the 2nd semester and limits enrolments to a maximum of " + maxCredits + " ECTS.")));
        eventTemplateF2.createConfig(APPLY_FROM, APPLY_UNTIL, config(tuitionMap -> {
            tuitionMap.addProperty(date2, Double.toString(tuitionF));
        }, insuranceMapConsumer, adminFeesMapConsumer, adminFeesPenaltyMapConsumer, null, null, maxCredits, executionYear.getLastExecutionPeriod(),
                tuitionProductCode, tuitionProductDescription));
        parent.addAlternativeEventTemplate(eventTemplateF2);
    }

    private LocalizedString ls(final String pt, final String en) {
        return new LocalizedString(PT, pt).with(EN, en);
    }

}
