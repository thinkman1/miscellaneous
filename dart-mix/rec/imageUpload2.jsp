<script>
	function reload() {
	  document.getElementById('imageUploadFrame').contentWindow.location.reload(true);
	  $("#restartButton").hide();
	}
	
	function showRestartButton() {
		$("#restartButton").show();
	}
</script>

<div id="imageUploadView">
	<button id="restartButton" onclick="reload();" style="margin-left:15px;margin-top:15px;display:none;">Upload More</button>
	<IFRAME frameborder="0"  
	            style="width: 100%; height: 400px; border: margin: 0px;"  
	            id="imageUploadFrame" name="imageUploadFrame" scrolling="no"  
	            src="imageUploadTool.htm">   
	</IFRAME>  
</div>