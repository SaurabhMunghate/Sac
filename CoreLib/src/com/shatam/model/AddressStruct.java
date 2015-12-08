package com.shatam.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import com.shatam.util.AbbrReplacement;
import com.shatam.util.ShatamIndexQueryStruct;
import com.shatam.util.StrUtil;
import com.shatam.util.U;

public class AddressStruct implements Comparable<AddressStruct> {

	// @XStreamOmitField
	private HashMap<AddColumns, String> fields = new HashMap<AddColumns, String>();

	// @XStreamOmitField
	public float hitScore;

	private String houseNumber = "99";
	public String unitNumber;
	private String state;
	public String inputAddress;
	public double longitude;
	public double latitude;
	public int _hnDistance = Integer.MAX_VALUE;
	public String unitTypeFromInputAddress;

	private String unitType;
	private String lowNoZipPlus4 = null;
	private String highNoZipPlus4 = null;
	private boolean unitTypeAndZipCalculated = false;

	public AddressStruct(String state) {
		this.state = state;
	}

	public AddressStruct(float hitScore) {
		this.hitScore = hitScore;
	}

	public String getUnitType() {
		if (!unitTypeAndZipCalculated)
			this.calculateSecondaryElements();
		return unitType;
	}

	public String getZip4() {
		if (!unitTypeAndZipCalculated)
			this.calculateSecondaryElements();
		return this.lowNoZipPlus4;
	}

	/*
	 * public String getHighZip4() { if (!unitTypeAndZipCalculated)
	 * this.calculateSecondaryElements(); return this.highNoZipPlus4; }
	 */

	/*
	 * public String TLID; public int FROMHN; public int TOHN; public String
	 * ZIP; public String PREDIRABRV; public String SUFDIRABRV; public String
	 * SUFTYPABRV; public String PREQUALABR; public String NAME; public String
	 * PRETYPABRV; public String CITY;
	 */

	public void put(AddColumns col, Object vo) throws Exception {
		String v = U._toStr(vo);

		if (state != null) {

			if (col.name().startsWith("PRE")) {
				v = AbbrReplacement.getFullName(v, AbbrReplacement.PREFIX,
						state);

			} else if (col.name().startsWith("SUF")) {
				v = AbbrReplacement.getFullName(v, AbbrReplacement.SUFFIX,
						state);

			}
		}
		fields.put(col, v);
	}

	public String get(AddColumns col) {

		String v = fields.get(col);
		return U._toStr(v);
	}

	public boolean contains(AddColumns col) {
		if (!fields.containsKey(col))
			return false;
		return !(StrUtil.isEmpty(fields.get(col)));
	}

	public void close() {

		fields.clear();
		// shatamIndexQueryString = null;

	}

	public String getState() {

		return state;
	}

	public StringBuffer toOnlyStreet() {
		StringBuffer buf = new StringBuffer();
		if (!StrUtil.isEmpty(this.inputAddress)) {
			buf.append(this.houseNumber);
			_a(buf, AddColumns.PREDIRABRV);
			_a(buf, AddColumns.PREQUALABR);
			_a(buf, AddColumns.PRETYPABRV);
			_a(buf, AddColumns.NAME);
			_a(buf, AddColumns.SUFTYPABRV);
			_a(buf, AddColumns.SUFDIRABRV);
			_a(buf, AddColumns.SUFQUALABR);

		}

		if (!StrUtil.isEmpty(this.unitNumber)) {
			if (buf.length() > 0) {
				buf.append(" ");
			}
			buf.append(this.getUnitType());
			buf.append(" ").append(this.unitNumber);
		}

		buf.trimToSize();
		return buf;
	}// toStreetName()

