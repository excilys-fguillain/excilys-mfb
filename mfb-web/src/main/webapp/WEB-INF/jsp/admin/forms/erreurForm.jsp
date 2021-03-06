<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<header id="overview" class="span10 offset2">
	<h1>
		<spring:message code="home.title" />
	</h1>
	<p class="lead">
		<spring:message code="home.erreurForm" />
	</p>

</header>

<section id="erreurForm">
	<div class="row">
		<%@ include file="../common/formSelection.jsp"%>
		<div class="span4 offset4 alignCenter">
			<div class="alert alert-error">
				<spring:message code="${message}" />
			</div>
		</div>
	</div>
</section>