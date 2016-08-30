<%--

    Copyright © 2002 Instituto Superior Técnico

    This file is part of FenixEdu Academic.

    FenixEdu Academic is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FenixEdu Academic is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FenixEdu Academic.  If not, see <http://www.gnu.org/licenses/>.

--%>
<%@page import="pt.ist.fenixedu.integration.domain.cgd.CgdCard"%>
<%@ page language="java"%>
<% final String contextPath = request.getContextPath(); %>
<%@ page import="org.fenixedu.academic.domain.candidacy.CandidacyOperationType"%>
<%@ page import="org.fenixedu.academic.servlet.ProcessCandidacyPrintAllDocumentsFilter"%>
<%@ page import="pt.ist.fenixedu.integration.domain.BpiCard"%>
<%@ page import="pt.ist.fenixedu.integration.domain.SantanderCard"%>
<%@ page import="org.joda.time.LocalDate"%>
<%@ page import="org.joda.time.Years"%>
<%@ page import="org.fenixedu.academic.domain.candidacy.Candidacy"%>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html"%>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean"%>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic"%>
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/fenix-renderers" prefix="fr"%>
<%@ taglib uri="http://jakarta.apache.org/taglibs/struts-example-1.0" prefix="app" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<html:xhtml />

<html:html xhtml="true">
    <head>
        <title>
            <bean:message  key="label.candidacy.candidacyDetails" bundle="CANDIDATE_RESOURCES"/>
        </title>

        <link href="${pageContext.request.contextPath}/themes/<%= org.fenixedu.bennu.portal.domain.PortalConfiguration.getInstance().getTheme() %>/css/style.css" rel="stylesheet" type="text/css" />

        <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
            .container {
                background-color: #fefefe;
                padding: 30px;
                border-radius: 10px;
                margin-top: 50px;
                max-width: 800px;
            }
            .title {
                border-bottom: 1px solid #eee;
                padding-bottom: 5px;
                font-size: 25px;
                min-height: 35px;
            }
            dd {
                margin-bottom: 5px;
            }
            #banksBody p {
                font-size: 20px;
            }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="title row">
                <div class="col-sm-3 text-right col-sm-push-9">
                    <img src="${pageContext.request.contextPath}/api/bennu-portal/configuration/logo"/>
                </div>
                <div class="col-sm-9 col-sm-pull-3">
                    <span id="visibleTitle"></span>
                </div>
            </div>
            <br/><br/>
            <div id="txt">
                <div id="banksBody">                    
                    <p>
                        Autorizo a cedência à Caixa Geral de Depósitos dos meus dados pessoais 
                        (nome, morada, contactos, números de identificação, fotografia e dados de matricula)  
                        para a emissão do meu cartão de identificação da Universidade de Lisboa, cartão Caixa IU,
                        <b>necessário para acesso à cantina do IST</b>, 
                        bem como a alguns outros serviços assegurados pela Universidade.
                    </p>
                    <% if (!ProcessCandidacyPrintAllDocumentsFilter.isPdfFillerToExclude("org.fenixedu.idcards.ui.candidacydocfiller.BPIPdfFiller") &&
                            !ProcessCandidacyPrintAllDocumentsFilter.isPdfFillerToExclude("org.fenixedu.idcards.ui.candidacydocfiller.SantanderPdfFiller")) { %>
                        <p class="text-center">
                            <a href="#" class="btn-primary btn btn-lg" onclick="postYes(true);">
                                Sim
                            </a>
                            <a href="#" class="btn-default btn btn-lg" onclick="goByeBye();">
                                Não
                            </a>
                        </p>
                    <% } else { %>

                        <div class="row">
                            <div class="col-lg-12 text-left">       
                                <span style="line-height: 20px; vertical-align: bottom; margin-right: 55px;">
                                    <input type="radio" name="cgdRadio" id="cgd_yes" value="true" onclick="removeDisabled()">Sim
                                </span>
                                <span>
                                    <input type="radio" name="cgdRadio" id="cgd_no" value="false" onclick="removeDisabled()">Não
                                </span>
                            </div>
                        </div>


                        <% if(ProcessCandidacyPrintAllDocumentsFilter.isPdfFillerToExclude("org.fenixedu.idcards.ui.candidacydocfiller.BPIPdfFiller")) { %>
                            <p style="margin-top: 40px;">
                                Autorizo a cedência ao Banco Português de Investimento (BPI) dos meus dados pessoais 
                                (nome, morada, contactos, números de identificação, fotografia e dados de matricula)  
                                para a emissão do meu cartão de identificação da Associação de Estudantes do IST.
                            </p>

                            <div class="row">
                                <div class="col-lg-12 text-left">
                                    <span style="line-height: 20px; vertical-align: bottom; margin-right: 55px;">
                                        <input type="radio" name="bpiRadio" id="bpi_yes" value="true" onclick="removeDisabled()">Sim
                                    </span>
                                    <span>
                                        <input type="radio" name="bpiRadio" id="bpi_no" value="false" onclick="removeDisabled()">Não
                                    </span>
                                </div>                          
                            </div>
                        <% } %>

                        <% if(ProcessCandidacyPrintAllDocumentsFilter.isPdfFillerToExclude("org.fenixedu.idcards.ui.candidacydocfiller.SantanderPdfFiller")) { %>
                            <p style="margin-top: 40px;">
                                Autorizo a cedência ao Banco Santander dos meus dados pessoais 
                                (nome, morada, contactos, números de identificação, fotografia e dados de matricula)  
                                para a emissão do meu cartão bancário de identificação perante o Técnico.
                            </p>

                            <div class="row">
                                <div class="col-lg-12 text-left">
                                    <span style="line-height: 20px; vertical-align: bottom; margin-right: 55px;">
                                        <input type="radio" name="santanderRadio" id="santander_yes" value="true" onclick="removeDisabled()">Sim
                                    </span>
                                    <span>
                                        <input type="radio" name="santanderRadio" id="santander_no" value="false" onclick="removeDisabled()">Não
                                    </span>
                                </div>                          
                            </div>
                        <% } %>
                        
                        <p class="text-center" style="margin-top:  30px;">
                            <a href="#" id="submitButton" class="btn-primary btn btn-lg disabled" onclick="submitForm()" >
                                Submeter
                            </a>                            
                        </p>
                    <% } %>
                </div>
                <div id ="byeByeBody" style="visibility: hidden;">
                    <logic:messagesPresent message="true">
                        <ul class="nobullet list6">
                            <html:messages id="messages" message="true" bundle="CANDIDATE_RESOURCES">
                                <li><span class="error0"><bean:write name="messages" /></span></li>
                            </html:messages>
                        </ul>
                    </logic:messagesPresent>

                    <dl class="dl-horizontal">
                        <dt>${fr:message('resources.CandidateResources', 'label.org.fenixedu.academic.domain.Person.name')}</dt>
                        <dd><c:out value="${candidacy.person.name}"/></dd>
                        <dt>${fr:message('resources.CandidateResources', 'label.studentNumber')}</dt>
                        <dd><c:out value="${candidacy.person.student.number}"/></dd>
                        <dt>${fr:message('resources.CandidateResources', 'label.username')}</dt>
                        <dd><c:out value="${candidacy.person.username}"/></dd>
                        <dt>${fr:message('resources.CandidateResources', 'org.fenixedu.academic.domain.candidacy.StudentCandidacy.executionDegree.degreeCurricularPlan.degree.name')}</dt>
                        <dd><c:out value="${candidacy.executionDegree.degreeName}"/></dd>
                        <dt>${fr:message('resources.CandidateResources', 'org.fenixedu.academic.domain.candidacy.Candidacy.activeCandidacySituation.candidacySituationType')}</dt>
                        <dd>${fr:message('resources.EnumerationResources', candidacy.activeCandidacySituation.candidacySituationType.qualifiedName)}</dd>
                    </dl>

                    <br />

                    <logic:equal name="candidacy" property="activeCandidacySituation.candidacySituationType" value="REGISTERED">
                        <h3>${fr:message('resources.CandidateResources', 'label.candidacy.congratulations')}!</h3>
                        <p class="lead">
                            ${fr:message('resources.CandidateResources', 'label.candidacy.process.concluded')}
                        </p>
                    </logic:equal>
                </div>
            </div>
        </div>
    </body>
