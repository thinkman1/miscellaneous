package sample.use;

public class Test {
	
	public void test() {
		reader.init(xmlContent);
		StreamingXmlReader sreader = new StreamingXmlReader();
		StreamingXmlToObject<MapToClass> objectMapper = new StreamingXmlToObject<MapToClass>(MapToClass.class);
		sreader.vistXml(reader, objectMapper);
		List<MapToClass> obj = objectMapper.getObject();

		/**
		 * validate new EBK fields
		 */
		assertEquals("29:Employment Authorization ID with Photo", obj.get(0).customerAuthenticationType);
		assertEquals("1234", obj.get(0).atmClientId);
		assertEquals("281196 (33595) DAY 1 DEVELOPMENT", obj.get(0).branchCostCenter);
		assertEquals("0319914419", obj.get(0).conductorEci);
		assertEquals("0191380505", obj.get(0).beneficiaryEci);
		assertEquals("Negotiated", obj.get(0).negotiationStatus);
		assertEquals("20000", obj.get(0).riskNegotiatedAmount);
		assertEquals("20000", obj.get(0).riskNonNegotiatedAmount);
	}




public static class MapToClass {
@SetFromXpath(xpath = "/dartData/unitOfWork/transaction/transactionSourceInformation/customerAuthenticationType")
private String customerAuthenticationType;

@SetFromXpath(xpath = "/dartData/unitOfWork/transaction/transactionSourceInformation/atmClientId")
private String atmClientId;

@SetFromXpath(xpath = "/dartData/unitOfWork/transaction/transactionSourceInformation/branchCostCenter")
private String branchCostCenter;

@SetFromXpath(xpath = "/dartData/unitOfWork/transaction/conductorEci")
private String conductorEci;

@SetFromXpath(xpath = "/dartData/unitOfWork/transaction/beneficiaryEci")
private String beneficiaryEci;

@SetFromXpath(xpath = "/dartData/unitOfWork/transaction/debit/negotiation/negotiationStatus")
private String negotiationStatus;

@SetFromXpath(xpath = "/dartData/unitOfWork/transaction/debit/negotiation/riskNegotiatedAmount")
private String riskNegotiatedAmount;

@SetFromXpath(xpath = "/dartData/unitOfWork/transaction/debit/negotiation/riskNonNegotiatedAmount")
private String riskNonNegotiatedAmount;

@SetFromXpath(xpath = "/dartData/tracking/@type")
private String type;

@SetFromXpath(xpath = "/dartData/unitOfWork/@id")
private String fileId;

@SetFromXpath(xpath = "/dartData/unitOfWork/terminalInformation/@atmId")
private String atmId;

@SetFromXpath(xpath = "/dartData/unitOfWork/transaction/@id")
private String transactionId;

@SetFromXpath(xpath = "/dartData/unitOfWork/transaction/transactionSourceInformation/source")
private String transactionSource;

@SetFromXpath(xpath = "/dartData/unitOfWork/transaction/pan")
private String pan;

@SetFromXpath(xpath = "/dartData/unitOfWork/transaction/credit/tranCode")
private String tranCode;

@SetFromXpath(xpath = "/dartData/unitOfWork/transaction/debit/itemStatus")
private String itemStatus;
}
}
