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
<%@page import="org.fenixedu.academic.domain.candidacy.PersonalInformationBean"%>
<%@page import="org.joda.time.LocalDate"%>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html"%>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean"%>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic"%>
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/fenix-renderers" prefix="fr"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html:html xhtml="true">
<head>

	<link rel="stylesheet" type="text/css" media="screen" href="${pageContext.request.contextPath}/themes/ashes/css/general.css">
	<link href="${pageContext.request.contextPath}/themes/ashes/css/style.css" rel="stylesheet" type="text/css" />
	<script src="<%= request.getContextPath() %>/javaScript/jquery/jquery.js"></script>
	<style type="text/css">
		.container {
	        background-color: #fefefe;
	        padding: 30px;
	        border-radius: 10px;
        }
        .title {
            border-bottom: 1px solid #eee;
            padding-bottom: 5px;
            font-size: 25px;
            min-height: 35px;
            margin-bottom: 20px;
        }
        .logo {
        	margin-left: 20px;
        }
	</style>
</head>
<body>
	<div id="content">

<div class="container">
<logic:present name="LOGGED_USER_ATTRIBUTE" property="person.student">

<div class="title row">
	<div class="col-sm-6 text-right col-sm-push-6">
		<c:forEach var="locale" items="${portal.supportedLocales}">
			<a class="locale-changer ${locale == portal.locale ? 'active' : ''}" title="${locale.displayName}" onclick="setLang('${locale}')">${locale.language}</a>
		</c:forEach>
		<img class="logo" src="${pageContext.request.contextPath}/api/bennu-portal/configuration/logo"/>
	</div>
	<div class="col-sm-6 col-sm-pull-6">
		<bean:message  key="label.fill.information" bundle="STUDENT_RESOURCES"/>
	</div>
</div>

<div class="panel panel-default">
	<div class="panel-body">
		<bean:message  key="label.fill.missing.candidacy.information.message" arg0="<%=org.fenixedu.academic.domain.organizationalStructure.Unit.getInstitutionAcronym()%>" arg1="<%=org.fenixedu.academic.domain.organizationalStructure.Unit.getInstitutionName().getContent()%>" bundle="STUDENT_RESOURCES"/>
	</div>
</div>

<h3><bean:write name="personalInformationBean" property="description"/></h3>


<bean:define id="personalInformationBean" name="personalInformationBean" type="org.fenixedu.academic.domain.candidacy.PersonalInformationBean"/>
<div class="alert alert-warning">
<% if (!PersonalInformationBean.isPastLimitDate()) { %>
	<span><bean:message key="label.RAIDES.skip.enabled" bundle="STUDENT_RESOURCES"/> <%= personalInformationBean.getLimitDate().toString() %>.</span>
	<br/>
	<span><bean:message key="label.RAIDES.skip.enabled.more" bundle="STUDENT_RESOURCES"/></span>
<% } else { %>
	<span><bean:message key="label.RAIDES.skip.disabled" bundle="STUDENT_RESOURCES"/> <%= personalInformationBean.getLimitDate().toString() %>.</span>
<% } %>
</div>

<bean:define id="personalInformationBean" name="personalInformationBean" type="org.fenixedu.academic.domain.candidacy.PersonalInformationBean" />

<logic:messagesPresent message="true">
	<div class="alert alert-danger">
		<ul>
			<html:messages id="messages" message="true" bundle="APPLICATION_RESOURCES">
				<li><bean:write name="messages" /></li>
			</html:messages>
		</ul>
	</div>
</logic:messagesPresent>

