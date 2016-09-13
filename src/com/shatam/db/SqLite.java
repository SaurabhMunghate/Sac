/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import com.shatam.util.Paths;
import com.shatam.util.U;

public class SqLite {
	String tableName = null;
	PreparedStatement prep;
	Connection conn;
	Statement stat;
	String[] columns;

	public SqLite(String stateName, String tableName) throws SQLException,
			ClassNotFoundException {
		this.tableName = tableName;
		String dbFileName = Paths.combine(Paths.SQLITE_ROOT, stateName + ".db");
		Class.forName("org.sqlite.JDBC");
		conn = DriverManager.getConnection("jdbc:sqlite:" + dbFileName);
		stat = conn.createStatement();

	}

	public SqLite(String stateName, String tableName, String[] columns)
			throws SQLException, ClassNotFoundException {
		this.columns = columns;
		this.tableName = tableName;
		String dbFileName = Paths.combine(Paths.SQLITE_ROOT, stateName + ".db");
		Class.forName("org.sqlite.JDBC");
		conn = DriverManager.getConnection("jdbc:sqlite:" + dbFileName);
		stat = conn.createStatement();
		stat.executeUpdate("drop table if exists " + tableName + ";");

		StringBuffer colBuf = new StringBuffer();
		StringBuffer commaBuf = new StringBuffer();

		for (String col : columns) {
			if (colBuf.length() > 0) {
				colBuf.append(",");
				commaBuf.append(",");
			}
			colBuf.append(col);
			commaBuf.append("?");
		}

		stat.executeUpdate("create table " + tableName + " (" + colBuf + ");");

		prep = conn.prepareStatement("insert into " + tableName + " values ("
				+ commaBuf + ");");
	}

	public void add(Object[] arr) throws SQLException {
		for (int i = 0; i < columns.length; i++) {
			prep.setString(i + 1, U._toStr(arr[i]));

		}

		prep.addBatch();
	}

	public void close() throws SQLException {
		conn.setAutoCommit(false);
		if (prep != null)
			prep.executeBatch();
		conn.setAutoCommit(true);

		conn.close();
	}

	public HashMap<String, String> read(String select, String where,
			String groupBy) throws SQLException {
		if (groupBy == null)
			groupBy = "";
		else
			groupBy = " GROUP BY " + groupBy;

		String query = "select " + select + " from " + this.tableName
				+ " where " + where + " " + groupBy + ";";

		;

		HashMap<String, String> row = new HashMap<String, String>();
		ResultSet rs = stat.executeQuery(query);
		ResultSetMetaData colMetadata = rs.getMetaData();

		int rowIndex = 0;
		while (rs.next()) {
			StringBuffer buf = new StringBuffer(++rowIndex + "]\t");
			for (int colIndex = 0; colIndex < colMetadata.getColumnCount(); colIndex++) {
				String col = colMetadata.getColumnName(colIndex + 1);
				buf.append(col + " = " + rs.getString(col));
				buf.append("\t\t");
			}
			U.log(buf);

		}
		rs.close();
		return row;

	}

	public HashMap<String, String> read(String select, String where)
			throws SQLException {
		return read(select, where, null);

	}

	public String getTableName() {

		return this.tableName;
	}

}
