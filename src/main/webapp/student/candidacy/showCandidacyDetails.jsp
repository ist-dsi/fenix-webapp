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
<%@ page import="pt.ist.fenixedu.integration.domain.BpiCard"%>
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
            	<div id="errorMessage" class="alert alert-danger" role="alert" style="display: none; font-size: 20px; margin-bottom: 30px;">Tem de responder a todas as questões.</div>
                <div id="banksBody">
                        <h2 style="border-bottom-width: 1px; border-bottom-color: #ddd; border-bottom-style: solid; margin-top: 40px;">
                            <bean:message key="authorize.personal.data.access.title.cgd" bundle="FENIXEDU_IST_INTEGRATION_RESOURCES"/>
                        </h2>

                        <p>
                            <bean:message key="authorize.personal.data.access.description.cgd" bundle="FENIXEDU_IST_INTEGRATION_RESOURCES"/>
                        </p>

                        <div class="row">
                            <div class="col-lg-12 text-left">       
                                <span style="line-height: 20px; vertical-align: bottom; margin-right: 55px;">
                                    <input type="radio" name="cgdRadio" id="cgd_yes" value="true">Sim
                                </span>
                                <span>
                                    <input type="radio" name="cgdRadio" id="cgd_no" value="false">Não
                                </span>
                            </div>
                        </div>

                        <h2 style="border-bottom-width: 1px; border-bottom-color: #ddd; border-bottom-style: solid; margin-top: 40px;">
                            <bean:message key="authorize.personal.data.access.title.bpi" bundle="FENIXEDU_IST_INTEGRATION_RESOURCES"/>
                        </h2>

                        <p style="margin-top: 40px;">
                            <bean:message key="authorize.personal.data.access.description.bpi" bundle="FENIXEDU_IST_INTEGRATION_RESOURCES"/>
                        </p>

                        <div class="row">
                            <div class="col-lg-12 text-left">
                                <span style="line-height: 20px; vertical-align: bottom; margin-right: 55px;">
                                    <input type="radio" name="bpiRadio" id="bpi_yes" value="true">Sim
                                </span>
                                <span>
                                    <input type="radio" name="bpiRadio" id="bpi_no" value="false">Não
                                </span>
                            </div>                          
                        </div>
                        
                        <p class="text-center" style="margin-top:  30px;">
                            <a href="#" id="submitButton" class="btn-primary btn btn-lg" onclick="submitForm()" >
                                Submeter
                            </a>                            
                        </p>
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
<% 
	org.fenixedu.bennu.spring.security.CSRFToken token = new org.fenixedu.bennu.spring.security.CSRFTokenRepository().getToken(request);
%>

<script type="text/javascript">
    function submitForm() {
    	var cgdRadio = document.querySelector('input[name="cgdRadio"]:checked');
        var bpiRadio = document.querySelector('input[name="bpiRadio"]:checked');

        if(cgdRadio != null && bpiRadio != null) {
        	document.getElementById("errorMessage").style.display = 'none';
	        postYes(document.querySelector('input[name="cgdRadio"]:checked').value,
	                document.querySelector('input[name="bpiRadio"]:checked').value);
	    } else {
	    	document.getElementById("errorMessage").style.display = 'block';
	    }
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

    function postYes(allowAccessCgd, allowAccessBpi) {
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

        var hiddenField3 = document.createElement("input");
        hiddenField3.setAttribute("type", "hidden");
        hiddenField3.setAttribute("name", "allowAccessBpi");
        hiddenField3.setAttribute("value", allowAccessBpi);
        form.appendChild(hiddenField3);
        
        var hiddenField4 = document.createElement("input");
        hiddenField4.setAttribute("type", "hidden");
        hiddenField4.setAttribute("name", "<%= token.getParameterName() %>");
        hiddenField4.setAttribute("value", "<%= token.getToken() %>");
        form.appendChild(hiddenField4);

        
        document.body.appendChild(form);
        form.submit();
        document.getElementById ( "banksBody" ).style.display = "none";
    }

    replaceTargetWith( 'visibleTitle', '<span id="visibleTitle">Cedência de Dados / Cartões</span>' );
    <%
        boolean bpiHasResponse = BpiCard.hasAccessResponse();
        
        if(bpiHasResponse){
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