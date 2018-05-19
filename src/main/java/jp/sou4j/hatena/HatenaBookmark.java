package jp.sou4j.hatena;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jp.sou4j.util.StringUtils;
import net.arnx.jsonic.JSON;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * <p>はてなブックマークのサービスにアクセスするためのクラスです。</p>
 * @author OSCA
 * @since 1.0.0
 *
 */
public class HatenaBookmark {

	private static final String URL_OF_SEARCH_TAG   = "http://b.hatena.ne.jp/search/tag?safe={0}&q={1}&users={2}&mode=rss";
	private static final String URL_OF_ENTRY_COUNT  = "http://api.b.st-hatena.com/entry.count?url={0}";
	private static final String URL_OF_ENTRY_COUNTS = "http://api.b.st-hatena.com/entry.counts";

	/**
	 * <p>プロキシのホスト名</p>
	 */
	private String proxyHost;

	/**
	 * <p>プロキシのポート番号</p>
	 */
	private int proxyPort;

	/**
	 * <p>インターネット通信の際に利用するプロキシの情報を設定します。</p>
	 * @param host ホスト名
	 * @param port ポート番号
	 */
	public void setProxy(String host, int port) {
		if( StringUtils.isNullOrEmpty(host) ) throw new IllegalArgumentException("Method argument 'host' is null or empty.") ;
		if( port < 0 ) throw new IllegalArgumentException("Method argument 'port' is negative.") ;

		this.proxyHost = host;
		this.proxyPort = port;
	}

	/**
	 * <p>指定したタグのエントリーを検索します。</p>
	 * @param query タグ文字列
	 * @return エントリー
	 * @throws HatenaException
	 */
	public List<HatenaBookmarkEntry> searchByTag(final String query) throws HatenaException {
		if( query == null ) throw new IllegalArgumentException("Method argument 'query' is null.") ;
		return this.searchByTag(query, 3);
	}

	/**
	 * <p>指定したはてなブックマーク数を持つ指定したタグのエントリーを検索します。</p>
	 * @param query タグ文字列
	 * @param users はてなブックマーク数
	 * @return エントリー
	 * @throws HatenaException
	 */
	public List<HatenaBookmarkEntry> searchByTag(final String query, final int users) throws HatenaException {
		if( query == null ) throw new IllegalArgumentException("Method argument 'query' is null.") ;
		if( users <  0    ) throw new IllegalArgumentException("Method argument 'users' is less than zero.") ;
		return this.searchByTag(query, users, true);
	}

	/**
	 * <p>指定したはてなブックマーク数を持つ指定したタグのエントリーを検索します。</p>
	 * @param query タグ文字列
	 * @param users はてなブックマーク数
	 * @param safe セーフサーチ
	 * @return エントリー
	 * @throws HatenaException
	 */
	public List<HatenaBookmarkEntry> searchByTag(final String query, final int users, final boolean safe) throws HatenaException {
		if( query == null ) throw new IllegalArgumentException("Method argument 'query' is null.") ;
		if( users <  0    ) throw new IllegalArgumentException("Method argument 'users' is less than zero.") ;

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.JAPAN);
		ArrayList<HatenaBookmarkEntry> entries = new ArrayList<HatenaBookmarkEntry>();

