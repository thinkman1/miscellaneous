		<script type="text/javascript" src="../scripts/jquery/jquery-1.8.2.js"></script>
		<script type="text/javascript" src="../scripts/jquery/tablesorter/jquery.tablesorter.mod.js"></script>
		<script type="text/javascript" src="../scripts/jquery/tablesorter/jquery.tablesorter.pager.mod.js"></script>
		<script type="text/javascript" src="../scripts/jquery/apjs/jquery.apjsAjaxUploadForm.js"></script>
		<c:url value="/css/jpmc.css" var="jpmccss" />
		<link rel="stylesheet" type="text/css" href="${jpmccss}">
		<script>
			<% String expirationTime = (String) ContextFactory.getObject("expirationTime"); %>
			var TIMEOUT_MINUTES = <%=expirationTime%>;
			var APP_CONTEXT_PATH = '<%=request.getContextPath()%>';
		
			var itemCount = 0;
			var currentItemType = 'check';
			$(function() {
				$(".sortable").tablesorter({
					widthFixed: true,
			    	widgets:['zebra','highlight']
				});

			});

			function deleteCheckItem(itemNo) {
			  	$("#checkItem" + itemNo +"TitleTab").remove();
				$("#checkItem" + itemNo +"SubTab").remove();
			 	for(var i = parseInt(itemNo) + 1; i <= itemCount; i++) {
					$("div#checkItem" + i + "TitleTab").attr("id", "checkItem" + (i - 1) + "TitleTab");
					$("div#checkItem" + i).attr("id", "checkItem" + (i - 1));
					$("b#checkItem" + i + "Title").html("Check Item " + (i - 1));
					$("b#checkItem" + i + "Title").attr("id", "checkItem" + (i - 1) + "Title");
					$("input#deleteCheckItemButton" + i).attr("id", "deleteCheckItemButton" + (i - 1));

 					var checkContent = $("div#checkItem" + i + "SubTab").html();
					checkContent = checkContent.replace(new RegExp("atmCheckItems" + (i - 1), 'g'), "atmCheckItems" + (i - 2));

					$("div#checkItem" + i + "SubTab").html(checkContent);
					var fields = $("input[name^='atmCheckItems[" + (i - 1) + "]']", $("div#checkItem" + i + "SubTab"));
					for(var j = 0; j < fields.length; j++) {
						var $oneField = $(fields[j]);
						var newName = $oneField.attr("name").replace(i - 1, i - 2);
						$oneField.attr("name", newName);
					}
					$("div#checkItem" + i + "SubTab").attr("id", "checkItem" + (i - 1) + "SubTab");
					$("input[name^='atmCheckItems[" + (i - 2) + "].id']", $("div#checkItem" + (i - 1) + "SubTab")).val(i - 1);

				}
			 	itemCount = itemCount - 1;
				return false;
			}

			function deleteCashItem(itemNo) {
			  	$("#cashItem" + itemNo +"TitleTab").remove();
				$("#cashItem" + itemNo +"SubTab").remove();
			 	for(var i = parseInt(itemNo) + 1; i <= itemCount; i++) {
			 		//update title tab
					$("div#cashItem" + i + "TitleTab").attr("id", "cashItem" + (i - 1) + "TitleTab");
					$("div#cashItem" + i).attr("id", "cashItem" + (i - 1));
					$("b#cashItem" + i + "Title").html("Cash Item " + (i - 1));
					$("b#cashItem" + i + "Title").attr("id", "cashItem" + (i - 1) + "Title");
					$("input#deleteCashItemButton" + i).attr("id", "deleteCashItemButton" + (i - 1));

					var fields = $("input[name^='atmCashItems[" + (i - 1) + "]']", $("div#cashItem" + i + "SubTab"));
					for(var j = 0; j < fields.length; j++) {
						var $oneField = $(fields[j]);
						var newName = $oneField.attr("name").replace(i - 1, i - 2);
						$oneField.attr("name", newName);
						$oneField.attr("id", "atmCashItems" + (i - 2));
					}
					$("div#cashItem" + i + "SubTab").attr("id", "cashItem" + (i - 1) + "SubTab");

				}
			 	itemCount = itemCount - 1;
				return false;
			}


			 function addCheckItem() {
					itemCount = itemCount + 1;
					var titleContent = '<div id="checkItem' + itemCount + '" class="collapse leftFloat">'
									 + 		'<img width="10" height="10" name="buttonImg" src="/dart-assemble-web/images/plus.gif">'
									 + 		'<b id="checkItem' + itemCount + 'Title"> Check Item ' + itemCount + '</b>'
									 + '</div>'
									 + '<div class="editButtons">'
									 +		'<input id="deleteCheckItemButton' + itemCount + '" class="deleteCheckItemButton" type="button" value="Delete" style="font-size: 8pt;">'
									 + '</div>'
									 + '<br>';

					var checkContent = '<table class="formatted dataTable sortable">'
									+ 	'<thead>'
									+ 		'<tr> <th width="30%">XML tag</th> <th width="70%">Values</th> </tr>'
									+ 	'</thead>'
									+ 	'<tbody>'
									+ 		'<tr><td>Amount<span class="red">*</span></td><td><input id="atmCheckItems' + (itemCount - 1) + '.ocrAmount" type="text" onchange="isValidAmount(this)" maxlength="11" value="" name="atmCheckItems[' + (itemCount - 1) + '].ocrAmount"></td></tr>'
									+ 		'<tr height="90"><td><input type="radio" name="selectedImg" value="atmCheckItems' + (itemCount - 1) + '.frontImg">Image Front<span class="red">*</span></td>'
									+			'<td id="atmCheckItems' + (itemCount - 1) + '.frontImg">'
									+				'<input id="atmCheckItems' + (itemCount - 1) + '.viewerImageFront" name="atmCheckItems[' + (itemCount - 1) + '].viewerImageFront" style="display:none;" type="text" value="">'
									+			'</td></tr>'
									+ 		'<tr height="90"><td><input type="radio" name="selectedImg" value="atmCheckItems' + (itemCount - 1) + '.backImg">Image Back<span class="red">*</span></td>'
									+			'<td id="atmCheckItems' + (itemCount - 1) + '.backImg">'
									+				'<input id="atmCheckItems' + (itemCount - 1) + '.viewerImageBack" name="atmCheckItems[' + (itemCount - 1) + '].viewerImageBack" style="display:none;" type="text" value="">'
									+			'</td></tr>'
									+	'</tbody>'
									+'</table>'
									+'<br>';

					$("#itemLevelCheckValuesContent").append("<div id='checkItem" + itemCount + "TitleTab' class='checkItemTitleTab'>" + titleContent + "</div>");
					$("#itemLevelCheckValuesContent").append("<div id='checkItem" + itemCount + "SubTab' style='display: none; padding-left: 20px;'>"
																+ checkContent + "</div>");

					$("div#checkItem" + itemCount).click(function(){
						var subTab = $("#" + $(this).attr("id").replace(/\s/g, '\\ ') + "SubTab");
						var buttonImg = $("img[name=buttonImg]", $(this)).attr("src");
						if(subTab.css('display') == 'none') {
							subTab.css('display', 'block');
							buttonImg = buttonImg.replace("plus", "minus");
							$(".sortable").tablesorter({
								widthFixed: true,
						    	widgets:['zebra','highlight']
							});
						} else {
							subTab.css('display', 'none');
							buttonImg = buttonImg.replace("minus", "plus");
						}
						$("img[name=buttonImg]", $(this)).attr("src", buttonImg);
					});

					$("#deleteCheckItemButton" + itemCount).click(function(){
						var id = $(this).attr("id").replace("deleteCheckItemButton", "");
						deleteCheckItem(id);
					});
					return false;
				}
				
				
				