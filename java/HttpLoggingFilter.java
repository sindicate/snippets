package solidstack.servlet;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;


public class HttpLoggingFilter implements Filter {

	static private final Logger logger = Logger.getLogger(HttpLoggingFilter.class);
	
	private String name;
	private String characterEncoding;
	private boolean catchExceptions;

	public HttpLoggingFilter() {
	}

	public void init(FilterConfig config) throws ServletException {
		this.name = config.getFilterName();
		this.characterEncoding = config.getInitParameter("characterEncoding");
		this.catchExceptions = "true".equals(config.getInitParameter("catchExceptions"));
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		
		if (!logger.isDebugEnabled()) {
			chain.doFilter(request, response);
		} else {

			String path = ((HttpServletRequest) request).getServletPath();
			if (path.endsWith(".css") || path.endsWith(".js") || path.endsWith(".ico") || path.endsWith(".jpg") || path.endsWith(".png")) {
				chain.doFilter(request, response);
			} else {

				if (!this.catchExceptions)
					doWithLogging(request, response, chain);
				else
					try {
						doWithLogging(request, response, chain);
					} catch (Exception e) {
						logger.error("", e);
					}
			}
		}
	}
	
	public void destroy() {
	}
	
	private void doWithLogging(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		
		if( this.characterEncoding != null )
			request.setCharacterEncoding(this.characterEncoding);

		logRequest((HttpServletRequest) request);
		
		chain.doFilter(request, response);
		
		logResponse((HttpServletRequest) request, (HttpServletResponse) response);
	}

	private void logRequest(HttpServletRequest request) {
		
		StringBuilder b = new StringBuilder( 256 );
		
		b.append("\n(" + this.name + ")");
		b.append("\n	REQUEST: " + request.getClass().getName());
		b.append("\n	> Method="); appendValue(b, request.getMethod());
		b.append("\n	> PathInfo="); appendValue(b, request.getPathInfo());
		b.append("\n	> PathTranslated="); appendValue(b, request.getPathTranslated());
		b.append("\n	> QueryString="); appendValue(b, request.getQueryString());
		b.append("\n	> RequestURI="); appendValue(b, request.getRequestURI());
		b.append("\n	> ServletPath="); appendValue(b, request.getServletPath());
		b.append("\n	> ContentType="); appendValue(b, request.getContentType());
		b.append("\n	> CharacterEncoding="); appendValue(b, request.getCharacterEncoding());
		b.append("\n	> Scheme="); appendValue(b, request.getScheme());
		b.append("\n	> ServerName="); appendValue(b, request.getServerName());
		b.append("\n	> ServerPort=" ); appendValue(b, Integer.toString(request.getServerPort()));
		b.append("\n	> RemoteUser=" ); appendValue(b, request.getRemoteUser());

		b.append("\n	HEADER");
		Enumeration< ? > names = request.getHeaderNames();
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			Enumeration< ? > e = request.getHeaders(name);
			while (e.hasMoreElements())
			{
				b.append("\n	> ");
				b.append(name);
				b.append('=');
				appendValue(b, e.nextElement());
			}
		}