	public StringBuffer toOnlyStreet2() {
		StringBuffer buf = new StringBuffer();
		if (!StrUtil.isEmpty(this.inputAddress)) {
			// buf.append(this.houseNumber);
			_a(buf, AddColumns.PREDIRABRV);
			_a(buf, AddColumns.PREQUALABR);
			_a(buf, AddColumns.PRETYPABRV);
			_a(buf, AddColumns.NAME);
			_a(buf, AddColumns.SUFTYPABRV);
			_a(buf, AddColumns.SUFDIRABRV);
			_a(buf, AddColumns.SUFQUALABR);

		}

		if (!StrUtil.isEmpty(this.unitNumber)) {
			if (buf.length() > 0) {
				buf.append(" ");
			}
			buf.append(this.getUnitType());
			buf.append(" ").append(this.unitNumber);
		}

		buf.trimToSize();
		return buf;
	}// toStreetName()

	public String toFullAddressString2() {
		StringBuffer buf = this.toOnlyStreet();
		buf.append(" ").append(this.get(AddColumns.CITY));
		buf.append(" ").append(this.state);
		buf.append(" ").append(this.get(AddColumns.ZIP));

		if (!StrUtil.isEmpty(this.getZip4())) {
			// buf.append("-").append(this.getZip4());
		}

		return buf.toString().trim().toUpperCase();
	}

	public String toFullAddressString3() {
		StringBuffer buf = this.toOnlyStreet2();
		buf.append(" ").append(this.get(AddColumns.CITY));
		buf.append(" ").append(this.state);
		buf.append(" ").append(this.get(AddColumns.ZIP));

		if (!StrUtil.isEmpty(this.getZip4())) {
			// buf.append("-").append(this.getZip4());
		}

		return buf.toString().trim().toUpperCase();
	}

	public String toFullAddressString4() {
		StringBuffer buf = this.toOnlyStreet();
		buf.append(" ").append(this.get(AddColumns.CITY));
		buf.append(" ").append(this.state);
		buf.append(" ").append(this.get(AddColumns.ZIP));

		if (!StrUtil.isEmpty(this.getZip4())) {
			// buf.append("-").append(this.getZip4());
		}

		return buf.toString().trim().toUpperCase();
	}

	private void calculateSecondaryElements() {

		// U.log("unitTypeFromInputAddress:"+unitTypeFromInputAddress);

		ArrayList<_USPSSecondaryStruct> allLines = getAllDataLines();
		for (int i = 0; i < allLines.size(); i++) {
			if (i > 10)
				break;
			// U.log("SORTED Line: " + allLines.get(i).toString());
		}

		this.unitType = null;
		this.lowNoZipPlus4 = null;
		this.highNoZipPlus4 = null;

		/*
		 * if (StrUtil.isEmpty(unitNumber)) { for (_USPSSecondaryStruct
		 * secStruct : allLines) { if
		 * (StrUtil.isEmpty(secStruct.addrSecondaryAbbr)){ this.lowNoZipPlus4 =
		 * secStruct.lowNoZipPlus4; this.highNoZipPlus4 =
		 * secStruct.highNoZipPlus4; unitTypeAndZipCalculated = true; return; }
		 * } }
		 */

		for (_USPSSecondaryStruct secStruct : allLines) {
			// if (StrUtil.isEmpty(unitNumber) &&
			// StrUtil.isEmpty(secStruct.addrSecondaryAbbr))

			if (StrUtil.isEmpty(unitNumber)
					|| secStruct.isUnitInRange(unitNumber)) {
				if (!StrUtil.isEmpty(secStruct.addrSecondaryAbbr)) {
					this.unitType = secStruct.addrSecondaryAbbr;
				}
				this.lowNoZipPlus4 = secStruct.lowNoZipPlus4;
				this.highNoZipPlus4 = secStruct.highNoZipPlus4;
				unitTypeAndZipCalculated = true;
				break;
			}

		} // for line

		if (StrUtil.isEmpty(unitType)) {
			for (_USPSSecondaryStruct secStruct : allLines) {
				if (!StrUtil.isEmpty(secStruct.addrSecondaryAbbr)) {
					this.unitType = secStruct.addrSecondaryAbbr;
					break;
				}
			}
		}
		if (StrUtil.isEmpty(unitType)
				|| unitTypeFromInputAddress.trim().equalsIgnoreCase("PO BOX")) {
			this.unitType = unitTypeFromInputAddress;
		}

	}// calculateSecondaryElements()

