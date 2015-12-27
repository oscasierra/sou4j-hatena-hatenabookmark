package jp.sou4j.hatena;

import java.util.Date;

/**
 * <p>はてなブックマークでブックマークされた記事を表すクラスです</p>
 * @author OSCA
 * @since 1.0.0
 *
 */
public class HatenaBookmarkEntry {

	private String title;
	private String link;
	private String description;
	private String content;
	private Date   date;
	private int    bookmarkCount;

	/**
	 * <p>記事のタイトルを返却します。</p>
	 * @return タイトル
	 */
	public String getTitle() {
		return title;
	}
	protected void setTitle(String title) {
		this.title = title;
	}

	/**
	 * <p>記事のURLを返却します。</p>
	 * @return URL
	 */
	public String getLink() {
		return link;
	}
	protected void setLink(String link) {
		this.link = link;
	}

	/**
	 * <p>記事の説明を返却します。</p>
	 * @return 説明
	 */
	public String getDescription() {
		return description;
	}
	protected void setDescription(String description) {
		this.description = description;
	}

	/**
	 * <p>記事のコンテンツを返却します。</p>
	 * @return コンテンツ
	 */
	public String getContent() {
		return content;
	}
	protected void setContent(String content) {
		this.content = content;
	}

	/**
	 * <p>記事の公開日時を返却します。</p>
	 * @return 公開日時
	 */
	public Date getDate() {
		return date;
	}
	protected void setDate(Date date) {
		this.date = date;
	}

	/**
	 * <p>記事のはてなブックマーク数を返却します。</p>
	 * @return はてなブックマーク数
	 */
	public int getBookmarkCount() {
		return bookmarkCount;
	}
	protected void setBookmarkCount(int bookmarkCount) {
		this.bookmarkCount = bookmarkCount;
	}

}
