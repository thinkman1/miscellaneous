<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib tagdir="/WEB-INF/tags/tablesorter" prefix="tablesorter"%>
<%@ page import="com.jpmc.vpc.core.ContextFactory"%>

<html>
	<head>
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

			 function getCashCurrencyOptions() {
				var	options = '<option value="Invalid">Select from list</option>'
							+ '<option value="100">1</option>'
							+	'<option value="200">2</option>'
							+	'<option value="500">5</option>'
							+	'<option value="1000">10</option>'
							+	'<option value="2000">20</option>'
							+	'<option value="5000">50</option>'
							+	'<option value="10000">100</option>';

				return options;
			 }

			 function checkCurrency(curr, name) {
				 $('#itemLevelCashValuesContent').find('select').each(function () {

					 if (name != this.name) {
						 if (curr.value == this.value) {
							 alert("Cash currency should not be duplicated!")
						 }
					 }
				});
			 }
			 
			 			 function addCashItem() {
				itemCount = itemCount + 1;

				var cashCurrencyOptions = getCashCurrencyOptions();

				var titleContent = '<div id="cashItem' + itemCount + '" class="collapse leftFloat">'
								 + 		'<img width="10" height="10" name="buttonImg" src="/dart-assemble-web/images/plus.gif">'
								 + 		'<b id="cashItem' + itemCount + 'Title"> Cash Item ' + itemCount + '</b>'
								 + '</div>'
								 + '<div class="editButtons">'
								 +		'<input id="deleteCashItemButton' + itemCount + '" class="deleteCashItemButton" type="button" value="Delete" style="font-size: 8pt;">'
								 + '</div>'
								 + '<br>';

				var cashContent = '<table class="formatted dataTable sortable">'
								+ 	'<thead>'
								+ 		'<tr> <th width="30%">XML tag</th> <th width="70%">Values</th> </tr>'
								+ 	'</thead>'
								+ 	'<tbody>'
								+		'<tr><td>Value of Currency<span class="red">*</span></td>'
								+			'<td><select id="atmCashItems' + (itemCount - 1) + '.value" type="text" onchange="checkCurrency(this, this.name);getDepositAmount()" name="atmCashItems[' + (itemCount - 1) + '].value">'
								+				cashCurrencyOptions
								+		'</select></td></tr>'
								+ 		'<tr><td>Note Count<span class="red">*</span></td><td><input id="atmCashItems' + (itemCount - 1) + '.count" type="text" onchange="isNum(this);getDepositAmount()" value="" name="atmCashItems[' + (itemCount - 1) + '].count"></td></tr>'
								+	'</tbody>'
								+'</table>'
								+'<br>';

				$("#itemLevelCashValuesContent").append("<div id='cashItem" + itemCount + "TitleTab' class='cashItemTitleTab'>" + titleContent + "</div>");
				$("#itemLevelCashValuesContent").append("<div id='cashItem" + itemCount + "SubTab' style='display: none; padding-left: 20px;'>"
															+ cashContent + "</div>");

				$("div#cashItem" + itemCount).click(function(){

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

				$("#deleteCashItemButton" + itemCount).click(function(){
					var id = $(this).attr("id").replace("deleteCashItemButton", "");
					deleteCashItem(id);
				});
				return false;
			}

			function addItem() {
				var itemType =  $('input:radio[name=itemType]:checked').val();
				if(itemType == 'cash') {
					addCashItem();
				} else {
					addCheckItem();
				}
			}

			function changeItemType() {
				var itemType =  $('input:radio[name=itemType]:checked').val();
				if(itemType != currentItemType) {
					$("#depositAmount").html("$0.00");
					currentItemType = itemType;
					itemCount = 0;
					if(itemType == 'cash') {
						$("#source").html("CashDeposit");
						$("#itemLevelCheckValuesContent").html("").css("display", "none");
						$("#itemLevelCashValuesContent").css("display", "block");
						$("#imagePanel").css("visibility", "hidden");
						$("#imageUploadView").css("visibility", "hidden");
					} else {
						$("#source").html("CheckDeposit");
						$("#itemLevelCashValuesContent").html("").css("display", "none");
						$("#itemLevelCheckValuesContent").css("display", "block");
						$("#imagePanel").css("visibility", "visible");
						$("#imageUploadView").css("visibility", "visible");
					}
				}
			}

			function checkMandatoryField() {
				var empty = [];
				if ($.trim($("#cardPostDate").val()) == 'Invalid') {
					empty.push("Please select a 'Process Date'\n");
				}
				if ($.trim($("#atmTerminalId").val()) == '') {
					empty.push("Please fill in 'Atm Id' field\n");
				}
				if ($.trim($("#accountNumber").val()) == '') {
					empty.push("Please fill in 'Account Number' field\n");
				}
				if ($.trim($("#accountType").val()) == 'Invalid') {
					empty.push("Please select a valid 'Account Type' type\n");
				}
				if ($.trim($("#cardFiid").val()) == 'Invalid') {
					empty.push("Please select a valid 'Card FIID' type\n");
				}
				if ($.trim($("#termFiid").val()) == 'Invalid') {
					empty.push("Please select a valid 'Term FIID' type\n");
				}
				if ($.trim($("#sequenceNumber").val()) == '') {
					empty.push("Please fill in 'Sequence #' field\n");
				}
				if ($.trim($("#acctFiid").val()) == 'Invalid') {
					empty.push("Please select a valid 'Account FIID' type\n");
				}
				if ($.trim($("#termAddress").val()) == '') {
					empty.push("Please fill in 'Street Address' field\n");
				}
				if ($.trim($("#termCity").val()) == '') {
					empty.push("Please fill in 'City' field\n");
				}
				if ($.trim($("#termState").val()) == '') {
					empty.push("Please fill in 'State' field\n");
				}

				$("input[id^=atmCheckItems]").each(function(){
					if($.trim($(this).attr('id')).match("ocrAmount$") && $.trim($(this).val()) == '' ) {
						var check=$.trim($(this).attr('id')).split('.')[0];
						var num=parseInt(check.substr(check.length-1)) + 1;
						empty.push("Please fill in a valid check amount of check item " + num + " \n");
					}

					if($.trim($(this).attr('id')).match("viewerImageFront$") && $.trim($(this).val()) == '' ) {
						var check=$.trim($(this).attr('id')).split('.')[0];
						var num=parseInt(check.substr(check.length-1)) + 1;
						empty.push("Please add front image of check item " + num + " \n");
					}

					if($.trim($(this).attr('id')).match("viewerImageBack$") && $.trim($(this).val()) == '' ) {
						var check=$.trim($(this).attr('id')).split('.')[0];
						var num=parseInt(check.substr(check.length-1)) + 1;
						empty.push("Please add back image of check item " + num + " \n");
					}
				});

				$("select[id^=atmCashItems]").each(function(){
					if($.trim($(this).attr('id')).match("value$") && $.trim($(this).val()) == 'Invalid' ) {
						var cash=$.trim($(this).attr('id')).split('.')[0];
						var num=parseInt(cash.substr(cash.length-1)) + 1;
						empty.push("Please select a valid cash currency of cash item " + num + " \n");
					}
				});

				$("input[id^=atmCashItems]").each(function(){
					if($.trim($(this).attr('id')).match("count$") && $.trim($(this).val()) == '' ) {
						var cash=$.trim($(this).attr('id')).split('.')[0];
						var num=parseInt(cash.substr(cash.length-1)) + 1;
						empty.push("Please fill in a valid cash note count of cash item " + num + " \n");
					}
				});

				return empty;
			}
			
						function submitTransaction() {

				if(!isContainItem()) {
					alert("Must contain at least one item.");
					return false;
				}

				if(validateInput()) {
					var isAmountValid = true;
					$("input[id^=atmCheckItems][id$=ocrAmount]").each(function(){
						if(!isValidAmount(this)) {
							isAmountValid = false;
							return false;
						}
					});

					if(isAmountValid) {
						//var isPanValid = isValidPAN(document.getElementById('pan'));
						//if(isPanValid) {
							var params = $("#atmTransactionInputForm").serialize() + "&" + $("#atmItemInputForm").serialize();
							$.post("/dart-assemble-web/assemble/submitTransaction.htm?SK_JPMC_XSRF=${SK_JPMC_XSRF}", //posting url
									params, //posting parameters
									function(data){//successful call back function
										if(data == "success") {
											$("#atmItemInputForm").submit();
										} else {
											alert("Cannot process submission, please investigate and try again later (error msg: " + data + ")");
										}
									}
							);
						//}
					}
				} else {
					//alert("Please fill in all the empty slots before submitting.");
					var empty = checkMandatoryField();
					alert(empty);
				}
			}

			function isContainItem() {
				if(($.trim($("#itemLevelCheckValuesContent").html()) == "")
						&& ($.trim($("#itemLevelCashValuesContent").html()) == "")) {
					return false;
				} else {
					return true;
				}
			}

			function validateInput() {
				var isValid = true;
				$("input", $("#atmTransactionInputForm")).each(function(){
					// ulid and pan are optional
					if($(this).attr("id") != 'ulid' && $(this).attr("id") != 'pan') {
						if($.trim($(this).val()) == '') {
							isValid = false;
						}
					}
				});

				$("select", $("#atmTransactionInputForm")).each(function(){
					if($.trim($(this).val()) == 'Invalid') {
						isValid = false;
					}
				});

				$("input[id^=atmCheckItems]").each(function(){
					if($.trim($(this).val()) == '') {
						isValid = false;
					}
				});


				$("input[id^=atmCashItems]").each(function(){
					if($.trim($(this).val()) == '') {
						isValid = false;
					}
				});

				$("select[id^=atmCashItems]").each(function(){
					if($.trim($(this).val()) == 'Invalid') {
						isValid = false;
					}
				});

				return isValid;
			}


			/*
				Function Name		:	isValidAmount
				Description			:	Check for valid amounts entered in amount fields.
				Returns	     		:	True if valid amount is entered else false.
			*/
			function isValidAmount(input){
				var txtValue = input.value;
				var len = txtValue.length;
				var regExAmount = /([^0-9.])/;
	
				if(regExAmount.test(txtValue)== true){
				  	alert("Please enter a valid amount.");
				  	input.focus();
				  	return false;
				}
	
				//Dot cannot be the first character
				var dotIndex = input.value.indexOf(".");
				if(txtValue.indexOf(".")== 0){
					alert("Please fill in a valid amount.");
					input.focus();
					return false;
				}
	
				//To restrict more than one dot
				if(txtValue.indexOf(".",dotIndex+1) != -1){
			       	alert("Please fill in a valid amount.");
					input.value = "";
					input.focus();
					return false;
		        }
	
				//Amount data validation e.g not allowing to enter with 3 precision eg: 123.453
				if(dotIndex == -1 ){
					if(txtValue.length> 8){
						alert("Integer part of the amount must not exceed 8 digits");
						input.focus();
						input.select();
						return false;
					}
				}else{
					var amtArr = txtValue.split(".");
					if( amtArr[1].length > 2 ){
				 		alert("Amount is allowed with 2 precisions only.");
				 		input.focus();
				 		input.select();
				 		return false;
				  	} else if(amtArr[0].length > 8) {
						alert("Integer part of the amount must not exceed 8 digits");
						input.focus();
						input.select();
						return false;
					}
				}
	
		        //if reach here, then the amount must be valid, now we need to change it to a good format
				input.value = formatAmount(txtValue);
				getDepositAmount();
				return true;
			}
	
			function formatAmount(amount) {
				var dotIndex = amount.indexOf(".");
				var len = amount.length;
				var formattedAmount;
				if(dotIndex == -1) {
					formattedAmount = amount + ".00";
				} else if(dotIndex == len - 1) {
					formattedAmount = amount + "00";
				} else if(dotIndex == len - 2) {
					formattedAmount = amount + "0";
				} else {
					formattedAmount = amount;
				}
	
				return formattedAmount;
			}
	
			function getDepositAmount() {
				var params = $("#atmTransactionInputForm").serialize() + "&" + $("#atmItemInputForm").serialize();
				$.post("/dart-assemble-web/assemble/getDepositAmount.htm?SK_JPMC_XSRF=${SK_JPMC_XSRF}", //posting url
						params, //posting parameters
						function(data){//successful call back function
							$("#depositAmount").html(data);
						}
				);
			}
		</script>
		
				<script type="text/javascript" src="../scripts/sessionExpired.js"></script>
		<style>

			.sortable {
				position: relative;
			}

			.sortable td {
				vertical-align:middle;
			}

		</style>
	</head>
	<body>

		<form:form method="post" action="abortManualTransaction.htm?SK_JPMC_XSRF=${SK_JPMC_XSRF}" commandName="atmTransaction" id="atmItemInputForm">
			<div style="padding-bottom:20px;">
				<br>
				<span>
					Item Type
					<input type="radio" name="itemType" value="check" onclick="changeItemType()" checked>Check
					<input type="radio" name="itemType" value="cash" onclick="changeItemType()">Cash &nbsp;&nbsp; &nbsp;
					<input type="button" id="addItemButton" style="font-size: 8pt;" value="Add Item" onclick="addItem()"/>
				</span>
			</div>

			<div id="itemLevelCheckValuesContent" style="padding-bottom:20px;">
			</div>

			<div id="itemLevelCashValuesContent" style="display:none; padding-bottom:20px;">
			</div>

			<span>
				<input type="button" id="submitButton" style="font-size: 8pt;" value="Submit" onclick="submitTransaction()"/>&nbsp;&nbsp; &nbsp;
				<!-- No default??? <input type="submit" id="abortButton" style="font-size: 8pt;" value="Abort"/> -->
				<input type="button" id="abortButton" style="font-size: 8pt;" value="Abort" onclick="document.getElementById('atmItemInputForm').submit();"/>
			</span>

		</form:form>

	</body>
</html>