	private static final int _SEC_LINES_LEVELS = 4;

	private ArrayList<_USPSSecondaryStruct> getAllDataLines() {
		ArrayList<ArrayList<_USPSSecondaryStruct>> lists = new ArrayList<ArrayList<_USPSSecondaryStruct>>();
		for (int i = 0; i < _SEC_LINES_LEVELS; i++)
			lists.add(new ArrayList<_USPSSecondaryStruct>());

		for (String line : get(AddColumns.DATA).split("\n")) {
			if (line.length() == 0)
				continue;

			_USPSSecondaryStruct secStruct = new _USPSSecondaryStruct(line);

			int levelI = 0;
			boolean[] bArr = { false, false, false, false };
			bArr[levelI] = secStruct.isHouseNumInRange(houseNumber);
			if (bArr[levelI])
				bArr[++levelI] = secStruct.isOnSameSideOfRoad(houseNumber);
			if (bArr[levelI])
				bArr[++levelI] = (StrUtil.isEmpty(unitNumber) && StrUtil
						.isEmpty(secStruct.addrSecondaryAbbr));
			if (bArr[levelI])
				bArr[++levelI] = secStruct
						.isSameHouseNumAsStartAndEnd(houseNumber);

			for (int i = 0; i < bArr.length; i++) {
				if (bArr[i]) {
					lists.get(i).add(secStruct);
				} else {
					break;
				}
			}
			/*
			 * if (secStruct.isHouseNumInRange(houseNumber)) { int levelI = 0;
			 * lists.get(levelI++).add(secStruct); if
			 * (secStruct.isOnSameSideOfRoad(houseNumber)) {
			 * lists.get(levelI++).add(secStruct);
			 * //U.log("StrUtil.isEmpty("+unitNumber
			 * +") && StrUtil.isEmpty("+secStruct.addrSecondaryAbbr+"):" +
			 * (StrUtil.isEmpty(unitNumber) &&
			 * StrUtil.isEmpty(secStruct.addrSecondaryAbbr))); if
			 * (StrUtil.isEmpty(unitNumber) &&
			 * StrUtil.isEmpty(secStruct.addrSecondaryAbbr)) {
			 * lists.get(levelI++).add(secStruct); if
			 * (secStruct.isSameHouseNumAsStartAndEnd(houseNumber)) {
			 * lists.get(levelI++).add(secStruct); } } } }
			 */

		} // for line

		ArrayList<_USPSSecondaryStruct> allLines = lists.get(0);
		for (int i = lists.size() - 1; i >= 0; i--) {
			if (lists.get(i).size() > 0) {
				allLines = lists.get(i);
				break;
			}
		}

		// U.log("houseNumber:" + houseNumber + "  unitNumber:" + unitNumber);

		Collections.sort(allLines, new SecondaryAddressComparator(houseNumber,
				unitNumber));

		return allLines;

	}// getAllDataLines()

	public String[] toSplitAddress() {
		/*
		 * StringBuffer buf = this.toOnlyStreet();
		 * buf.append(" ").append(this.get(AddColumns.CITY));
		 * buf.append(" ").append(U.STATE_NAME_MAP.get(this.getState()));
		 * buf.append(" ").append(this.get(AddColumns.ZIP));
		 * 
		 * if (!StrUtil.isEmpty(this.getZip4())) {
		 * //buf.append("-").append(this.getZip4()); }
		 * 
		 * return buf.toString().trim().toUpperCase();
		 */
		String pre = this.get(AddColumns.PREDIRABRV);
		String post = this.get(AddColumns.SUFDIRABRV);
		String street = this.get(AddColumns.NAME);
		String city = this.get(AddColumns.CITY);
		String zip = this.get(AddColumns.ZIP);
		String state = this.state;

		String arr[] = new String[] { pre, street, post, city, state, zip };

		return arr;
	}

