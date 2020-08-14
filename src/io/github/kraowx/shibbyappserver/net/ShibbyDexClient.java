package io.github.kraowx.shibbyappserver.net;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import io.github.kraowx.shibbyappserver.tools.FormattedOutput;

public class ShibbyDexClient {
	private final long COOKIE_LIFESPAN = 7200*1000;
	private final long COOKIE_REFRESH = COOKIE_LIFESPAN - 60*1000;
	private final String LOGIN_URL = "https://shibbydex.com/login";
	private final String SHIBBYDEX_EMAIL = "shibbyappbot@gmail.com";
	private final String SHIBBYDEX_PASSWORD = "wsPOdW1M9YKJFuva3la9X4ZDBVgRi7l0";
	
	private String authCookie;
	
	private Timer authTimer;
	
	public ShibbyDexClient() throws IOException {
		authCookie = getAuthenticatedCookie();
		startReauthTimer();
	}
	
	public Document getHTMLResource(String url) throws IOException {
		if (authCookie != null) {
			CloseableHttpClient httpclient = HttpClientBuilder.create()
					.setRedirectStrategy(new LaxRedirectStrategy()).build();
			HttpClientContext context = HttpClientContext.create();
			HttpGet httpGet = new HttpGet(url);
			httpGet.addHeader("cookie", authCookie);
			CloseableHttpResponse response = httpclient.execute(httpGet, context);
			Document doc = null;
			try {
			    HttpEntity entity = response.getEntity();
			    doc = Jsoup.parse(entity.getContent(), "utf-8", url);
			    EntityUtils.consume(entity);
			} finally {
			    response.close();
			}
			return doc;
		}
		throw new IOException("Client is not authenticated!");
	}
	
	public void startReauthTimer() {
		if (authTimer == null) {
			authTimer = new Timer();
			authTimer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					System.out.println(FormattedOutput.get("Re-authenticating with ShibbyDex..."));
					try {
						authCookie = getAuthenticatedCookie();
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.out.println(FormattedOutput.get("Re-authentication complete."));
				}
			}, COOKIE_REFRESH, COOKIE_REFRESH);
		}
	}
	
	public void stopReauthTimer() {
		if (authTimer != null) {
			authTimer.cancel();
			authTimer.purge();
			authTimer = null;
		}
	}
	
	/*
	 * Creates an authenticated session cookie.
	 * This gives the client access to all areas of ShibbyDex.
	 */
	private String getAuthenticatedCookie() throws IOException {
		CloseableHttpClient httpclient = HttpClientBuilder.create()
				.setRedirectStrategy(new LaxRedirectStrategy()).build();
		String newSessionCookie = getNewSessionCookie(httpclient);
		String csrfToken = getCSRFToken(httpclient, newSessionCookie);
		List <NameValuePair> authData = new ArrayList <NameValuePair>();
		authData.add(new BasicNameValuePair("_token", csrfToken));
		authData.add(new BasicNameValuePair("email", SHIBBYDEX_EMAIL));
		authData.add(new BasicNameValuePair("password", SHIBBYDEX_PASSWORD));
		HttpResponse resp = httpPost(httpclient, LOGIN_URL,
				getLoginHeaders(newSessionCookie), authData);
//		System.out.println(resp.getBody());
//		System.out.println(resp.getStatus());
//		System.out.println(resp.getCookies());
		return parseAuthCookie(resp);
	}
	
	/*
	 * Parses the CSRF token from the client HTML.
	 * This token will always be the same as long as the same session cookies are sent.
	 */
	private String getCSRFToken(CloseableHttpClient httpclient, String newSessionCookie) throws IOException {
		Map<String, String> headers = getCSRFTokenHeaders(newSessionCookie);
		HttpResponse resp = httpGet(httpclient, LOGIN_URL, headers);
		String body = resp.getBody();
		if (body != null) {
			return body.substring(body.indexOf("csrf-token")+21, body.indexOf("csrf-token")+61);
		}
		return null;
	}
	
	/*
	 * Requests a new cookie so that all subsequent requests appear from the same client.
	 */
	private String getNewSessionCookie(CloseableHttpClient httpclient) throws IOException {
		HttpResponse resp = httpGet(httpclient, LOGIN_URL, null);
		return parseAuthCookie(resp);
	}
	
	private String parseAuthCookie(HttpResponse response) {
		List<Cookie> cookies = response.getCookies();
		String cookie = "";
		for (Cookie c : cookies) {
			if (c.getName().startsWith("XSRF-TOKEN") || c.getName().startsWith("__Secure-shibbydex_session")) {
				cookie += String.format("%s=%s;", c.getName(), c.getValue());
			}
		}
		return cookie;
	}
	
	private Map<String, String> getCSRFTokenHeaders(String cookie) {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36");
		headers.put("Accept", "*/*");
		headers.put("Cookie", cookie);
		return headers;
	}
	
	private Map<String, String> getLoginHeaders(String cookie) {
		Map<String, String> headers = getCSRFTokenHeaders(cookie);
		headers.put("Content-Type", "application/x-www-form-urlencoded");
		headers.put("Accept-Encoding", "gzip, deflate");
		return headers;
	}
	
	private HttpResponse httpGet(CloseableHttpClient httpclient, String urlStr, Map<String, String> headers) throws IOException {
		HttpClientContext context = HttpClientContext.create();
		HttpGet httpGet = new HttpGet(urlStr);
		CloseableHttpResponse response = httpclient.execute(httpGet, context);
		HttpResponse packedResponse = null;
		try {
		    HttpEntity entity = response.getEntity();
		    packedResponse = HttpResponse.fromResponse(response, entity, context);
		    EntityUtils.consume(entity);
		} finally {
		    response.close();
		}
		return packedResponse;
	}
	
	private HttpResponse httpPost(CloseableHttpClient httpclient, String urlStr, Map<String, String> headers,
			List<NameValuePair> formData) throws IOException {
		HttpPost httpPost = new HttpPost(urlStr);
		HttpClientContext context = HttpClientContext.create();
		httpPost.setEntity(new UrlEncodedFormEntity(formData));
		if (headers != null) {
			for (String header : headers.keySet()) {
				httpPost.addHeader(header, headers.get(header));
			}
		}
		CloseableHttpResponse response = httpclient.execute(httpPost, context);
		
		HttpResponse packedResponse = null;
		try {
		    HttpEntity entity = response.getEntity();
		    packedResponse = HttpResponse.fromResponse(response, entity, context);
		    EntityUtils.consume(entity);
		} finally {
		    response.close();
		}
		return packedResponse;
	}
}
