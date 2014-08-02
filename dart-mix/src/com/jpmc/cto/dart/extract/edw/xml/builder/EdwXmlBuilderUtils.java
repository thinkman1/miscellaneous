package com.jpmc.cto.dart.extract.edw.xml.builder;

import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.beans.factory.InitializingBean;

import com.jpmc.cto.dart.attr.AtmAttribute;
import com.jpmc.cto.dart.model.ObjectAttribute;
import com.jpmc.dart.commons.util.DartConstants;
import com.jpmc.dart.dao.jdbc.ClaimsDao;
import com.jpmc.dart.dao.jdbc.DartRemediationDao;
import com.jpmc.dart.dao.jdbc.EDWDao;
import com.jpmc.dart.dao.jdbc.PostingHistoryDao;
import com.jpmc.dart.dao.jdbc.RemediationTypeDao;
import com.jpmc.vpc.model.dart.check.DartCredit;
import com.jpmc.vpc.model.dart.check.DartDebit;
import com.jpmc.vpc.model.dart.check.DartFile;
import com.jpmc.vpc.model.dart.check.DartTransaction;
import com.jpmc.vpc.model.dart.database.Claim;
import com.jpmc.vpc.model.dart.database.PostingHistory;
import com.jpmc.vpc.model.dart.type.DebitType;
import com.jpmc.vpc.model.remediation.Remediation;
import com.jpmc.vpc.model.remediation.RemediationType;

/**
 * @author R502440
 * 
 */
public class EdwXmlBuilderUtils implements InitializingBean {

	private static PostingHistoryDao postingHistoryDao;

	private static ClaimsDao claimsDao;

	private static DartRemediationDao remediationDao;

	private static RemediationTypeDao remediationTypeDao;

	private static EDWDao edwDao;
	
	private static final Map<Integer, com.jpmc.vpc.model.remediation.RemediationType> REMEDIATION_TYPE_LOOKUP = Collections
			.synchronizedMap(new HashMap<Integer, com.jpmc.vpc.model.remediation.RemediationType>());

	/**
	 * This keeps a copy of the DataTypeFactory to a single thread since it is
	 * not thread safe.
	 */
	final private static ThreadLocal<DatatypeFactory> datatypeFactoryHolder = new ThreadLocal<DatatypeFactory>() {
		@Override
		protected DatatypeFactory initialValue() {
			try {
				return DatatypeFactory.newInstance();
			} catch (DatatypeConfigurationException e) {
				throw new IllegalStateException("Failed to create " + DatatypeFactory.class.getSimpleName(), e);
			}
		}
	};

	@Override
	public void afterPropertiesSet() throws Exception {
		Validate.notNull(postingHistoryDao, "postingHistryDao cannot be null");
		Validate.notNull(claimsDao, "claimsDao cannot be null");
		Validate.notNull(remediationDao, "remediationDao cannot be null");
		Validate.notNull(remediationTypeDao, "remediationTypeDao cannot be null");
		Validate.notNull(edwDao, "edwDao cannot be null");

		// Let's load out remediation types.
		loadRemediationTypeLookup();
	}

	/**
	 * Load our table. It's built to be reloadable but not doing that right now.
	 */
	private static void loadRemediationTypeLookup() {
		if (REMEDIATION_TYPE_LOOKUP.size() != 0) {
			REMEDIATION_TYPE_LOOKUP.clear();
		}

		List<com.jpmc.vpc.model.remediation.RemediationType> allTypes = remediationTypeDao.findAll();
		for (RemediationType remediationType : allTypes) {
			Integer key = Integer.valueOf(remediationType.getId());
			REMEDIATION_TYPE_LOOKUP.put(key, remediationType);
		}
	}

	/**
	 * Get the remediation type based upon the ID we have. It will load the
	 * table if it is empty.
	 * 
	 * @param id
	 * @return
	 */
	public static com.jpmc.vpc.model.remediation.RemediationType lookupRemediationType(Integer id) {
		if (REMEDIATION_TYPE_LOOKUP.size() == 0) {
			loadRemediationTypeLookup();
		}

		return REMEDIATION_TYPE_LOOKUP.get(id);
	}

