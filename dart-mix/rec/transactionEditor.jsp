<script>
	$(function(){
		$(document).on("updateImagesEvent", function(){
			$.getJSON("/dart-assemble-web/assemble/getNewImages.htm",
					function (data){
						if(data != null) {	 
							updateImagePanel(data.list);
						} 
					}
				);	
			showRestartButton();
		});		
		

	});
	
	function moveImg(btn) {
		var checkImgName = $('input:radio[name=selectedImg]:checked').val();
		if(typeof checkImgName === 'undefined') {
			alert("Please select a desitination to move to.")
		} else {
			$(btn).prev().appendTo("td#" + checkImgName.replace(".", "\\."));
			
			$colSelected = $("td#" + checkImgName.replace(".", "\\."));
			$colSelected.append("<button type='button' onclick='removeImg(this)'>Remove</button>");
			$("input", $colSelected).val($(btn).attr("id"));
			
			$('input:radio[name=selectedImg]:checked').css('visibility', 'hidden');
			$('input:radio[name=selectedImg]').removeAttr('checked');
			$(btn).parent().parent().remove();
		}
	}
	
	function removeImg(btn) {
		$("#imageTable").append("<tr><td></td></tr>");
		$lastRow = $("#imageTable tr:last");
		$(btn).prev().appendTo($("td", $lastRow));
		var id = $("input", $(btn).parent()).val();
		$("td", $lastRow).append("<button id='" + id + "' onclick='moveImg(this)'>Move</button>");
		$(":radio", $(btn).parent().prev()).css('visibility', 'visible');
		$("input", $(btn).parent()).val("");
		$(btn).remove();
	}
	
	function updateImagePanel(list) {
		for(var count=0; count < list.length; count++) {
			$("#imageTable").append("<tr>"
					+"<td><img src='showImage.htm?imgName=" + list[count] + "' width='300' height='80'>"
					+"<button id='" + list[count] + "' onclick='moveImg(this)'>Move</button></td></tr>");
		}
	}
</script>

<style>
	#imagePanel {
		margin-left:15px;
		margin-top:15px;
		width: 100%;
		_height: expression( this.scrollHeight > 499 ? "500px" : "auto" );
		max-height: 500px;
		float: left;
		overflow: auto;
		overflow-x: hidden;
	}
	
	#imagePanel td {
		vertical-align:middle;
	}
	
	img {
		vertical-align:middle;
	}
</style>

<div id="imagePanel">
	<table id="imageTable">		
	
	</table>
</div>