		try {
			String   url      = this.generateSearchTagUrl(query, users, safe);

			Document document = this.createDocument(url);
			Node     root     = document.getDocumentElement();
			NodeList rootChildNodes = root.getChildNodes();
			for( int i=0; i<rootChildNodes.getLength(); i++ ) {
				Node rootChildNode = rootChildNodes.item(i);
				if( rootChildNode.getNodeName().equals("item") ) {
					HatenaBookmarkEntry hatenaBookmarkEntry = new HatenaBookmarkEntry();

					Node itemNode = rootChildNode;
					NodeList itemChildNodes = itemNode.getChildNodes();
					for( int j=0; j<itemChildNodes.getLength(); j++) {
						Node itemChildNode = itemChildNodes.item(j);
						if( itemChildNode.getNodeName().equals("title") ) {
							hatenaBookmarkEntry.setTitle(itemChildNode.getTextContent());
						}
						else if( itemChildNode.getNodeName().equals("link") ) {
							hatenaBookmarkEntry.setLink(itemChildNode.getTextContent());
						}
						else if( itemChildNode.getNodeName().equals("description") ) {
							hatenaBookmarkEntry.setDescription(itemChildNode.getTextContent());
						}
						else if( itemChildNode.getNodeName().equals("content:encoded") ) {
							hatenaBookmarkEntry.setContent(itemChildNode.getTextContent());
						}
						else if( itemChildNode.getNodeName().equals("dc:date") ) {
							String dateString = itemChildNode.getTextContent();
							hatenaBookmarkEntry.setDate(dateFormat.parse(dateString));
						}
						else if( itemChildNode.getNodeName().equals("hatena:bookmarkcount") ) {
							String countString = itemChildNode.getTextContent();
							int    count       = ( StringUtils.isNullOrEmpty(countString) ) ? 0 : Integer.parseInt(countString) ;
							hatenaBookmarkEntry.setBookmarkCount(count);
						}
					}

					entries.add(hatenaBookmarkEntry);
				}
			}

			return entries;
		}
		catch( Exception exception ) {
			throw new HatenaException(exception.getMessage(), exception);
		}
	}

	/**
	 * <p>指定したURLのはてなブックマーク数を返却します。</p>
	 * @param url URL
	 * @return はてなブックマーク数
	 * @throws IOException はてなAPIへの通信に失敗した場合
	 * @throws HatenaException はてなAPIが返却したレスポンスの解析に失敗した場合
	 */
	public int getCount(final String url) throws IOException, HatenaException {
		if( url == null ) throw new IllegalArgumentException("Method argument 'url' is null.") ;

		// APIのリクエストURLを生成
		String encodedUrl      = URLEncoder.encode(url, "UTF-8");
		String urlOfEntryCount = MessageFormat.format(URL_OF_ENTRY_COUNT, encodedUrl);

		String contents = this.readContents(urlOfEntryCount);
		if(StringUtils.isNullOrEmpty(contents)) return 0;

		try {
			return Integer.parseInt(contents);
		}
		catch(NumberFormatException exception) {
			throw new HatenaException("コンテンツの取得に失敗しました", exception) ;
		}
	}

	/**
	 * <p>指定した複数のURLのはてなブックマーク数を返却します。</p>
	 * @param urls URL文字列の配列
	 * @return はてなブックマーク数(URL文字列をキーとするMap)
	 * @throws IOException はてなAPIへの通信に失敗した場合
	 * @throws HatenaException はてなAPIが返却するレスポンス文字列の解析に失敗した場合
	 */
	public Map<String, Integer> getCounts(final String[] urls) throws IOException, HatenaException {
		if( urls == null ) throw new IllegalArgumentException("Method argument 'urls' is null.") ;

		// APIのリクエストURLを生成
		String urlOfEntryCounts = URL_OF_ENTRY_COUNTS;
		boolean begin = true;
		for(String url : urls) {
			urlOfEntryCounts += (begin) ? "?" : "&" ;
			urlOfEntryCounts += "url=" + URLEncoder.encode(url, "UTF-8");
			begin = false;
		}

		// HTTPリクエスト&レスポンスボディの取得
		String contents = this.readContents(urlOfEntryCounts);

		return this.parseEntryCountsResponse(contents);
	}

	/**
	 * <p>指定したURLがはてなブックマーク数が0以上か否かを検証し、その結果を返却します。</p>
	 * @param url URL
	 * @return はてなブックマーク数0よりも大きい場合に true を返却します。
	 * @throws IOException はてなAPIへの通信に失敗した場合
	 * @throws HatenaException はてなAPIが返却したレスポンスの解析に失敗した場合
	 */
	public boolean hasCount(final String url) throws IOException, HatenaException {
		if( url == null ) throw new IllegalArgumentException("Method argument 'url' is null.") ;
		return this.hasCount(url, 0);
	}

	/**
	 * <p>指定したURLが指定した数以上のはてなブックマーク数をもっているかを検証し、その結果を返却します。</p>
	 * @param url URL
	 * @param count 基準はてなブックマーク数
	 * @return はてなブックマーク数が指定した数よりも大きい場合に true を返却します。
	 * @throws IOException はてなAPIへの通信に失敗した場合
	 * @throws HatenaException はてなAPIが返却したレスポンスの解析に失敗した場合
	 */
	public boolean hasCount(final String url, final int count) throws IOException, HatenaException {
		if( url   == null ) throw new IllegalArgumentException("Method argument 'url' is null.") ;
		if( count <  0    )	throw new IllegalArgumentException("Method argument 'count' is less than zero.") ;
		return (this.getCount(url) >= count);
	}

	/**
	 * <p>複数のブックマーク数を返却するAPIのレスポンスJSON文字列を解析して、Map型で結果を返却します。</p>
	 * @param jsonString レスポンスJSON文字列
	 * @return ブックマーク数(Map)
	 * @throws HatenaException はてなAPIが返却するレスポンスJSON文字列の解析に失敗した場合
	 */
	private Map<String, Integer> parseEntryCountsResponse(final String jsonString) throws HatenaException {
		if( jsonString == null ) throw new IllegalArgumentException("Method argument 'jsonString' is null.") ;

		Map<String, BigDecimal> jsonMap   = JSON.decode(jsonString);
		Map<String, Integer>    resultMap = new HashMap<String, Integer>();
		for(Map.Entry<String, BigDecimal> entry : jsonMap.entrySet()) {
			try {
				resultMap.put(entry.getKey(), new Integer(entry.getValue().intValue()));
			}
			catch(Exception exception) {
				throw new HatenaException("はてなブックマーク数の取得に失敗しました。 レスポンス文字列："+jsonString, exception);
			}
		}

		return resultMap;
	}

	/**
	 * <p>指定したURLのレスポンスXMLを解析して、そのDocumentオブジェクトを返却します。</p>
	 * @param url URL
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	private Document createDocument(final String url) throws IOException, ParserConfigurationException, SAXException {
		if( url == null ) throw new IllegalArgumentException("Method argument 'url' is null.") ;

		String                 contents = this.readContents(url);
		DocumentBuilderFactory factory  = DocumentBuilderFactory.newInstance();
		DocumentBuilder        builder  = factory.newDocumentBuilder();
		Document               document = builder.parse(new ByteArrayInputStream(contents.getBytes("UTF-8")));

		return document;
	}

	/**
	 * <p>指定したURLにHTTPのGETリクエストを送信し、そのレスポンスボディ文字列を返却します。</p>
	 * @param url URL
	 * @return レスポンスボディ文字列
	 * @throws IOException 指定したURLページへのアクセスに失敗した場合
	 */
	private String readContents(final String url) throws IOException {
		if( StringUtils.isNullOrEmpty(url) ) throw new IllegalArgumentException("Method argument 'url' is null or empty.") ;

		URL urlObject = new URL(url);

		// URLConnection の生成
		URLConnection urlConnection = null;
		if( !StringUtils.isNullOrEmpty(this.proxyHost) && this.proxyPort > 0 ) {
			urlConnection = urlObject.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(this.proxyHost, this.proxyPort)));
		}
		else {
			urlConnection = urlObject.openConnection();
		}

		StringBuilder stringBuilder = new StringBuilder();
		try(InputStream inputStream=urlConnection.getInputStream(); BufferedReader reader=new BufferedReader(new InputStreamReader(inputStream,"UTF-8")); ) {
			String line;
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line);
			}
		}

		return stringBuilder.toString();
	}

	/**
	 * <p>タグ検索用URLを生成します。</p>
	 * @param query クエリ
	 * @param users ユーザ数
	 * @param safe
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private String generateSearchTagUrl(final String query, final int users, final boolean safe) throws UnsupportedEncodingException {
		if( query == null ) throw new IllegalArgumentException("Method argument 'query' is null.") ;
		if( users <  0    ) throw new IllegalArgumentException("Method argument 'users' is less than zero.") ;

		String   safeString  = (safe) ? "on" : "off" ;
		String   queryString = URLEncoder.encode(query, "UTF-8");
		Object[] parameters  = {safeString, queryString, (new Integer(users)).toString()};
		return MessageFormat.format(URL_OF_SEARCH_TAG, parameters);
	}
}