</html:html>

<script type="text/javascript">
    function removeDisabled() {
        var cgdRadio = document.querySelector('input[name="cgdRadio"]:checked');
        var bpiRadio = document.querySelector('input[name="bpiRadio"]:checked');
        var santanderRadio = document.querySelector('input[name="santanderRadio"]:checked');

        var bpiAnswered = true;
        <% if(ProcessCandidacyPrintAllDocumentsFilter.isPdfFillerToExclude("org.fenixedu.idcards.ui.candidacydocfiller.BPIPdfFiller")){ %>
            bpiAnswered = bpiRadio != null && bpiRadio.value != undefined;
        <% } %>
        var santanderAnswered = true;
        <% if(ProcessCandidacyPrintAllDocumentsFilter.isPdfFillerToExclude("org.fenixedu.idcards.ui.candidacydocfiller.SantanderPdfFiller")){ %>
            santanderAnswered = santanderRadio != null && santanderRadio.value != undefined;
        <% } %>

        if (cgdRadio != null && cgdRadio.value != undefined && bpiAnswered && santanderAnswered) {
            document.getElementById("submitButton").className = "btn-primary btn btn-lg";
        }
    }

    function submitForm() {
        postYes(document.querySelector('input[name="cgdRadio"]:checked').value,
        document.querySelector('input[name="bpiRadio"]:checked').value,
        document.querySelector('input[name="santanderRadio"]:checked').value);
    }

    function replaceTargetWith( targetID, html ) {
          var i, tmp, elm, last, target = document.getElementById(targetID);
          tmp = document.createElement(html.indexOf('<td')!=-1?'tr':'div');
          tmp.innerHTML = html;
          i = tmp.childNodes.length;
          last = target;
          while(i--){
            target.parentNode.insertBefore((elm = tmp.childNodes[i]), last);
            last = elm;
          }
          target.parentNode.removeChild(target);
    }

    function goByeBye() {
        document.getElementById ( "banksBody" ).style.display = "none";
        document.getElementById ( "byeByeBody" ).style.visibility = "visible";
        replaceTargetWith( 'visibleTitle', '<span id="visibleTitle">Processo Concluído</span>' );
    }

    function postYes(allowAccessCgd, allowAccessBpi, allowAccessSantander) {
        var form = document.createElement("form");
        form.setAttribute("method", "post");
        form.setAttribute("action", '<%= contextPath %>' + '/authorize-personal-data-access' );

        var hiddenField = document.createElement("input");
        hiddenField.setAttribute("type", "hidden");
        hiddenField.setAttribute("name", "allowAccessCgd");
        hiddenField.setAttribute("value", allowAccessCgd);
        form.appendChild(hiddenField);

        var hiddenField2 = document.createElement("input");
        hiddenField2.setAttribute("type", "hidden");
        hiddenField2.setAttribute("name", "qs");
        hiddenField2.setAttribute("value", window.location);
        form.appendChild(hiddenField2);


        if(allowAccessBpi != undefined) {
            var hiddenField3 = document.createElement("input");
            hiddenField3.setAttribute("type", "hidden");
            hiddenField3.setAttribute("name", "allowAccessBpi");
            hiddenField3.setAttribute("value", allowAccessBpi);
            form.appendChild(hiddenField3);
        }

        if(allowAccessSantander != undefined) {
            var hiddenField4 = document.createElement("input");
            hiddenField4.setAttribute("type", "hidden");
            hiddenField4.setAttribute("name", "allowAccessSantander");
            hiddenField4.setAttribute("value", allowAccessSantander);
            form.appendChild(hiddenField4);
        }
        
        document.body.appendChild(form);
        form.submit();
        document.getElementById ( "banksBody" ).style.display = "none";
    }

    replaceTargetWith( 'visibleTitle', '<span id="visibleTitle">Cedência de Dados / Cartões</span>' );
    <%
        boolean bpiHasResponse = true;
        boolean santanderHasResponse = true;
        if(ProcessCandidacyPrintAllDocumentsFilter.isPdfFillerToExclude("org.fenixedu.idcards.ui.candidacydocfiller.BPIPdfFiller")){
            bpiHasResponse = BpiCard.hasAccessResponse();
        }
        if(ProcessCandidacyPrintAllDocumentsFilter.isPdfFillerToExclude("org.fenixedu.idcards.ui.candidacydocfiller.SantanderPdfFiller")){
            santanderHasResponse = SantanderCard.hasAccessResponse();
        }
        System.out.println("bpi " + bpiHasResponse);
        System.out.println("santander " + santanderHasResponse);
        if(bpiHasResponse && santanderHasResponse && CgdCard.hasCGDAccessResponse()){ 
    %>
            goByeBye();
    <%
        }
        Candidacy candidacy = (Candidacy)request.getAttribute("candidacy");
        if(Years.yearsBetween(candidacy.getPerson().getDateOfBirthYearMonthDay().toLocalDate(), new LocalDate()).getYears() < 18) {
    %>
            goByeBye();
    <%
        }
    %>
</script>