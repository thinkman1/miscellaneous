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
	
							<td width="10%" class="HeaderTableTDText">
							<fmt:message key="debit.detail.credit.actNumber.hdr.name" />
							<span class="red">*</span>:&nbsp;
						</td>
						<td width="15%" >
							<form:input path="accountNumber" onkeypress="javascript:ValidAlphaNum(this,event);" onchange="javascript:PadAccountNumber(this);findAccountFiid(this)" maxlength="20"/>
						</td>

						<td width="10%" class="HeaderTableTDText">
							<fmt:message key="debit.detail.credit.actType.hdr.name" />
							<span class="red">*</span>:&nbsp;
						</td>
						<td width="15%">
							<form:select path="accountType" cssStyle="width:85%">
							<form:option value="Invalid">Select from list</form:option>
							<form:option value="<%=AccountType.DDA.getId() %>"><%=AccountType.DDA.getDesc() %></form:option>
							<form:option value="<%=AccountType.SAVINGS.getId() %>"><%=AccountType.SAVINGS.getDesc() %></form:option>
							<form:option value="<%=AccountType.PPA.getId() %>"><%=AccountType.PPA.getDesc() %></form:option>
							</form:select>
						</td>
						
												<td width="10%" class="HeaderTableTDText">
							<fmt:message key="debit.detail.file.cardFIID.hdr.name" />
							<span class="red">*</span>:&nbsp;
						</td>
						<td width="15%">
							<form:select path="cardFiid" cssStyle="width:85%">
							<form:option value="Invalid">Select from list</form:option>
							<c:forEach items="<%=ManualTransactionUtils.getOrderedBanks() %>" var="cardFiidBank">
								<form:option value="${cardFiidBank}"><fmt:formatNumber minIntegerDigits="3" value="${cardFiidBank}"/></form:option>
							</c:forEach>
							</form:select>
						</td>