		b.append("\n	PARAMETERS");
		names = request.getParameterNames();
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			String[] values = request.getParameterValues(name);
			for (int i = 0; i < values.length; i++)
			{
				b.append("\n	> ");
				b.append(name);
				b.append('=');
				appendValue(b, values[i]);
			}
		}
		
		b.append("\n	ATTRIBUTES");
		names = request.getAttributeNames();
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			Object value = request.getAttribute(name);
			b.append("\n	> ");
			b.append(name);
			b.append('=');
			appendValue(b, value);
		}
		
		HttpSession session = request.getSession(false);
		if(session != null) {
			b.append("\n	ATTRIBUTES (SESSION)");
			names = session.getAttributeNames();
			while (names.hasMoreElements()) {
				String name = (String) names.nextElement();
				Object value = session.getAttribute(name);
				b.append("\n	> ");
				b.append(name);
				b.append('=');
				appendValue(b, value);
			}
		}
		
		logger.debug(b);
	}

	private void logResponse(HttpServletRequest request, HttpServletResponse response) {
		
		StringBuilder b = new StringBuilder( 256 );
		b.append("\n(" + this.name + ")");
		b.append("\n	RESPONSE for ");
		b.append(request.getRequestURI());
		if(request.getQueryString() != null) {
			b.append('?');
			b.append(request.getQueryString());
		}

		if (response.getClass().getName().equals("org.apache.catalina.connector.ResponseFacade")) {
			
			try {
				Field field = response.getClass().getDeclaredField("response");
				field.setAccessible(true);
				Object _response_ = field.get(response); // org.apache.catalina.connector.Response

				field = _response_.getClass().getDeclaredField("coyoteResponse");
				field.setAccessible(true);
				Object __response__ = field.get(_response_); // org.apache.coyote.Response

				Integer status = (Integer) __response__.getClass().getDeclaredMethod("getStatus").invoke(__response__);
				b.append("\n	< Status="); appendValue(b, status);

				// TODO This one is not tested yet
				String message = (String) __response__.getClass().getDeclaredMethod("getMessage").invoke(__response__);
				b.append("\n	< Message="); appendValue(b, message);

				String contentType = (String) __response__.getClass().getDeclaredMethod("getContentType").invoke(__response__);
				b.append("\n	< ContentType="); appendValue(b, contentType);

				String characterEncoding = (String) __response__.getClass().getDeclaredMethod("getCharacterEncoding").invoke(__response__);
				b.append("\n	< CharacterEncoding="); appendValue(b, characterEncoding);

				Locale locale = (Locale) __response__.getClass().getDeclaredMethod("getLocale").invoke(__response__);
				b.append("\n	< Locale="); appendValue(b, locale.toString());

				b.append("\n	HEADER");

				Object object = _response_.getClass().getDeclaredMethod("getHeaderNames").invoke(_response_);
				if( object instanceof List ) {

					// Tomcat 8
					ArrayList<String> namesList = (ArrayList<String>) object;
					Set<String> names = new LinkedHashSet<String>(namesList);
					Method getHeaderValuesMethod = _response_.getClass().getDeclaredMethod("getHeaders", String.class);
					for (String name : names) {
						Collection<String> values = (Collection<String>) getHeaderValuesMethod.invoke(_response_, name);
						for (String value : values) {
							b.append("\n	< ");
							b.append(name);
							b.append('=');
							appendValue(b, value);
						}
					}
					
				} else {
					
					// Tomcat 6
					String[] namesArray = (String[]) object;
					Set<String> names = new LinkedHashSet<String>(Arrays.asList(namesArray));
					Method getHeaderValuesMethod = _response_.getClass().getDeclaredMethod("getHeaderValues", String.class);
					for (String name : names) {
						String[] values = (String[]) getHeaderValuesMethod.invoke(_response_, name);
						for (int j = 0; j < values.length; j++) 
						{
							b.append("\n	< ");
							b.append(name);
							b.append('=');
							appendValue(b, values[j]);
						}
					}
				}
				
			} catch (NoSuchFieldException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e.getCause());
			}
			
		} else if (response.getClass().getName().equals("com.evermind.server.http.EvermindHttpServletResponse")) {
			
			try {
				b.append("\n	RESPONSE");

				Field field = response.getClass().getDeclaredField("status");
				field.setAccessible(true);
				Integer status = (Integer) field.get(response);
				b.append("\n	< Status="); appendValue(b, status);

				Method method = response.getClass().getDeclaredMethod("getStatusMessage");
				method.setAccessible(true);
				String statusMessage = (String) method.invoke(response);
				b.append("\n	< Message="); appendValue(b, statusMessage);

				String contentType = (String) response.getClass().getDeclaredMethod("getContentType").invoke(response);
				b.append("\n	< ContentType="); appendValue(b, contentType);

				String characterEncoding = (String) response.getClass().getDeclaredMethod("getCharacterEncoding").invoke(response);
				b.append("\n	< CharacterEncoding="); appendValue(b, characterEncoding);

				Locale locale = (Locale) response.getClass().getDeclaredMethod("getLocale").invoke(response);
				b.append("\n	< Locale="); appendValue(b, locale.toString());

				b.append("\n	HEADER");

				String[] headers = (String[]) response.getClass().getDeclaredMethod("getHeaders").invoke(response);
				int headerCount = (Integer) response.getClass().getDeclaredMethod("getHeaderCount").invoke(response);
				for (int i = 0; i < headerCount; i += 2) {
					b.append("\n	< ");
					b.append(headers[i]);
					b.append('=');
					appendValue(b, headers[i + 1]);
				}

			} catch (NoSuchFieldException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e.getCause());
			}
			
		} else {
			b.append("\n	Response logging not implemented for response class [" + response.getClass().getName() + "]");
		}

		logger.debug(b);
	}

	private void appendValue(StringBuilder b, Object o) {
		if (o == null)
			b.append("<null>");
		else
		{
			b.append('[');
			b.append(o);
			b.append(']');
		}
	}
}
