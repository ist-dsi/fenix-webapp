<%--

    Copyright © 2013 Instituto Superior Técnico

    This file is part of FenixEdu IST Integration.

    FenixEdu IST Integration is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FenixEdu IST Integration is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FenixEdu IST Integration.  If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ page language="java" %>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html"%>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean"%>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic"%>
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/fenix-renderers" prefix="fr" %>
<html:xhtml/>
<bean:define id="link" type="java.lang.String">
	<%= request.getContextPath() + "/gep/reportsByDegreeType.do?method=selectDegreeType"%>
</bean:define>

<h2><bean:message key="label.gep.latest.requests.done" bundle="GEP_RESOURCES" /></h2>

<logic:present name="executionYear">
<bean:define id="year" name="executionYear" property="year"/>
<bean:define id="executionYearID" name="executionYear" property="externalId"/>
<bean:define id="link" type="java.lang.String">
	<bean:write name="link" />&executionYearID=<%= executionYearID %>
</bean:define>
</logic:present>
<logic:present name="degreeType">
<bean:define id="degreeTypeName" name="degreeType" property="name.content"/>
<bean:define id="degreeTypeID" name="degreeType" property="externalId"/>
<bean:define id="link" type="java.lang.String">
	<bean:write name="link" />&degreeType=<%= degreeTypeID %>
</bean:define>
</logic:present>


<bean:define id="list" name="list"/>

<ul>
	<li><a href="<%=  link %>"><bean:message bundle="GEP_RESOURCES" key="label.gep.back"/></a></li>
</ul>

<logic:notEmpty name="queueJobList">

<h3 class="mtop15 mbottom05"><bean:message key="label.gep.listing.type" bundle="GEP_RESOURCES" arg0="<%= list.toString() %>"/></h3>
<logic:present name="executionYear">
	<b><bean:message key="title.gep.executionYear" bundle="GEP_RESOURCES"/></b>: <bean:write name="year" /><br/>
</logic:present>

<logic:present name="degreeType">
	<b><bean:message key="label.gep.degree.type" bundle="GEP_RESOURCES"/></b>: <bean:write name="degreeTypeName" /><br/>
</logic:present>

	<fr:view name="queueJobList">
		<fr:schema type="org.fenixedu.academic.domain.QueueJob" bundle="GEP_RESOURCES">
			<fr:slot name="upperCaseType" key="label.gep.format"/>
			<fr:slot name="requestDate" key="label.gep.date"/>
			<fr:slot name="person.name" key="label.get.person"/>
		</fr:schema>
    	<fr:layout name="tabular">
    		<fr:property name="classes" value="tstyle1 mtop05" />
    		<fr:property name="columnClasses" value=",,,acenter,,,,,," />
			<fr:property name="link(Download)" value="/downloadQueuedJob.do?method=downloadFile"/>
			<fr:property name="param(Download)" value="externalId/id"/>
			<fr:property name="bundle(Download)" value="GEP_RESOURCES"/>
			<fr:property name="visibleIf(Download)" value="done"/>
			<fr:property name="module(Download)" value=""/>
			
			<fr:property name="link(sendJob)" value="<%= "/gep/reportsByDegreeType.do?method=resendJobFromViewReports&executionYearID=" + request.getParameter("executionYearID") + "&degreeType=" + request.getParameter("degreeType") + "&type=" + request.getParameter("type") %>"/>
			<fr:property name="param(sendJob)" value="externalId/id"/>
			<fr:property name="key(sendJob)" value="label.sendJob"/>
			<fr:property name="bundle(sendJob)" value="GEP_RESOURCES"/>
			<fr:property name="visibleIf(sendJob)" value="isNotDoneAndCancelled"/>
			<fr:property name="module(sendJob)" value=""/>
			
			<fr:property name="link(Cancel)" value="<%= "/gep/reportsByDegreeType.do?method=cancelQueuedJobFromViewReports&executionYearID=" + request.getParameter("executionYearID") + "&degreeType=" + request.getParameter("degreeType") + "&type=" + request.getParameter("type") %>"/>
			<fr:property name="param(Cancel)" value="externalId/id"/>
			<fr:property name="key(Cancel)" value="label.cancel"/>
			<fr:property name="bundle(Cancel)" value="GEP_RESOURCES"/>
			<fr:property name="visibleIf(Cancel)" value="isNotDoneAndNotCancelled"/>
			<fr:property name="module(Cancel)" value=""/>
		</fr:layout>
	</fr:view>

</logic:notEmpty>


<logic:empty name="queueJobList">
	<p class="mtop15 mbottom05"><em><bean:message key="label.gep.listing.type.non.existing" bundle="GEP_RESOURCES" /></em></p>
</logic:empty>
