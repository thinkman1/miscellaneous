package spring.form.jsp.init.binding.sample;

public class SampleController {
	@InitBinder
	protected void initBinder(final HttpServletRequest request, final WebDataBinder binder) {

		binder.registerCustomEditor(Date.class, new DateEditor());
	}

	public class DateEditor extends PropertyEditorSupport {

		public DateEditor() {
		}

		@Override
		public void setAsText(String text) throws IllegalArgumentException {
			DateFormat dateformat = new SimpleDateFormat(PNCResearchConstants.PROC_DATE_FORMAT);
			DateFormat timeformat = new SimpleDateFormat(PNCResearchConstants.TIMESTAMP_FORMAT);
			List<DateFormat> parseFormatters = Arrays.asList(new DateFormat[] { dateformat, timeformat });

			if (StringUtils.isEmpty(text)) {
				text = null;
			}
			if (text != null) {
				boolean parseable = false;
				List<String> errors = new ArrayList<String>();

				for(DateFormat formatter : parseFormatters) {
					try {
						setValue(new Timestamp(formatter.parse(text).getTime()));
						parseable = true;
					} catch (ParseException e) {
						errors.add(e.getMessage());
					}
				}

				if(!parseable) {
					throw new IllegalArgumentException("Could not convert Date for " + text + ": " + errors.toString());
				}
			}
		}

		@Override
		public String getAsText() {
			if (getValue() == null) {
				return "";
			}

			DateFormat dateformat = new SimpleDateFormat(PNCResearchConstants.PROC_DATE_FORMAT);

			Date value = (Date) getValue();
			return (value != null ? dateformat.format(value) : "");
		}
	}
}
