<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<link href="<c:url value="/css/jquery/jquery-ui-1.8.12.custom.css" />"
	rel="stylesheet" type="text/css" />
<script src="<c:url value="/scripts/jquery/jquery-ui-1.8.12.custom.min.js" />"
	type="text/javascript"></script>

<script>
	$(document).ready(function() {
		$.ajaxSetup({ cache: false });
		$("input[name=project]").click(function() {
			getVersionsForProject($(this).val());
		});
	});

	function getVersionsForProject(project) {
		$("#versionsDisplay").fadeOut('fast', function() {
			$("#versionsDisplay").html($("#searchResultsLoading").html()).fadeIn('fast', function() {
				$.ajax({
					url: '<c:url value="/pw/viewVersions" />',
					method: 'post',
					data: {project:project},
					success: function(data) {
						$("#versionsDisplay").fadeOut('fast', function() {
							$('#versionsDisplay').html(data).fadeIn('fast');
						});
					}, cache: false
				});
			});
		});
	}
</script>

<h2>Projects</h2>
<table width="100%" border="0" cellpadding="0" cellspacing="0">
	<tbody>
		<c:forEach items="${projects}" var="project" varStatus="i">
			<c:if test="${i.count % 4 eq 1}">
				<c:if test="${i.count ne 0}">
					</tr>
				</c:if>
				<tr>
			</c:if>
			<td width="25%">
				<input type="radio" value="${project}" name="project" /> ${project}
			</td>
		</c:forEach>
	</tbody>
</table>
<br>

<div id="versionsDisplay"></div>
