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
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html"%>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean"%>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic"%>
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/fenix-renderers" prefix="fr"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@page import="org.fenixedu.academic.domain.student.Registration"%>
<%@page import="pt.ist.fenixedu.quc.domain.StudentInquiryRegistry"%>
<%@page import="org.fenixedu.academic.domain.ExecutionSemester"%><html:xhtml/>

<%
    Registration registration = (Registration) request.getAttribute("registration");

    request.setAttribute("hasInquiriesToRespond", StudentInquiryRegistry.hasInquiriesToRespond(registration.getStudent()));
%>

<logic:present name="executionSemester" property="enrolmentInstructions">
    <bean:write name="executionSemester" property="enrolmentInstructions.tempInstructions.content" filter="false"/>
</logic:present>

<bean:define id="registrationOid" name="registration" property="externalId" />

<c:if test="${hasInquiriesToRespond}">
    <div class="alert alert-warning">
        ${fr:message('resources.FenixEduQucResources', 'message.student.cannotEnroll.inquiriesNotAnswered')}
    </div>
</c:if>

<c:if test="${not empty debtsMessage}">
    <div class="alert alert-warning">
        ${fr:message('resources.ApplicationResources', debtsMessage)}
    </div>
</c:if>

<c:if test="${empty debtsMessage && not hasInquiriesToRespond}">
    <a class="btn btn-primary" href="${pageContext.request.contextPath}/student/bolonhaStudentEnrollment.do?method=prepare&registrationOid=${registrationOid}&executionSemesterID=${executionSemester.externalId}">
        ${fr:message('resources.ApplicationResources', 'label.continue')}
    </a>
</c:if>