	private void _a(StringBuffer buf, AddColumns col) {
		String v = this.get(col);
		if (!StrUtil.isEmpty(v)) {
			// U.log(col.name() + " = " + v);
			// if (col.name().equalsIgnoreCase("PRETYPABRV") &&
			// v.equalsIgnoreCase("HIGHWAY"))
			if (col.name().equalsIgnoreCase("PRETYPABRV")) {
				// keep it as is.
			} else if (col.name().startsWith("PRE")) {
				v = AbbrReplacement.getAbbr(v, AbbrReplacement.PREFIX, state);
			} else if (col.name().startsWith("SUF")) {
				v = AbbrReplacement.getAbbr(v, AbbrReplacement.SUFFIX, state);

			} else if (col == AddColumns.NAME && v.equalsIgnoreCase("po box")) {
				return;
			}

			if (buf.length() > 0)
				buf.append(" ");
			buf.append(v);
		}// if

	}// _a()

	public void makeCopy(AddressStruct from) throws Exception {
		for (AddColumns col : AddColumns.values()) {
			put(col, from.get(col));
		}// AddColumns col
		this.latitude = from.latitude;
		this.longitude = from.longitude;

	}// makeCopy

	public boolean isGoodLatLon() {
		boolean badLoc = (latitude + "").equals("NaN")
				|| (longitude + "").equals("NaN") || latitude == 0.0
				|| longitude == 0.0;
		return !badLoc;
	}

	public String getHouseNumber() {
		return this.houseNumber;
	}

	public void setHouseNumber(String hn) {
		this.houseNumber = hn;

	}

	private ShatamIndexQueryStruct queryStruct = null;

	public void setQueryStruct(ShatamIndexQueryStruct shatamIndexQueryStruct) {
		queryStruct = shatamIndexQueryStruct;
	}

	public ShatamIndexQueryStruct getQueryStruct() {
		return queryStruct;
	}
	public String getLuceneQueryString() {
		return queryStruct.getQuery().toUpperCase();
	}
	
	public void setBlank() throws Exception {
		// this.unitTypeFromInputAddress = "";
		this.setHouseNumber("");
		this.put(AddColumns.NAME, "");
		this.put(AddColumns.FULLNAME, "");
		this.put(AddColumns.PREDIRABRV, "");
		this.put(AddColumns.PREQUALABR, "");
		this.put(AddColumns.PRETYPABRV, "");

		this.put(AddColumns.SUFDIRABRV, "");
		this.put(AddColumns.SUFQUALABR, "");
		this.put(AddColumns.SUFTYPABRV, "");
		// inputAddress = "";
	}

	public String getQueryCity() {
		if (queryStruct == null || StrUtil.isEmpty(queryStruct.getCity()))
			return "";
		return queryStruct.getCity().toUpperCase();
	}

	public String getQueryZip() {
		if (queryStruct == null || StrUtil.isEmpty(queryStruct.getZip()))
			return "";
		return queryStruct.getZip().toUpperCase();
	}

	public String getshatamIndexQueryString() {
		return queryStruct.getQuery().toUpperCase();
	}

	public String getFoundName() {
		return get(AddColumns.NAME).toUpperCase();
	}

	public String toFullAddressString() {
		StringBuffer buf = this.toOnlyStreet();
		buf.append(" ").append(this.get(AddColumns.CITY));
		buf.append(" ").append(this.state);
		// buf.append(" ").append(this.get(AddColumns.ZIP));

		if (!StrUtil.isEmpty(this.getZip4())) {
			// buf.append("-").append(this.getZip4());
		}

		return buf.toString().trim().toUpperCase();
	}

	@Override
	public int compareTo(AddressStruct objectAdStruct) {

		Double hitsc = Double.parseDouble(this.hitScore + "");

		Double destsc = Double.parseDouble(objectAdStruct.hitScore + "");
		return destsc.compareTo(hitsc);
	}

}