<fr:form action="/editMissingCandidacyInformation.do?method=edit">

	<fr:edit id="personalInformationBean" name="personalInformationBean" visible="false" />

	<h3><bean:message key="label.previous.degree.information" bundle="STUDENT_RESOURCES"/></h3>
	
	<div><bean:message key="label.previous.degree.information.details" bundle="STUDENT_RESOURCES"/></div>
	
	<p class="alert alert-info">
		<bean:message key="label.RAIDES.check.institution.and.degree" bundle="STUDENT_RESOURCES"/><br/>
		<bean:message key="label.RAIDES.check.schoolLevel" bundle="STUDENT_RESOURCES"/>
	</p>
		
	<fr:edit id="personalInformationBean.editPrecedentDegreeInformation" name="personalInformationBean">
		<fr:schema bundle="APPLICATION_RESOURCES" type="org.fenixedu.academic.domain.candidacy.PersonalInformationBean">
			<fr:slot name="countryWhereFinishedPreviousCompleteDegree" layout="menu-select-postback" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator">
				<fr:property name="format" value="\${localizedName.content}"/>
				<fr:property name="sortBy" value="name=asc" />
				<fr:property name="providerClass" value="org.fenixedu.academic.ui.renderers.providers.DistinctCountriesProvider" />
				<fr:property name="destination" value="schoolLevelPostback" />
			</fr:slot>
			<fr:slot name="schoolLevel" layout="menu-select-postback" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator">
				<fr:property name="providerClass" value="org.fenixedu.academic.ui.renderers.providers.candidacy.SchoolLevelTypeForStudentProvider" />
				<fr:property name="destination" value="schoolLevelPostback" />
			</fr:slot>
			<% if (personalInformationBean.isSchoolLevelOther()) { %>
				<fr:slot name="otherSchoolLevel" />
			<% } %>
			<% if (personalInformationBean.isHightSchoolCountryFieldRequired()) { %>
				<fr:slot name="countryWhereFinishedHighSchoolLevel" layout="menu-select" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator">
					<fr:property name="format" value="\${localizedName.content}"/>
					<fr:property name="sortBy" value="name=asc" />
					<fr:property name="providerClass" value="org.fenixedu.academic.ui.renderers.providers.DistinctCountriesProvider" />
				</fr:slot>
			<% } %>
			<% if (personalInformationBean.isUnitFromRaidesListMandatory()) { %>
				<fr:slot name="institutionUnitName" layout="autoCompleteWithPostBack" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator">
					<fr:property name="size" value="50"/>
					<fr:property name="labelField" value="unit.name"/>
					<fr:property name="indicatorShown" value="true"/>
					<fr:property name="provider" value="org.fenixedu.academic.service.services.commons.searchers.SearchRaidesDegreeUnits"/>
					<fr:property name="args" value="slot=name,size=50"/>
					<fr:property name="minChars" value="4"/>
					<fr:property name="rawSlotName" value="institutionName"/>
					<fr:property name="destination" value="institutionPostBack"/>
				</fr:slot>
				<fr:slot name="raidesDegreeDesignation" layout="autoComplete" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator">
					<fr:property name="size" value="50"/>
					<fr:property name="labelField" value="description"/>
					<fr:property name="indicatorShown" value="true"/>
					<fr:property name="provider" value="org.fenixedu.academic.service.services.commons.searchers.SearchRaidesDegreeDesignations"/>
					<fr:property name="args" value="<%="slot=description,size=50,filterSchoolLevelName=" + ((personalInformationBean.getSchoolLevel() != null) ? personalInformationBean.getSchoolLevel().getName() : "null") + ",filterUnitOID=" + ((personalInformationBean.getInstitution() != null) ? personalInformationBean.getInstitution().getExternalId() : "null") %>"/>
					<fr:property name="minChars" value="3"/>
				</fr:slot>
			<% } else { %>
				<fr:slot name="institutionUnitName" layout="autoComplete" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator">
					<fr:property name="size" value="50"/>
					<fr:property name="labelField" value="unit.name"/>
					<fr:property name="indicatorShown" value="true"/>
					<fr:property name="provider" value="org.fenixedu.academic.service.services.commons.searchers.SearchExternalUnitsWithScore"/>
					<fr:property name="args" value="slot=name,size=20"/>
					<fr:property name="minChars" value="3"/>
					<fr:property name="rawSlotName" value="institutionName"/>
				</fr:slot>
				<fr:slot name="degreeDesignation" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator">
					<fr:property name="size" value="50"/>
					<fr:property name="maxLength" value="255"/>
				</fr:slot>
			<% } %>
			<fr:slot name="conclusionGrade" validator="org.fenixedu.academic.ui.struts.action.student.candidacy.EditMissingCandidacyInformationDA$ConclusionGradeRegexpValidator">
				<fr:property name="size" value="2"/>
				<fr:property name="maxLength" value="2"/>
			</fr:slot>
			<fr:slot name="conclusionYear" validator="org.fenixedu.academic.ui.struts.action.student.candidacy.EditMissingCandidacyInformationDA$ConclusionYearRegexpValidator">
				<fr:property name="size" value="4"/>
				<fr:property name="maxLength" value="4"/>
			</fr:slot>
		</fr:schema>
		<fr:layout name="tabular">
			<fr:property name="classes" value="tstyle5 thlight thleft mtop15" />
			<fr:property name="columnClasses" value="width300px,,tdclear tderror1"/>
			<fr:destination name="invalid" path="/editMissingCandidacyInformation.do?method=prepareEditInvalid" />
			<fr:destination name="postback" path="/editMissingCandidacyInformation.do?method=prepareEditPostback" />
			<fr:destination name="schoolLevelPostback" path="/editMissingCandidacyInformation.do?method=schoolLevelPostback" />
			<fr:destination name="institutionPostBack" path="/editMissingCandidacyInformation.do?method=prepareEditInstitutionPostback" />
		</fr:layout>
	</fr:edit>
	
	<br/>

	
	<h3><bean:message  key="label.personal.information" bundle="STUDENT_RESOURCES"/></h3>
	
	<div><bean:message  key="label.personal.information.details" bundle="STUDENT_RESOURCES"/></div>
	
	<fr:edit id="personalInformationBean.editPersonalInformation" name="personalInformationBean">
		<fr:schema bundle="APPLICATION_RESOURCES" type="org.fenixedu.academic.domain.candidacy.PersonalInformationBean">
			<fr:slot name="countryOfResidence" layout="menu-select-postback" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator">
				<fr:property name="format" value="\${localizedName.content}"/>
				<fr:property name="sortBy" value="name=asc" />
				<fr:property name="providerClass" value="org.fenixedu.academic.ui.renderers.providers.DistinctCountriesProvider" />
				<fr:property name="destination" value="countryOfResidencePostback" />
			</fr:slot>
			<% if (personalInformationBean.isCountryOfResidenceDefaultCountry()) { %>
				<fr:slot name="districtSubdivisionOfResidence" layout="autoComplete">
					<fr:property name="size" value="50"/>
					<fr:property name="format" value="\${name} - (\${district.name})"/>
					<fr:property name="indicatorShown" value="true"/>
					<fr:property name="provider" value="org.fenixedu.academic.service.services.commons.searchers.SearchDistrictSubdivisions"/>
					<fr:property name="args" value="slot=name,size=50"/>
					<fr:property name="minChars" value="1"/>
				</fr:slot>
			<% } %>
			<fr:slot name="dislocatedFromPermanentResidence" layout="radio" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator">
				<fr:property name="classes" value="dinline liinline nobullet"/>
			</fr:slot>
			<fr:slot name="schoolTimeDistrictSubdivisionOfResidence" layout="autoComplete">
				<fr:property name="size" value="50"/>
				<fr:property name="format" value="\${name} - (\${district.name})"/>
				<fr:property name="indicatorShown" value="true"/>
				<fr:property name="provider" value="org.fenixedu.academic.service.services.commons.searchers.SearchDistrictSubdivisions"/>
				<fr:property name="args" value="slot=name,size=50"/>
				<fr:property name="minChars" value="1"/>
			</fr:slot>
			<fr:slot name="grantOwnerType" layout="menu-select-postback" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator">
				<fr:property name="providerClass" value="org.fenixedu.academic.ui.renderers.providers.candidacy.GrantOwnerTypesProvider" />
				<fr:property name="destination" value="grantOwnerTypePostback" />
			</fr:slot>
			<% if (personalInformationBean.isGrantProviderAnotherInstitution()) { %>
				<fr:slot name="grantOwnerProviderUnitName" layout="autoComplete">
					<fr:property name="size" value="50"/>
					<fr:property name="labelField" value="unit.name"/>
					<fr:property name="indicatorShown" value="true"/>
					<fr:property name="provider" value="org.fenixedu.academic.service.services.commons.searchers.SearchExternalUnits"/>
					<fr:property name="args" value="slot=name,size=50"/>
					<fr:property name="minChars" value="1"/>
					<fr:property name="rawSlotName" value="grantOwnerProviderName"/>
				</fr:slot>
			<% } %>
			<% if ((personalInformationBean.getSchoolLevel() != null) && (personalInformationBean.getSchoolLevel().isHighSchoolOrEquivalent())) { %>
				<fr:slot name="highSchoolType" layout="menu-select" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator">
					<fr:property name="providerClass" value="org.fenixedu.academic.ui.renderers.providers.candidacy.HighSchoolTypesProvider" />
					<fr:property name="eachLayout" value="this-does-not-exist" />
				</fr:slot>
			<% } %>
			<fr:slot name="maritalStatus" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator">
				<fr:property name="excludedValues" value="UNKNOWN" />
			</fr:slot>
			<fr:slot name="professionalCondition" layout="menu-select" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator">
				<fr:property name="providerClass" value="org.fenixedu.academic.ui.renderers.providers.ProfessionalSituationConditionTypeProviderForRaides"/>
			</fr:slot>
			<fr:slot name="professionType" layout="menu-select" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator">
				<fr:property name="providerClass" value="org.fenixedu.academic.ui.renderers.providers.ActiveProfessionTypeProvider" />
			</fr:slot>
			<fr:slot name="motherSchoolLevel" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator" layout="menu-select">
				<fr:property name="providerClass" value="org.fenixedu.academic.ui.renderers.providers.candidacy.SchoolLevelTypeForStudentHouseholdProvider" />
				<fr:property name="eachLayout" value="this-does-not-exist" />
			</fr:slot>
			<fr:slot name="motherProfessionalCondition" layout="menu-select" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator">
				<fr:property name="providerClass" value="org.fenixedu.academic.ui.renderers.providers.ProfessionalSituationConditionTypeProviderForRaides"/>
			</fr:slot>
			<fr:slot name="motherProfessionType" layout="menu-select" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator">
				<fr:property name="providerClass" value="org.fenixedu.academic.ui.renderers.providers.ActiveProfessionTypeProvider" />
			</fr:slot>
			<fr:slot name="fatherSchoolLevel" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator" layout="menu-select">
				<fr:property name="providerClass" value="org.fenixedu.academic.ui.renderers.providers.candidacy.SchoolLevelTypeForStudentHouseholdProvider" />
				<fr:property name="eachLayout" value="this-does-not-exist" />
			</fr:slot>
			<fr:slot name="fatherProfessionalCondition" layout="menu-select" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator">
				<fr:property name="providerClass" value="org.fenixedu.academic.ui.renderers.providers.ProfessionalSituationConditionTypeProviderForRaides"/>
			</fr:slot>
			<fr:slot name="fatherProfessionType" layout="menu-select" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator">
				<fr:property name="providerClass" value="org.fenixedu.academic.ui.renderers.providers.ActiveProfessionTypeProvider" />
			</fr:slot>
		</fr:schema>
		<fr:layout name="tabular">
			<fr:property name="classes" value="tstyle5 thlight thleft mtop15" />
			<fr:property name="columnClasses" value="width300px,,tdclear tderror1"/>
			<fr:destination name="invalid" path="/editMissingCandidacyInformation.do?method=prepareEditInvalid" />
			<fr:destination name="countryOfResidencePostback" path="/editMissingCandidacyInformation.do?method=countryOfResidencePostback" />
			<fr:destination name="grantOwnerTypePostback" path="/editMissingCandidacyInformation.do?method=grantOwnerTypePostback" />
		</fr:layout>
	</fr:edit>

	<div class="form-group">
		<div class="col-sm-9 col-sm-offset-3">
			<html:submit bundle="HTMLALT_RESOURCES" altKey="submit.submit"><bean:message bundle="APPLICATION_RESOURCES" key="label.submit"/></html:submit>
		</div>
	</div>
	
</fr:form>

<br/>

<bean:define id="personalInformationBean" name="personalInformationBean" type="org.fenixedu.academic.domain.candidacy.PersonalInformationBean"/>
<div id="skip">
<% if (!PersonalInformationBean.isPastLimitDate()) { %>
	<form action="<%= request.getContextPath() + "/home.do" %>">
		<html:submit bundle="HTMLALT_RESOURCES" altKey="submit.submit"><bean:message bundle="APPLICATION_RESOURCES" key="button.inquiries.respond.later"/></html:submit>
	</form>
	<br/>
<% } %>
</div>

</logic:present>
</div>


</div>

<script>
$(".col-sm-2").removeClass("col-sm-2").addClass("col-sm-3");
$(".col-sm-10").removeClass("col-sm-10").addClass("col-sm-9");
setLang = function(l) {
	$.post('${pageContext.request.contextPath}/api/bennu-core/profile/locale/' + l.replace('_', '-')).always(function() { location.reload(); });
}
</script>

</body>
</html:html>
