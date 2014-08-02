<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/tld/security.tld" prefix="security" %>
<script>
	$(document).ready(function() {
		$.ajaxSetup({ cache: false });
		$("input[name=version]").click(function() {
			buildButton($(this).val());
		});
	});

	function buildButton(version) {
		$("#buildButton").fadeOut('fast', function() {
			$("#buildButton").html($("#searchResultsLoading").html()).fadeIn('fast', function() {
				$.ajax({
					url: '<c:url value="/pw/importProcess" />',
					method: 'post',
					data: {version:version},
					success: function(data) {
						$("#buildButton").fadeOut('fast', function() {
							$('#buildButton').html(data).fadeIn('fast');
						});
					}, cache: false
				});
			});
		});
	}
</script>

<h2>Versions</h2>
<table width="100%" border="0" cellpadding="0" cellspacing="0">
	<tbody>
		<c:forEach items="${versions}" var="version" varStatus="i">
			<c:if test="${i.count % 4 eq 1}">
				<c:if test="${i.count ne 0}">
					</tr>
				</c:if>
				<tr>
			</c:if>
			<td width="25%">
				<input type="radio" value="${version}" name="version" /> ${version}
			</td>
		</c:forEach>
	</tbody>
</table>
<div id="buildButton"></div>
