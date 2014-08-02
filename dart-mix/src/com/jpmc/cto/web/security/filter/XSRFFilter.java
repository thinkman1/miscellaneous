package com.jpmc.cto.web.security.filter;

import static com.jpmc.cto.web.security.CtoWebSecurityContstants.SK_JPMC_XSRF;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.jpmc.cto.web.security.util.CtoWebSecurityUtils;

/**
 * <p>This filter will prevent <a href="http://en.wikipedia.org/wiki/Cross-site_request_forgery">Cross 
 * Site Request Forgery</a> attacks. It basically just keeps a token in the session
 * and checks that when the user sends a request back it has the correct token. If an invalid or
 * missing token is received, then it throws a {@link org.springframework.security.access.AccessDeniedException}.</p> 
 * 
 * <p>This class is used in conjunction with some javascript that runs on the client side that takes
 * the token and adds a hidden input field on any forms and/or ajax calls on the page to ensure
 * that the same token is sent back to the server upon submission.</p>
 * 
 * <p>When configuring this filter, if you don't set any properties, it will by default only use the
 * token-based validation (described above). You can optionally turn on validation of the HTTP
 * referer header. You can also turn of the token-based validation if you desire.</p> 
 * 
 * @author Andrew J. Pickett
 */
public class XSRFFilter extends OncePerRequestFilter {
	private static final Log LOG = LogFactory.getLog(XSRFFilter.class);
	
	private boolean validateXSRFToken = true;
	private boolean validateHttpReferer = false;

	/**
	 * @see org.springframework.web.filter.GenericFilterBean#initFilterBean()
	 */
	@Override
	public void initFilterBean() throws ServletException {
	}

	/**
	 * @see org.springframework.web.filter.OncePerRequestFilter#doFilterInternal(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.FilterChain)
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws ServletException, IOException {
		if (validateXSRFToken) {
			validateXSRFToken(req);
		} 
		
		if (validateHttpReferer) {
			validateHttpReferer(req);
		}
		
		chain.doFilter(req, resp);
	}
	
	private void validateXSRFToken(HttpServletRequest req) {
		HttpSession sess = req.getSession();
		if (sess.getAttribute(SK_JPMC_XSRF) == null) {
			UUID id = UUID.randomUUID();
			LOG.debug("XSRF variable not set. Adding " + id + " to the session.");
			sess.setAttribute(SK_JPMC_XSRF, id);
		}
		if (req.getMethod().equals("POST")) {
			if (StringUtils.hasText(req.getParameter(SK_JPMC_XSRF))) {
				if (sess.getAttribute(SK_JPMC_XSRF).toString().equals(req.getParameter(SK_JPMC_XSRF).toString())) {
					LOG.debug("Found a match with the XSRF token: " + sess.getAttribute(SK_JPMC_XSRF).toString() + ". Creating new token for next requests.");
				} else {
					LOG.error("We received a POST request with invalid XSRF token. Expected " + sess.getAttribute(SK_JPMC_XSRF) + " but was " + req.getParameter(SK_JPMC_XSRF));
					throw new AccessDeniedException("Cross Site Request Forgery attack suspected: Incorrect token sent from the client.");
				}
			} else {
				String username = CtoWebSecurityUtils.getUser() == null ? "UNKNOWN" : CtoWebSecurityUtils.getUser().getSsoId();
				LOG.warn("We received a POST request with no XSRF security: " + req.getRequestURL().toString() + " from user " + username);
				throw new AccessDeniedException("Cross Site Request Forgery attack suspected: Missing token.");
			}
		} else {
			LOG.debug("Non POST request. We don't care about these.");
		}
	}
	
	private void validateHttpReferer(HttpServletRequest req) {
		if (req.getMethod().equals("POST")) {
			String refererHost = "";
			String reqHost = req.getServerName();
			try {
				refererHost = new URI(req.getHeader("referer")).getHost();
			} catch (Exception e) {
				throw new AccessDeniedException("Cross Site Request Forger attach suspected: Invalid Referer URI.", e);
			}
			
			if (!refererHost.toLowerCase().equals(reqHost.toLowerCase())) {
				throw new AccessDeniedException("Cross Site Request Forger attach suspected: Host mismatch.");
			} else {
				LOG.debug("Valid referer host: " + reqHost);
			}
		} else {
			LOG.debug("Non POST request. We don't care about these.");
		}
	}
	
	/**
	 * @see org.springframework.web.filter.GenericFilterBean#destroy()
	 */
	@Override
	public void destroy() {
	}
}
