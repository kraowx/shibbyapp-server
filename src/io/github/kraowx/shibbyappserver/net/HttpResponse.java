package io.github.kraowx.shibbyappserver.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;

public class HttpResponse {
	private int status;
	private String body;
	private Header[] headers;
	private List<Cookie> cookies;
	
	public static HttpResponse fromResponse(CloseableHttpResponse response,
			HttpEntity entity, HttpClientContext context) {
		try {
			HttpResponse resp = new HttpResponse();
			resp.status = response.getStatusLine().getStatusCode();
			resp.body = parseBody(entity.getContent());
			resp.headers = response.getAllHeaders();
			CookieStore cookieStore = context.getCookieStore();
			resp.cookies = cookieStore.getCookies();
			return resp;
		} catch (IOException e) {
			return null;
		}
	}
	
	public int getStatus() {
		return status;
	}
	
	public String getBody() {
		return body;
	}
	
	public Header[] getHeaders() {
		return headers;
	}
	
	public List<Cookie> getCookies() {
		return cookies;
	}
	
	private static String parseBody(InputStream stream) {
		try {
			StringBuilder sb = new StringBuilder();
			String line;
			BufferedReader in = new BufferedReader(new InputStreamReader(stream));
			while ((line = in.readLine()) != null) {
				sb.append(line);
			}
			return sb.toString();
		} catch (IOException e) {
			return null;
		}
	}
}
