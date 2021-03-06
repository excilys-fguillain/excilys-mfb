<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib prefix="sec"
	uri="http://www.springframework.org/security/tags"%>

<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<div class="navbar navbar-fixed-top">
	<div class="navbar-inner">
		<div class="container">
			<a class="brand" href="${contextPath}/home.html"> <spring:message
					code="header.title" />
			</a>
			<div class="nav-collapse collapse">
				<ul class="nav">
					<li class="divider-vertical"></li>
					<sec:authorize access="isAuthenticated()">
						<li><a href="${contextPath}/home.html"><spring:message
									code="header.link.home" /></a></li>
					</sec:authorize>
					<sec:authorize access="hasRole('ROLE_CLIENT')">
						<li><a href="${contextPath}/client/virementInterne.html"><spring:message
									code="header.link.virementInterne" /></a></li>
						<li><a href="${contextPath}/client/virementExterne.html"><spring:message
									code="header.link.virementExterne" /></a></li>
						<%--<li class="dropdown">
							<a href="#" data-toggle="dropdown" class="dropdown-toggle"> 
								<spring:message code="header.button.virement" /> <b class="caret"></b>
							</a>
							<ul class="dropdown-menu">
								<li><a href="${contextPath}/client/virementInterne.html"><spring:message
											code="header.link.virementInterne" /></a></li>
								<li><a href="${contextPath}/client/virementExterne.html"><spring:message
											code="header.link.virementExterne" /></a></li>
							</ul>
						</li> --%>
					</sec:authorize>
					<sec:authorize
						access="hasRole('ROLE_ADMIN') and hasRole('ROLE_CLIENT')">
						<li><a href="${contextPath}/admin/home.html"><spring:message
									code="header.link.admin" /></a></li>
					</sec:authorize>

				</ul>

				<sec:authorize access="isAnonymous()">
					<form action="<c:url value='/j_spring_security_check'/>"
						class="navbar-form form-inline pull-right" method="post">
						<input name="j_username" type="text" id="form-top"
							class="input-small form-top"
							placeholder="<spring:message code="header.placeholder.username" />">
						<input name="j_password" type="password" id="form-top"
							class="input-small form-top"
							placeholder="<spring:message code="header.placeholder.password" />">
						<button type="submit" class="btn">
							<i class="icon-home"></i>
							<spring:message code="header.button.login" />
						</button>
						<a href="?lang=en"><img
							src="${contextPath}/content/images/drapeaux/en.png" height="30"
							width="30" /></a> <a href="?lang=fr"><img
							src="${contextPath}/content/images/drapeaux/fr.png" height="30"
							width="30" /></a>
					</form>
				</sec:authorize>

				<sec:authorize access="isAuthenticated()">
					<form action="<c:url value='/j_spring_security_logout'/>"
						class="navbar-form form-inline pull-right" method="post">
						${userFirstName} ${userLastName}
						<button class="btn">
							<i class="icon-off"></i>
							<spring:message code="header.button.logout" />
						</button>
						<a href="?lang=en"><img
							src="${contextPath}/content/images/drapeaux/en.png" height="30"
							width="30" /></a> <a href="?lang=fr"><img
							src="${contextPath}/content/images/drapeaux/fr.png" height="30"
							width="30" /></a>
					</form>
				</sec:authorize>


			</div>
		</div>
	</div>
</div>
