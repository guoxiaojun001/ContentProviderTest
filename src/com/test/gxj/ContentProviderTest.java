package com.test.gxj;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class ContentProviderTest extends ContentProvider {
	private DBOpenHelper dbOpenHelper;
	private UriMatcher URI_MATCHER;
	private static final int PERSONS = 0;
	private static final int PERSON = 1;
	public static final String AUTHORITY = "cn.bs.testcontentprovider";
	public static final Uri CONTENT_URL = Uri.parse("content://"+AUTHORITY);

	@Override
	public boolean onCreate() {
		initUriMatcher();
		dbOpenHelper = new DBOpenHelper(getContext());
		return true;
	}

	// 初始化UriMatcher
	private void initUriMatcher() {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		// 表示返回所有的person,其中PERSONS为该特定Uri的标识码
		URI_MATCHER.addURI(AUTHORITY, "person", PERSONS);
		// 表示返回某一个person,其中PERSON为该特定Uri的标识码
		URI_MATCHER.addURI(AUTHORITY, "person/#", PERSON);
	}

	/**
	 * 插入操作: 插入操作只有一种可能:向一张表中插入 返回结果为新增记录对应的Uri 方法db.insert()返回结果为新增记录对应的主键值
	 */
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
		switch (URI_MATCHER.match(uri)) {
		case PERSONS:
			long rowid = db.insert("person", "name,phone,salary", values);
			getContext().getContentResolver().notifyChange(uri, null); 
			return ContentUris.withAppendedId(uri, rowid);
		default:
			throw new IllegalArgumentException("unknown uri" + uri.toString());
		}
		
		
	}

	/**
	 * 更新操作: 更新操作有两种可能:更新一张表或者更新某条数据 在更新某条数据时原理类似于查询某条数据,见下.
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
		int updataNum = 0;
		switch (URI_MATCHER.match(uri)) {
		// 更新表
		case PERSONS:
			updataNum = db.update("person", values, selection, selectionArgs);
			break;
		// 按照id更新某条数据
		case PERSON:
			long id = ContentUris.parseId(uri);
			String where = "personid=" + id;
			if (selection != null && !"".equals(selection.trim())) {
				where = selection + " and " + where;
			}
			updataNum = db.update("person", values, where, selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("unknown uri" + uri.toString());
		}
		
		getContext().getContentResolver().notifyChange(uri, null); 
		return updataNum;
	}

	/**
	 * 删除操作: 删除操作有两种可能:删除一张表或者删除某条数据 在删除某条数据时原理类似于查询某条数据,见下.
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
		int deletedNum = 0;
		switch (URI_MATCHER.match(uri)) {
		// 删除表
		case PERSONS:
			deletedNum = db.delete("person", selection, selectionArgs);
			break;
		// 按照id删除某条数据
		case PERSON:
			long id = ContentUris.parseId(uri);
			String where = "personid=" + id;
			if (selection != null && !"".equals(selection.trim())) {
				where = selection + " and " + where;
			}
			deletedNum = db.delete("person", where, selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("unknown uri" + uri.toString());
		}
		
		getContext().getContentResolver().notifyChange(uri, null); 
		return deletedNum;
	}

	/**
	 * 查询操作: 查询操作有两种可能:查询一张表或者查询某条数据 注意事项: 在查询某条数据时要注意--因为此处是按照personid来查询
	 * 某条数据,但是同时可能还有其他限制.例如: 要求personid为2且name为xiaoming1 所以在查询时分为两步: 第一步:
	 * 解析出personid放入where查询条件 第二部: 判断是否有其他限制(如name),若有则将其 组拼到where查询条件. 详细代码见下.
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
		Cursor cursor;
		switch (URI_MATCHER.match(uri)) {
		// 查询表
		case PERSONS:
			cursor = db.query("person", projection, selection, selectionArgs,
					null, null, sortOrder);
			break;
		// 按照id查询某条数据
		case PERSON:
			// 第一步:
			long id = ContentUris.parseId(uri);
			String where = "personid=" + id;
			// 第二步:
			if (selection != null && !"".equals(selection.trim())) {
				where = selection + " and " + where;
			}
			cursor = db.query("person", projection, where, selectionArgs, null,
					null, sortOrder);
			break;
		default:
			throw new IllegalArgumentException("unknown uri" + uri.toString());
		}
		
		getContext().getContentResolver().notifyChange(uri, null); 
		return cursor;
	}

	/**
	 * 返回当前Uri所代表的数据的MIME类型. 如果该Uri对应的数据可能包含多条记录,那么返回
	 * 字符串应该以"vnd.android.cursor.dir/"开头 如果该Uri对应的数据只包含一条记录,那么返回
	 * 字符串应该以"vnd.android.cursor.item/"开头
	 */
	@Override
	public String getType(Uri uri) {
		switch (URI_MATCHER.match(uri)) {
		case PERSONS:
			return "vnd.android.cursor.dir/persons";
		case PERSON:
			return "vnd.android.cursor.item/person";
		default:
			throw new IllegalArgumentException("unknown uri" + uri.toString());
		}
	}
}
