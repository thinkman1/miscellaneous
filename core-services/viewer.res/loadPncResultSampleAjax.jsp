<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<script>
function loadResearchResults() {
	if ($.trim($("#emailContent").val()) == "") {
		$.colorbox({
			title: "Validation Error",
			html: "<p>You must provide the email content.</p>"
		});		
	} else {	
		$("#emailContentInputForm").attr("action", "javascript:alert('The result is loading, resubmit is not allowed');");
		$("#researchResultOutput").apjsFadingLoader({
			ajaxUrl : JS_CONTEXT_ROOT + "support/pncResearchResults",
			ajaxParams : $("#emailContent").serialize(),
			loadingHtml : $("#researchResultLoading").html(),
			complete : function() {	
				$("#emailContentInputForm").attr("action", "javascript:loadResearchResults();");
			    $(".sortable").tablesorter({
				    widthFixed: true,
			    	widgets:['zebra','highlight']
			    }).each(function(){
				    $(this).tablesorterPager({
					    container: $(this).prev(),
					    size: 5,
					    seperator: ' of ',
					    positionFixed: false,
					    pageCounter: 'text'
					});		
			    });
				$(".sortable").trigger("appendCache");
			}
			
		});
	}
	
}
</script>

<br>
<center>
	<b>PNC Research:</b>
</center>
<br>

<div style="border:1px solid #cccccc; background-color:#ffffff; padding:15px;">
	<form id="emailContentInputForm"
		action="javascript:loadResearchResults();" method="POST">
		<table class="formatted padded">
			<tr>
				<td>Please paste the email content here:</td>
			</tr>
			<tr>
				<td><textarea id="emailContent" name="emailContent" wrap="off"
						style="width: 100%; height: 250px; font-family: verdana; background-color: #ffffff;"></textarea>
				</td>
			</tr>
			<tr>
				<td colspan="4" align="center">
					<input type="submit"
					value="Submit" class="btn" id="submitButton" />&nbsp;&nbsp; &nbsp; 
					<input type="reset" value="Reset" class="btn" />
				</td>			
			</tr>


		</table>
	</form>
</div>
<br>

<div id="researchResultOutput" style="display: none;"></div>
<div id="researchResultLoading"  style="display: none;">
	<img src='<c:url value="/images/waitingImg.jpg" />' height="15" width="15" style="border-style: none" />
	Loading PNC Research Results...
</div>