	/**
	 * 
	 * @param date
	 * @return
	 * @throws DatatypeConfigurationException
	 */
	public static XMLGregorianCalendar convertDateToXmlGregCal(Date date) throws DatatypeConfigurationException {

		if (date == null) {
			return null;
		}

		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(date);

		XMLGregorianCalendar result = datatypeFactoryHolder.get().newXMLGregorianCalendar(gc);

		return result;
	}

	/**
	 * Convert a string into a XML Gregorian Calendar. If we can't convert then
	 * a NULL is returned.
	 * 
	 * @param date
	 * @return
	 */
	public static XMLGregorianCalendar stringToXMLGregorianCalendar(String date) {
		try {
			return datatypeFactoryHolder.get().newXMLGregorianCalendar(date);
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * 
	 * @param attr
	 * @param attrs
	 * @return
	 */
	public static String getAttibuteVal(AtmAttribute attr, List<ObjectAttribute> attrs) {

		if (CollectionUtils.isNotEmpty(attrs)) {
			for (ObjectAttribute oa : attrs) {
				if (StringUtils.equals(oa.getItem().getName(), attr.getName()))
					return oa.getValue();
			}
		}

		return StringUtils.EMPTY;
	}

	/**
	 * 
	 * @param obj
	 * @return
	 */
	public static String getStringVal(Object obj) {
		if (obj == null) {
			return StringUtils.EMPTY;
		}

		return obj.toString();
	}

	/**
	 * 
	 * @param obj
	 * @return
	 */
	public static BigInteger getBigIntegerVal(Object obj) {

		if (obj instanceof String) {
			if (StringUtils.isNotBlank((String) obj)) {
				return NumberUtils.createBigInteger((String) obj);
			}
		}

		if (obj instanceof Long) {
			return BigInteger.valueOf(((Long) obj).longValue());
		}

		if (obj instanceof Integer) {
			return BigInteger.valueOf(((Integer) obj).intValue());
		}

		if (obj instanceof Number) {
			return BigInteger.valueOf(((Number) obj).longValue());
		}

		return NumberUtils.createBigInteger("1");
	}
	
	/**
	 * Do it for ints
	 * 
	 * @param integerValue
	 * @return
	 */
	public static BigInteger getBigIntegerVal(int integerValue) {

		return BigInteger.valueOf(integerValue);
	}

	/**
	 * Do it for longs - this seems wasteful but folks don't always pay
	 * attention if there is an Object or Primitive.
	 * 
	 * @param longValue
	 * @return
	 */
	public static BigInteger getBigIntegerVal(long longValue) {

		return BigInteger.valueOf(longValue);
	}

	/**
	 * 
	 * @param transactionId
	 * @param procDate
	 * @return
	 */
	public static PostingHistory getPostingHistoryObj(UUID transactionId) {
		PostingHistory result = postingHistoryDao.findByRefId(transactionId);

		return result;
	}

	/**
	 * 
	 * @param creditId
	 * @param procDate
	 * @return
	 */
	public static Claim getClaimFromCredit(UUID creditId, Date procDate) {
		Claim result = claimsDao.findClaimByCredit(creditId, procDate);

		return result;
	}

	public static List<Remediation> getRemediations(UUID debitId, Date procDate) {
		return remediationDao.getRemediationByObjectIdAndProcDate(debitId, procDate);
	}

	/**
	 * @param postingHistoryDao
	 *            the postingHistoryDao to set
	 */
	public static void setPostingHistoryDao(PostingHistoryDao postingHistoryDao) {
		EdwXmlBuilderUtils.postingHistoryDao = postingHistoryDao;
	}

	/**
	 * @param claimsDao
	 *            the claimsDao to set
	 */
	public static void setClaimsDao(ClaimsDao claimsDao) {
		EdwXmlBuilderUtils.claimsDao = claimsDao;
	}

	/**
	 * @param remediationDao
	 *            the remediationDao to set
	 */
	public static void setRemediationDao(DartRemediationDao remediationDao) {
		EdwXmlBuilderUtils.remediationDao = remediationDao;
	}

	/**
	 * @return the remediationTypeDao
	 */
	public static RemediationTypeDao getRemediationTypeDao() {
		return remediationTypeDao;
	}

	/**
	 * @param remediationTypeDao
	 *            the remediationTypeDao to set
	 */
	public static void setRemediationTypeDao(RemediationTypeDao remediationTypeDao) {
		EdwXmlBuilderUtils.remediationTypeDao = remediationTypeDao;
	}

	/**
	 * @return the edwDao
	 */
	public static EDWDao getEdwDao() {
		return edwDao;
	}

	/**
	 * @param edwDao
	 *            the edwDao to set
	 */
	public static void setEdwDao(EDWDao edwDao) {
		EdwXmlBuilderUtils.edwDao = edwDao;
	}

	/**
	 * This probably belongs somewhere else. But I need it for EDW for now.
	 * Should move it someday. Can I describe how much I hate that
	 * List<DartCredit> doesn't inherit from List<MicrData>????
	 * 
	 * @param credits
	 * @return
	 */
	public static long sumCreditAmount(List<DartCredit> credits) {
		long tempAmount = 0;

		if (CollectionUtils.isNotEmpty(credits)) {
			for (DartCredit credit : credits) {
				tempAmount += credit.getAmount();
			}
		}

		return tempAmount;
	}

	/**
	 * This probably belongs somewhere else. But I need it for EDW for now.
	 * Should move it someday. Can I describe how much I hate that
	 * List<DartCredit> doesn't inherit from List<MicrData>????
	 * <p>
	 * We have a rule for getting debit amount refer to <code>DartDebit</code>
	 * 
	 * @param debits
	 * @return
	 */
	public static long sumDebitAmount(List<DartDebit> debits) {
		long tempAmount = 0;

		if (CollectionUtils.isNotEmpty(debits)) {
			for (DartDebit debit : debits) {

				tempAmount += debit.getPostingAmount();
			}
		}

		return tempAmount;
	}

	/**
	 * This probably belongs somewhere else. But I need it for EDW for now.
	 * Should move it someday. Today in DART we we create a debit for each type
	 * of cash denomination. Phooey - that's just not a good idea.
	 * 
	 * This method will return a count of real debits - a real debit is either a
	 * check and/or '1' for a 'cash' debit.
	 * 
	 * @param debits
	 * @return
	 */
	public static int countDebitsForReporting(List<DartDebit> debits) {
		boolean foundCash = false;
		int tempCount = 0;

		if (CollectionUtils.isNotEmpty(debits)) {
			for (DartDebit debit : debits) {
				if (DebitType.CASH.equals(debit.getItemType())) {
					foundCash = true;
				} else {
					tempCount++;
				}
			}
		}

		return foundCash ? tempCount + 1 : tempCount;
	}

	/**
	 * I'm not crazy about this but we need it - and the DAO's aren't used
	 * elsewhere. Get the IP Address. Pretty neat we keep this in an 'EDW'
	 * table. Sigh.
	 */
	public static String getAtmIpAddress(String atmId) {

		return edwDao.findIPAddressByAtmId(atmId);
	}

	/**
	 * @param termPostDate
	 *            - The TERM_POST_DATE Object attribute
	 * @return
	 */
	public static Date getTermPostDate(ObjectAttribute termPostDate) {
		Date result = null;

		if (termPostDate == null) {
			return null;
		}

		String termPostDateStr = termPostDate.getValue();
		DateFormat df = new SimpleDateFormat(DartConstants.Credit.TERM_POST_DATE_XML_FORMAT);
		try {
			result = df.parse(termPostDateStr);
		} catch (ParseException pe) {
			result = null;
		}

		return result;
	}
	
	public static Date getCardPostDate(ObjectAttribute cardPostDate) {
		Date result = null;

		if (cardPostDate == null) {
			return null;
		}

		String cardPostDateStr = cardPostDate.getValue();
		DateFormat df = new SimpleDateFormat("yyyyMMdd");
		try {
			result = df.parse(cardPostDateStr);
		} catch (ParseException pe) {
			result = null;
		}

		return result;
	}
}