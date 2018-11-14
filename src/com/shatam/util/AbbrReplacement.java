/* 
 * Copyright (C) Shatam Technologies, Nagpur, India (shatam.com) - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Shatam development team <info@shatam.com>, Aug 2016
 * 
 */
package com.shatam.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map.Entry;

public class AbbrReplacement {
	public static final int SPANISH = 1;
	public static final int ENGLISH = 0;
	static int STANDARD_ABBR = 1;
	static int NONSTANDARD_ABBR = 2;

	private static Hashtable<String, String> _prefixMap = new Hashtable<String, String>();
	private static Hashtable<String, String> _suffixMap = new Hashtable<String, String>();
	private static HashMap<String, String> suffixFull2AbbrMap = new HashMap<String, String>();
	private static HashMap<String, String> priffixFull2AbbrMap = new HashMap<String, String>();
	private static ArrayList<String[]> _SpanishAbbrWithSpaces = new ArrayList<String[]>();
	private static ArrayList<String[]> _EnglishEbbrWithSpaces = new ArrayList<String[]>();
	private static HashMap<String, String> standardFull2AbbrMap = new HashMap<String, String>();

	private static ArrayList<String[]> _SpanishAbbr = new ArrayList<String[]>();
	private static ArrayList<String[]> _EnglishEbbr = new ArrayList<String[]>();

	private static void addAbbr(String abbrev, String full, int is_es,
			int pref, int suff) {
		addAbbr(abbrev, full, is_es, pref, suff, NONSTANDARD_ABBR);
	}

	private static void addAbbr(String abbrev, String full, int is_es,
			int pref, int suff, int abbrType) {
		abbrev = abbrev.trim().toLowerCase();
		full = full.trim().toLowerCase();

		if (abbrType == STANDARD_ABBR) {
			standardFull2AbbrMap.put(full, abbrev);
		}

		if (is_es == SPANISH)
			abbrev = abbrev + "_" + SPANISH;

		if (pref == 1) {
			priffixFull2AbbrMap.put(full, abbrev);
			_prefixMap.put(abbrev, full);
		}

		if (suff == 1) {
			suffixFull2AbbrMap.put(full, abbrev);
			_suffixMap.put(abbrev, full);
		}

		if (abbrev.contains(" ")) {
			if (is_es == SPANISH)
				_SpanishAbbrWithSpaces.add(new String[] { abbrev, full });
			else
				_EnglishEbbrWithSpaces.add(new String[] { abbrev, full });
		}
	}

	static {

		addAbbr("N", "North", ENGLISH, 1, 1, STANDARD_ABBR);
		addAbbr("S", "South", ENGLISH, 1, 1, STANDARD_ABBR);
		addAbbr("W", "West", ENGLISH, 1, 1, STANDARD_ABBR);
		addAbbr("E", "East", ENGLISH, 1, 1, STANDARD_ABBR);
		addAbbr("NE", "Northeast", ENGLISH, 1, 1, STANDARD_ABBR);
		addAbbr("NW", "Northwest", ENGLISH, 1, 1, STANDARD_ABBR);
		addAbbr("SE", "Southeast", ENGLISH, 1, 1, STANDARD_ABBR);
		addAbbr("SW", "Southwest", ENGLISH, 1, 1, STANDARD_ABBR);
		addAbbr("N", "Norte", SPANISH, 1, 1, STANDARD_ABBR);
		addAbbr("S", "Sur", SPANISH, 1, 1, STANDARD_ABBR);
		addAbbr("E", "Este", SPANISH, 1, 1, STANDARD_ABBR);
		addAbbr("O", "Oeste", SPANISH, 1, 1, STANDARD_ABBR);
		addAbbr("NE", "Noreste", SPANISH, 1, 1, STANDARD_ABBR);
		addAbbr("NO", "Noroeste", SPANISH, 1, 1, STANDARD_ABBR);
		addAbbr("SE", "Sudeste", SPANISH, 1, 1, STANDARD_ABBR);
		addAbbr("SO", "Sudoeste", SPANISH, 1, 1, STANDARD_ABBR);
		addAbbr("Acc", "Access", ENGLISH, 0, 1);
		addAbbr("Alt", "Alternate", ENGLISH, 1, 1);
		addAbbr("Byp", "Bypass", ENGLISH, 1, 1);
		addAbbr("Con", "Connector", ENGLISH, 0, 1);
		addAbbr("Exd", "Extended", ENGLISH, 1, 1);
		addAbbr("Exn", "Extension", ENGLISH, 0, 1);
		addAbbr("Hst", "Historic", ENGLISH, 1, 0);
		addAbbr("Lp", "Loop", ENGLISH, 1, 1);
		addAbbr("Old", "Old", ENGLISH, 1, 0);
		addAbbr("Pvt", "Private", ENGLISH, 1, 1);
		addAbbr("Scn", "Scenic", ENGLISH, 0, 1);
		addAbbr("Spr", "Spur", ENGLISH, 1, 1);
		addAbbr("Rmp", "Ramp", ENGLISH, 0, 1);
		addAbbr("Unp", "Underpass", ENGLISH, 0, 1);
		addAbbr("Ovp", "Overpass", ENGLISH, 0, 1);
		addAbbr("Acmdy", "Academy", ENGLISH, 1, 1);
		addAbbr("Acdmy", "Academy", ENGLISH, 1, 1);
		addAbbr("Acueducto", "Acueducto", SPANISH, 1, 0);
		addAbbr("Aero", "Aeropuerto", SPANISH, 1, 0);
		addAbbr("AFB", "Air Force Base", ENGLISH, 0, 1);
		addAbbr("Airfield", "Airfield", ENGLISH, 0, 1);
		addAbbr("Airpark", "Airpark", ENGLISH, 0, 1);
		addAbbr("Arprt", "Airport", ENGLISH, 0, 1);
		addAbbr("Airstrip", "Airstrip", ENGLISH, 0, 1);
		addAbbr("Aly", "Alley", ENGLISH, 0, 1);
		addAbbr("Alleyway", "Alleyway", ENGLISH, 0, 1);
		addAbbr("Apt Bldg", "Apartment Building", ENGLISH, 0, 1);
		addAbbr("Apt Complex", "Apartment Complex", ENGLISH, 0, 1);
		addAbbr("Apts", "Apartments", ENGLISH, 0, 1);
		addAbbr("Apt", "Apartment", ENGLISH, 0, 1);
		addAbbr("ste", "suite", ENGLISH, 0, 1);
		addAbbr("Aqueduct", "Aqueduct", ENGLISH, 0, 1);
		addAbbr("Arc", "Arcade", ENGLISH, 1, 1);
		addAbbr("Arroyo", "Arroyo", SPANISH, 1, 0);
		addAbbr("Asstd Liv Ctr", "Assisted Living Center", ENGLISH, 0, 1);
		addAbbr("Asstd Liv Fac", "Assisted Living Facility", ENGLISH, 0, 1);
		addAbbr("Autopista", "Autopista", SPANISH, 1, 0);
		addAbbr("Avenida", "Avenue", ENGLISH, 1, 0);
		addAbbr("Ave", "Avenue", ENGLISH, 1, 1, STANDARD_ABBR);
		addAbbr("Av", "Avenue", ENGLISH, 1, 1);
		addAbbr("Bahia", "Bahia", SPANISH, 1, 0);
		addAbbr("Bk", "Bank", ENGLISH, 1, 1);
		addAbbr("Base", "Base", ENGLISH, 0, 1);
		addAbbr("Basin", "Basin", ENGLISH, 0, 1);
		addAbbr("Bay", "Bay", ENGLISH, 1, 1);
		addAbbr("Byu", "Bayou", ENGLISH, 1, 1);
		addAbbr("Bch", "Beach", ENGLISH, 0, 1);
		addAbbr("B and B", "Bed and Breakfast", ENGLISH, 0, 1);
		addAbbr("Beltway", "Beltway", ENGLISH, 0, 1);
		addAbbr("Bnd", "Bend", ENGLISH, 0, 1, NONSTANDARD_ABBR);
		addAbbr("Blf", "Bluff", ENGLISH, 0, 1);
		addAbbr("Brdng Hse", "Boarding House", ENGLISH, 0, 1);
		addAbbr("Bog", "Bog", ENGLISH, 0, 1);
		addAbbr("Bosque", "Bosque", SPANISH, 1, 0);
		addAbbr("Blvd", "Boulevard", ENGLISH, 1, 1);
		addAbbr("Boundary", "Boundary", ENGLISH, 0, 1);
		addAbbr("Br", "Branch", ENGLISH, 1, 1);
		addAbbr("Brg", "Bridge", ENGLISH, 0, 1);
		addAbbr("Brk", "Brook", ENGLISH, 0, 1);
		addAbbr("Bldg", "Building", ENGLISH, 1, 1);
		addAbbr("Bulevar", "Bulevar", SPANISH, 1, 0);
		addAbbr("BIA Highway", "Bureau of Indian Affairs Highway", ENGLISH, 1,
				1);
		addAbbr("BIA Highway", "Bureau of Indian Affairs Highway", SPANISH, 1,
				0);
		addAbbr("BIA Hwy", "Bureau of Indian Affairs Highway", ENGLISH, 1, 1);
		addAbbr("BIA Hwy", "Bureau of Indian Affairs Highway", SPANISH, 1, 0);
		addAbbr("BIA Road", "Bureau of Indian Affairs Road", ENGLISH, 1, 1);
		addAbbr("BIA Road", "Bureau of Indian Affairs Road", SPANISH, 1, 0);
		addAbbr("BIA Rd", "Bureau of Indian Affairs Road", ENGLISH, 1, 1);
		addAbbr("BIA Rd", "Bureau of Indian Affairs Road", SPANISH, 1, 0);
		addAbbr("BIA Route", "Bureau of Indian Affairs Route", ENGLISH, 1, 1);
		addAbbr("BIA Route", "Bureau of Indian Affairs Route", SPANISH, 1, 0);
		addAbbr("BIA Rte", "Bureau of Indian Affairs Route", ENGLISH, 1, 1);
		addAbbr("BIA Rte", "Bureau of Indian Affairs Route", SPANISH, 1, 0);
		addAbbr("BLM Rd", "Bureau of Land Management Road", ENGLISH, 1, 1);
		addAbbr("BLM Rd", "Bureau of Land Management Road", SPANISH, 1, 0);
		addAbbr("BLM Road", "Bureau of Land Management Road", ENGLISH, 1, 1);
		addAbbr("BLM Road", "Bureau of Land Management Road", SPANISH, 1, 0);
		addAbbr("Byp", "Bypass", ENGLISH, 1, 1);
		addAbbr("Cll", "Calle", SPANISH, 1, 0);
		addAbbr("Calleja", "Calleja", SPANISH, 1, 0);
		addAbbr("Callejón", "Callejón", SPANISH, 1, 0);
		addAbbr("Cmt", "Caminito", SPANISH, 1, 0);
		addAbbr("Cam", "Camino", SPANISH, 1, 0);
		addAbbr("Cp", "Camp", ENGLISH, 1, 1);
		addAbbr("Cmpgrnd", "Campground", ENGLISH, 0, 1);
		addAbbr("Cmps", "Campus", ENGLISH, 0, 1);
		addAbbr("Cnl", "Canal", ENGLISH, 1, 1);
		addAbbr("Caño", "Caño", SPANISH, 1, 0);
		addAbbr("Cantera", "Cantera", SPANISH, 1, 0);
		addAbbr("Cyn", "Canyon", ENGLISH, 1, 1);
		addAbbr("Capilla", "Capilla", SPANISH, 1, 0);
		addAbbr("Ctra", "Carretera", SPANISH, 1, 0);
		addAbbr("Carr", "Carretera", SPANISH, 1, 0);
		addAbbr("Cswy", "Causeway", ENGLISH, 0, 1);
		addAbbr("Cayo", "Cayo", SPANISH, 1, 0);
		addAbbr("Cem", "Cementerio", SPANISH, 1, 0);
		addAbbr("Cem", "Cemetery", ENGLISH, 0, 1);
		addAbbr("Cementery", "Cemetery", ENGLISH, 0, 1);
		addAbbr("Cemetary", "Cemetery", ENGLISH, 0, 1);
		addAbbr("Cmtry", "Cemetery", ENGLISH, 0, 1);
		addAbbr("Ctr", "Center", ENGLISH, 1, 1);
		addAbbr("Centro", "Centro", SPANISH, 1, 0);
		addAbbr("Cer", "Cerrada", SPANISH, 1, 0);
		addAbbr("Cham of Com", "Chamber of Commerce", ENGLISH, 0, 1);
		addAbbr("Chnnl", "Channel", ENGLISH, 0, 1);
		addAbbr("Cpl", "Chapel", ENGLISH, 1, 1);
		addAbbr("Childrens Home", "Childrens Home", ENGLISH, 1, 1);
		addAbbr("Church", "Church", ENGLISH, 1, 1);
		addAbbr("Cir", "Circle", ENGLISH, 0, 1);
		addAbbr("Cir", "Circulo", SPANISH, 1, 0);
		addAbbr("City Hall", "City Hall", ENGLISH, 0, 1);
		addAbbr("City Park", "City Park", ENGLISH, 0, 1);
		addAbbr("Clf", "Cliff", ENGLISH, 0, 1);
		addAbbr("Clb", "Club", ENGLISH, 1, 1);
		addAbbr("Colegio", "Colegio", SPANISH, 1, 0);
		addAbbr("Colg", "College", ENGLISH, 1, 1);
		addAbbr("Cmn", "Common", ENGLISH, 0, 1);
		addAbbr("Cmns", "Commons", ENGLISH, 1, 1);
		addAbbr("Community Ctr", "Community Center", ENGLISH, 0, 1);
		addAbbr("Community Clg", "Community College", ENGLISH, 1, 1);
		addAbbr("Community Park", "Community Park", ENGLISH, 1, 1);
		addAbbr("Complx", "Complex", ENGLISH, 1, 1);
		addAbbr("Condios", "Condominios", SPANISH, 1, 0);
		addAbbr("Condo", "Condiminium", ENGLISH, 1, 1);
		addAbbr("Condos", "Condiminiums", ENGLISH, 0, 1);
		addAbbr("Cnvnt", "Convent", ENGLISH, 1, 1);
		addAbbr("Convention Ctr", "Convention Center", ENGLISH, 1, 1);
		addAbbr("Cors", "Corners", ENGLISH, 0, 1);
		addAbbr("Corr Faclty", "Correctional Facility", ENGLISH, 0, 1);
		addAbbr("Corr Inst", "Correctional Institute", ENGLISH, 0, 1);
		addAbbr("Corte", "Corte", SPANISH, 1, 0);
		addAbbr("Cottage", "Cottage", ENGLISH, 0, 1);
		addAbbr("Coulee", "Coulee", ENGLISH, 0, 1);
		addAbbr("Country Club", "Country Club", ENGLISH, 1, 1);
		addAbbr("Co Hwy", "County Highway", ENGLISH, 1, 1);
		addAbbr("Co Hwy", "County Highway", SPANISH, 1, 0);
		addAbbr("Co Home", "County Home", ENGLISH, 1, 1);
		addAbbr("Co Ln", "County Lane", ENGLISH, 1, 0);
		addAbbr("Co Park", "County Park", ENGLISH, 0, 1);
		addAbbr("Co Rd", "County Road", ENGLISH, 1, 0);
		addAbbr("Co Rte", "County Route", ENGLISH, 1, 0);
		addAbbr("Cr", "County Road", ENGLISH, 1, 0);
		addAbbr("Co St Aid Hwy", "County State Aid Highway", ENGLISH, 1, 1);
		addAbbr("Co St Aid Hwy", "County State Aid Highway", SPANISH, 1, 0);
		addAbbr("Co Trunk Hwy", "County Trunk Highway", ENGLISH, 1, 1);
		addAbbr("Co Trunk Hwy", "County Trunk Highway", SPANISH, 1, 0);
		addAbbr("Co Trunk Rd", "County Trunk Road", ENGLISH, 1, 1);
		addAbbr("Co Trunk Rd", "County Trunk Road", SPANISH, 1, 0);
		addAbbr("Crs", "Course", ENGLISH, 0, 1);
		addAbbr("Ct", "Court", ENGLISH, 1, 1);
		addAbbr("Courthouse", "Courthouse", ENGLISH, 0, 1);
		addAbbr("Cts", "Courts", ENGLISH, 0, 1);
		addAbbr("Cv", "Cove", ENGLISH, 0, 1);
		addAbbr("Crk", "Creek", ENGLISH, 0, 1);
		addAbbr("Cres", "Crescent", ENGLISH, 0, 1);
		addAbbr("Crst", "Crest", ENGLISH, 0, 1);
		addAbbr("Xing", "Crossing", ENGLISH, 0, 1);
		addAbbr("Xroad", "Crossroads", ENGLISH, 1, 1);
		addAbbr("Cutoff", "Cutoff", ENGLISH, 0, 1);
		addAbbr("Dm", "Dam", ENGLISH, 0, 1);
		addAbbr("Delta Rd", "Delta Road", ENGLISH, 1, 0);
		addAbbr("Dept", "Department", ENGLISH, 1, 1);
		addAbbr("Dep", "Depot", ENGLISH, 0, 1);
		addAbbr("Detention Ctr", "Detention Center", ENGLISH, 0, 1);
		addAbbr("DC Hwy", "District of Columbia Highway", ENGLISH, 1, 1);
		addAbbr("DC Hwy", "District of Columbia Highway", SPANISH, 1, 0);
		addAbbr("Ditch", "Ditch", ENGLISH, 1, 1);
		addAbbr("Dv", "Divide", ENGLISH, 0, 1);
		addAbbr("Dock", "Dock", ENGLISH, 0, 1);
		addAbbr("Dormitory", "Dormitory", ENGLISH, 0, 1);
		addAbbr("Drn", "Drain", ENGLISH, 0, 1);
		addAbbr("Draw", "Draw", ENGLISH, 0, 1);
		addAbbr("Dr", "Drive", ENGLISH, 0, 1);
		addAbbr("Driveway", "Driveway", ENGLISH, 1, 1);
		addAbbr("Drwy", "Driveway", ENGLISH, 1, 1);
		addAbbr("Dump", "Dump", ENGLISH, 0, 1);
		addAbbr("Edif", "Edificio", SPANISH, 1, 0);
		addAbbr("Elem School", "Elementary School", ENGLISH, 0, 1);
		addAbbr("Ensenada", "Ensenada", SPANISH, 1, 0);
		addAbbr("Ent", "Entrada", SPANISH, 1, 0);
		addAbbr("Escuela", "Escuela", SPANISH, 1, 0);
		addAbbr("Esplanade", "Esplanade", ENGLISH, 1, 1);
		addAbbr("Esplanade", "Esplanade", SPANISH, 1, 1);
		addAbbr("Ests", "Estates", ENGLISH, 0, 1);
		addAbbr("Estuary", "Estuary", ENGLISH, 0, 1);
		addAbbr("Expreso", "Expreso", SPANISH, 1, 0);
		addAbbr("Expy", "Expressway", ENGLISH, 1, 1);
		addAbbr("Exp-Way", "Expressway", ENGLISH, 1, 1);
		addAbbr("Ext", "Extension", ENGLISH, 1, 1);
		addAbbr("Faclty", "Facility", ENGLISH, 0, 1);
		addAbbr("Fairgrounds", "Fairgrounds", ENGLISH, 0, 1);
		addAbbr("Fls", "Falls", ENGLISH, 1, 1);
		addAbbr("Frm", "Farm", ENGLISH, 0, 1);
		addAbbr("Farm Rd", "Farm Road", ENGLISH, 1, 0);
		addAbbr("FM", "Farm-to-Market", ENGLISH, 1, 0);
		addAbbr("Fence Line", "Fence Line", ENGLISH, 0, 1);
		addAbbr("Ferry Crossing", "Ferry Crossing", ENGLISH, 1, 1);
		addAbbr("Fld", "Field", ENGLISH, 0, 1);
		addAbbr("Fire Cntrl Rd", "Fire Control Road", ENGLISH, 1, 1);
		addAbbr("Fire Dept", "Fire Department", ENGLISH, 0, 1);
		addAbbr("Fire Dist Rd", "Fire District Road", ENGLISH, 1, 1);
		addAbbr("Fire Ln", "Fire Lane", ENGLISH, 1, 0);
		addAbbr("Fire Rd", "Fire Road", ENGLISH, 1, 0);
		addAbbr("Fire Rte", "Fire Route", ENGLISH, 1, 0);
		addAbbr("Fire Sta", "Fire Station", ENGLISH, 1, 1);
		addAbbr("Fire Trl", "Fire Trail", ENGLISH, 1, 0);
		addAbbr("Flowage", "Flowage", ENGLISH, 0, 1);
		addAbbr("Flume", "Flume", ENGLISH, 0, 1);
		addAbbr("Frst", "Forest", ENGLISH, 0, 1);
		addAbbr("Forest Hwy", "Forest Highway", SPANISH, 1, 1);
		addAbbr("Forest Rd", "Forest Road", ENGLISH, 1, 1);
		addAbbr("Forest Rd", "Forest Road", SPANISH, 1, 0);
		addAbbr("Forest Rte", "Forest Route", ENGLISH, 1, 1);
		addAbbr("Forest Rte", "Forest Route", SPANISH, 1, 0);
		addAbbr("FS Rd", "Forest Service Road", ENGLISH, 1, 1);
		addAbbr("FS Rd", "Forest Service Road", SPANISH, 1, 0);
		addAbbr("F S", "Forest Service", ENGLISH, 1, 1);
		addAbbr("N F S", "National Forest Service", ENGLISH, 1, 1);
		addAbbr("North F S", "National Forest Service", ENGLISH, 1, 1);
		addAbbr("North F South", "National Forest Service", ENGLISH, 1, 1);
		addAbbr("Frk", "Fork", ENGLISH, 0, 1);
		addAbbr("Ft", "Fort", ENGLISH, 1, 0);
		addAbbr("4WD Trl", "Four-Wheel Drive Trail", ENGLISH, 1, 1);
		addAbbr("Frtrnty", "Fraternity", ENGLISH, 0, 1);
		addAbbr("Fwy", "Freeway", ENGLISH, 0, 1);
		addAbbr("Grge", "Garage", ENGLISH, 0, 1);
		addAbbr("Gdns", "Gardens", ENGLISH, 0, 1);
		addAbbr("Gtwy", "Gateway", ENGLISH, 0, 1);
		addAbbr("Gen", "General", ENGLISH, 1, 0);
		addAbbr("Glacier", "Glacier", ENGLISH, 0, 1);
		addAbbr("Gln", "Glen", ENGLISH, 0, 1);
		addAbbr("Golf Club", "Golf Club", ENGLISH, 1, 1);
		addAbbr("Golf Course", "Golf Course", ENGLISH, 1, 1);
		addAbbr("Grade", "Grade", ENGLISH, 0, 1);
		addAbbr("Grn", "Green", ENGLISH, 0, 1);
		addAbbr("Group Home", "Group Home", ENGLISH, 0, 1);
		addAbbr("Gulch", "Gulch", ENGLISH, 0, 1);
		addAbbr("Gulf", "Gulf", ENGLISH, 1, 1);
		addAbbr("Gully", "Gully", ENGLISH, 0, 1);
		addAbbr("Halfway House", "Halfway House", ENGLISH, 0, 1);
		addAbbr("Hall", "Hall", ENGLISH, 0, 1);
		addAbbr("Hbr", "Harbor", ENGLISH, 0, 1);
		addAbbr("Hts", "Heights", ENGLISH, 0, 1);
		addAbbr("High School", "High School", ENGLISH, 0, 1);
		addAbbr("Hwy", "Highway", ENGLISH, 1, 1);
		addAbbr("Hl", "Hill", ENGLISH, 0, 1);
		addAbbr("Holw", "Hollow", ENGLISH, 0, 1);
		addAbbr("Home", "Home", ENGLISH, 1, 1);
		addAbbr("Hosp", "Hospital", ENGLISH, 1, 1);
		addAbbr("Hostel", "Hostel", ENGLISH, 0, 1);
		addAbbr("Hotel", "Hotel", ENGLISH, 1, 1);
		addAbbr("Hse", "House", ENGLISH, 1, 1);
		addAbbr("Hsng", "Housing", ENGLISH, 1, 1);
		addAbbr("Iglesia", "Iglesia", SPANISH, 1, 0);
		addAbbr("Indian Rte", "Indian Route", ENGLISH, 1, 1);
		addAbbr("Indian Svc Rte", "Indian Service Route", ENGLISH, 1, 1);
		addAbbr("Ind St Rte", "Indian State Route", ENGLISH, 1, 1);
		addAbbr("Ind St Rt", "Indian State Route", ENGLISH, 1, 1);
		addAbbr("Indl Park", "Industrial Park", ENGLISH, 0, 1);
		addAbbr("Inlt", "Inlet", ENGLISH, 0, 1);
		addAbbr("Inn", "Inn", ENGLISH, 1, 1);
		addAbbr("Inst", "Institute", ENGLISH, 1, 1);
		addAbbr("Instn", "Institution", ENGLISH, 0, 1);
		addAbbr("Instituto", "Instituto", SPANISH, 1, 0);
		addAbbr("Inter School", "Intermediate School", ENGLISH, 0, 1);
		addAbbr("I-", "Interstate Highway ", ENGLISH, 1, 0);
		addAbbr("Isla", "Isla", SPANISH, 1, 0);
		addAbbr("Is", "Island", ENGLISH, 0, 1);
		addAbbr("Iss", "Islands", ENGLISH, 1, 1);
		addAbbr("Isle", "Isle", ENGLISH, 1, 1);
		addAbbr("Jail", "Jail", ENGLISH, 0, 1);
		addAbbr("Jeep Trl", "Jeep Trail", ENGLISH, 1, 1);
		addAbbr("Junction", "Junction", ENGLISH, 0, 1);
		addAbbr("Jr HS", "Junior High School", ENGLISH, 0, 1);
		addAbbr("Kill", "Kill", ENGLISH, 1, 1);
		addAbbr("Lago", "Lago", SPANISH, 1, 0);
		addAbbr("Lagoon", "Lagoon", ENGLISH, 0, 1);
		addAbbr("Laguna", "Laguna", SPANISH, 1, 0);
		addAbbr("Lk", "Lake", ENGLISH, 1, 1);
		addAbbr("Lks", "Lakes", ENGLISH, 0, 1);
		addAbbr("Lndfll", "Landfill", ENGLISH, 0, 1);
		addAbbr("Lndg", "Landing", ENGLISH, 1, 1, STANDARD_ABBR);
		addAbbr("Landing Area", "Landing Area", ENGLISH, 1, 1);
		addAbbr("Landing Fld", "Landing Field", ENGLISH, 1, 1);
		addAbbr("Landing Strp", "Landing Strip", ENGLISH, 1, 1);
		addAbbr("Ln", "Lane", ENGLISH, 1, 1, STANDARD_ABBR);
		addAbbr("Lateral", "Lateral", ENGLISH, 1, 1);
		addAbbr("Levee", "Levee", ENGLISH, 1, 1);
		addAbbr("Lbry", "Library", ENGLISH, 1, 1);
		addAbbr("Lift", "Lift", ENGLISH, 1, 1);
		addAbbr("Lighthouse", "Lighthouse", ENGLISH, 0, 1);
		addAbbr("Line", "Line", ENGLISH, 1, 1);
		addAbbr("Ldg", "Lodge", ENGLISH, 0, 1);
		addAbbr("Logging Rd", "Logging Road", ENGLISH, 1, 1);
		addAbbr("Loop", "Loop", ENGLISH, 1, 1);
		addAbbr("Mall", "Mall", ENGLISH, 1, 1);
		addAbbr("Mnr", "Manor", ENGLISH, 0, 1);
		addAbbr("Mar", "Mar", SPANISH, 1, 0);
		addAbbr("Marginal", "Marginal", SPANISH, 1, 0);
		addAbbr("Marina", "Marina", ENGLISH, 0, 1);
		addAbbr("Marsh", "Marsh", ENGLISH, 0, 1);
		addAbbr("Mdws", "Meadows", ENGLISH, 0, 1);
		addAbbr("Medical Bldg", "Medical Building", ENGLISH, 0, 1);
		addAbbr("Medical Ctr", "Medical Center", ENGLISH, 1, 1);
		addAbbr("Meml", "Memorial", ENGLISH, 0, 1);
		addAbbr("Memorial Gnds", "Memorial Gardens", ENGLISH, 0, 1);
		addAbbr("Memorial Pk", "Memorial Park", ENGLISH, 0, 1);
		addAbbr("Mesa", "Mesa", ENGLISH, 1, 1);
		addAbbr("Mgmt", "Management", ENGLISH, 1, 1);
		addAbbr("Mid Schl", "Middle School", ENGLISH, 0, 1);
		addAbbr("Mil Res", "Military Reservation", ENGLISH, 0, 1);
		addAbbr("Millpond", "Millpond", ENGLISH, 0, 1);
		addAbbr("Mine", "Mine", ENGLISH, 0, 1);
		addAbbr("Mssn", "Mission", ENGLISH, 1, 1);
		addAbbr("Mobile Hm Cmty", "Mobile Home Community", ENGLISH, 1, 1);
		addAbbr("Mobile Hm Est", "Mobile Home Estates", ENGLISH, 1, 1);
		addAbbr("Mobile Hm Pk", "Mobile Home Park", ENGLISH, 1, 1);
		addAbbr("Monstry", "Monastery", ENGLISH, 1, 1);
		addAbbr("Mnmt", "Monument", ENGLISH, 0, 1);
		addAbbr("Mosque", "Mosque", ENGLISH, 1, 1);
		addAbbr("Mtl", "Motel", ENGLISH, 1, 1);
		addAbbr("Motor Lodge", "Motor Lodge", ENGLISH, 0, 1);
		addAbbr("Mtwy", "Motorway", ENGLISH, 0, 1);
		addAbbr("Mt", "Mount", ENGLISH, 1, 1);
		addAbbr("Mtn", "Mountain", ENGLISH, 0, 1);
		addAbbr("Mus", "Museum", ENGLISH, 1, 1);
		addAbbr("Natl Bfld", "National Battlefield", ENGLISH, 0, 1);
		addAbbr("Natl Bfld Pk", "National Battlefield Park", ENGLISH, 0, 1);
		addAbbr("Natl Bfld Site", "National Battlefield Site", ENGLISH, 0, 1);
		addAbbr("Natl Cnsv Area", "National Conservation Area", ENGLISH, 0, 1);
		addAbbr("Natl Forest", "National Forest", ENGLISH, 1, 1);
		addAbbr("Nf Rd", "National Forest Road", ENGLISH, 0, 1);
		addAbbr("Nat For Dev Rd", "National Forest Development Road", ENGLISH,
				1, 1);
		addAbbr("NFD", "National Forest Development", ENGLISH, 1, 1);
		addAbbr("N F D", "National Forest Development", ENGLISH, 1, 1);
		addAbbr("North F D", "National Forest Development", ENGLISH, 1, 1);
		addAbbr("Nat For Development", "National Forest Development", ENGLISH,
				1, 1);
		addAbbr("Natl Forest Develop", "National Forest Development", ENGLISH,
				1, 1);
		addAbbr("F Dev Rd", "Forest Development Road", ENGLISH, 1, 1);
		addAbbr("F Dev", "Forest Development", ENGLISH, 1, 1);
		addAbbr("N F Dev Rd", "National Forest Development Road", ENGLISH, 1, 1);
		addAbbr("N F Dev", "National Forest Development", ENGLISH, 1, 1);
		addAbbr("North F Dev Rd", "National Forest Development Road", ENGLISH,
				1, 1);
		addAbbr("North F Dev", "National Forest Development", ENGLISH, 1, 1);
		addAbbr("Natl Grsslands", "National Grasslands", ENGLISH, 1, 1);
		addAbbr("Natl Hist Site", "National Historic Site", ENGLISH, 0, 1);
		addAbbr("Natl Hist Pk", "National Historical Park", ENGLISH, 0, 1);
		addAbbr("Natl Lkshr", "National Lakeshore", ENGLISH, 0, 1);
		addAbbr("Natl Meml", "National Memorial", ENGLISH, 0, 1);
		addAbbr("Natl Mil Pk", "National Military Park", ENGLISH, 0, 1);
		addAbbr("Natl Mnmt", "National Monument", ENGLISH, 0, 1);
		addAbbr("Natl Pk", "National Park", ENGLISH, 0, 1);
		addAbbr("Natl Prsv", "National Preserve", ENGLISH, 0, 1);
		addAbbr("Natl Rec Area", "National Recreation Area", ENGLISH, 0, 1);
		addAbbr("Natl Rec Riv", "National Recreational River", ENGLISH, 0, 1);
		addAbbr("Natl Resv", "National Reserve", ENGLISH, 0, 1);
		addAbbr("Natl Riv", "National River", ENGLISH, 0, 1);
		addAbbr("Natl Sc Area", "National Scenic Area", ENGLISH, 0, 1);
		addAbbr("Natl Sc Riv", "National Scenic River", ENGLISH, 0, 1);
		addAbbr("Natl Sc Rvrwys", "National Scenic Riverways", ENGLISH, 0, 1);
		addAbbr("Natl Sc Trl", "National Scenic Trail", ENGLISH, 0, 1);
		addAbbr("Natl Shr", "National Seashore", ENGLISH, 0, 1);
		addAbbr("Natl Wld Rfg", "National Wildlife Refuge", ENGLISH, 0, 1);
		addAbbr("Natl", "National", ENGLISH, 0, 1);
		addAbbr("Navajo Svc Rte", "Navajo Service Route", ENGLISH, 1, 0);
		addAbbr("Naval Air Sta", "Naval Air Station", ENGLISH, 0, 1);
		addAbbr("Nurse Home", "Nursing Home", ENGLISH, 0, 1);
		addAbbr("Ocean", "Ocean", ENGLISH, 0, 1);
		addAbbr("Océano", "Océano", SPANISH, 1, 0);
		addAbbr("Ofc", "Office", ENGLISH, 1, 1);
		addAbbr("Office Bldg", "Office Building", ENGLISH, 0, 1);
		addAbbr("Office Park", "Office Park", ENGLISH, 0, 1);
		addAbbr("Orchard", "Orchard", ENGLISH, 0, 1);
		addAbbr("Orchrds", "Orchards", ENGLISH, 0, 1);
		addAbbr("Orphanage", "Orphanage", ENGLISH, 0, 1);
		addAbbr("Outlet", "Outlet", ENGLISH, 0, 1);
		addAbbr("Oval", "Oval", ENGLISH, 0, 1);
		addAbbr("Opas", "Overpass", ENGLISH, 0, 1);
		addAbbr("Parish Rd", "Parish Road", ENGLISH, 1, 0);
		addAbbr("Park", "Park", ENGLISH, 0, 1);
		addAbbr("Park and Ride", "Park and Ride", ENGLISH, 0, 1);
		addAbbr("Pkwy", "Parkway", ENGLISH, 0, 1, STANDARD_ABBR);
		addAbbr("Pky", "Parkway", ENGLISH, 0, 1);
		addAbbr("Parque", "Parque", SPANISH, 1, 0);
		addAbbr("Pasaje", "Pasaje", SPANISH, 1, 0);
		addAbbr("Pso", "Paseo", SPANISH, 1, 0);
		addAbbr("Pass", "Pass", ENGLISH, 1, 1);
		addAbbr("Psge", "Passage", ENGLISH, 1, 1);
		addAbbr("Path", "Path", ENGLISH, 0, 1);
		addAbbr("Pavilion", "Pavilion", ENGLISH, 0, 1);
		addAbbr("Peak", "Peak", ENGLISH, 0, 1);
		addAbbr("Penitentiary", "Penitentiary", ENGLISH, 0, 1);
		addAbbr("Pier", "Pier", ENGLISH, 1, 1);
		addAbbr("Pike", "Pike", ENGLISH, 0, 1);
		addAbbr("Pipeline", "Pipeline", ENGLISH, 0, 1);
		addAbbr("Pl", "Place", ENGLISH, 0, 1);
		addAbbr("Pla", "Placita", SPANISH, 1, 0);
		addAbbr("Plnt", "Plant", ENGLISH, 0, 1);
		addAbbr("Plantation", "Plantation", ENGLISH, 0, 1);
		addAbbr("Playa", "Playa", SPANISH, 1, 0);
		addAbbr("Playground", "Playground", ENGLISH, 0, 1);
		addAbbr("Plz", "Plaza", ENGLISH, 1, 1);
		addAbbr("Pt", "Point", ENGLISH, 1, 1);
		addAbbr("Pointe", "Pointe", ENGLISH, 0, 1);
		addAbbr("Police Dept", "Police Department", ENGLISH, 1, 1);
		addAbbr("Police Station", "Police Station", ENGLISH, 1, 1);
		addAbbr("Pond", "Pond", ENGLISH, 1, 1);
		addAbbr("Ponds", "Ponds", ENGLISH, 0, 1);
		addAbbr("Prt", "Port", ENGLISH, 1, 1);
		addAbbr("Post Office", "Post Office", ENGLISH, 0, 1);
		addAbbr("Power Line", "Power Line", ENGLISH, 0, 1);
		addAbbr("Power Plant", "Power Plant", ENGLISH, 0, 1);
		addAbbr("Prairie", "Prairie", ENGLISH, 0, 1);
		addAbbr("Preserve", "Preserve", ENGLISH, 0, 1);
		addAbbr("Prison", "Prison", ENGLISH, 0, 1);
		addAbbr("Prison Farm", "Prison Farm", ENGLISH, 0, 1);
		addAbbr("Promenade", "Promenade", ENGLISH, 0, 1);
		addAbbr("Prong", "Prong", ENGLISH, 0, 1);
		addAbbr("Puente", "Puente", SPANISH, 1, 0);
		addAbbr("Quandrangle", "Quadrangle", ENGLISH, 0, 1);
		addAbbr("Quar", "Quarry", ENGLISH, 0, 1);
		addAbbr("Quarters", "Quarters", ENGLISH, 0, 1);
		addAbbr("Qbda", "Quebrada", SPANISH, 1, 0);
		addAbbr("Race", "Race", ENGLISH, 0, 1);
		addAbbr("Rail", "Rail", ENGLISH, 0, 1);
		addAbbr("Rail Link", "Rail Link", ENGLISH, 1, 1);
		addAbbr("Railnet", "Railnet", ENGLISH, 0, 1);
		addAbbr("R ROUTE", "Rural Route", ENGLISH, 0, 1);
		addAbbr("Rlwy", "Railway", ENGLISH, 0, 1);
		addAbbr("Ry", "Railway", ENGLISH, 0, 1);
		addAbbr("Ramal", "Ramal", SPANISH, 1, 0);
		addAbbr("Ramp", "Ramp", ENGLISH, 0, 1);
		addAbbr("Ranch Rd", "Ranch Road", ENGLISH, 1, 0);
		addAbbr("rnch", "Ranch", ENGLISH, 1, 0);
		addAbbr("RM", "Ranch to Market Road", ENGLISH, 1, 0);
		addAbbr("Rch", "Rancho", SPANISH, 1, 0);
		addAbbr("Ravine", "Ravine", ENGLISH, 0, 1);
		addAbbr("Rec Area", "Recreation Area", ENGLISH, 0, 1);
		addAbbr("Reformatory", "Reformatory", ENGLISH, 0, 1);
		addAbbr("Refuge", "Refuge", ENGLISH, 0, 1);
		addAbbr("Regional Pk", "Regional Park", ENGLISH, 0, 1);
		addAbbr("Reservation", "Reservation", ENGLISH, 0, 1);
		addAbbr("Resvn Hwy", "Reservation Highway", ENGLISH, 1, 1);
		addAbbr("Resvn Hwy", "Reservation Highway", SPANISH, 1, 0);
		addAbbr("Resv", "Reserve", ENGLISH, 0, 1);
		addAbbr("Reservoir", "Reservoir", ENGLISH, 1, 1);
		addAbbr("Res Hall", "Residence Hall", ENGLISH, 0, 1);
		addAbbr("Residencial", "Residencial", SPANISH, 1, 0);
		addAbbr("Resrt", "Resort", ENGLISH, 0, 1);
		addAbbr("Rest Home", "Rest Home", ENGLISH, 0, 1);
		addAbbr("Retirement Home", "Retirement Home", ENGLISH, 0, 1);
		addAbbr("Retirement Vlg", "Retirement Village", ENGLISH, 0, 1);
		addAbbr("Rdg", "Ridge", ENGLISH, 0, 1);
		addAbbr("Rio", "Rio", SPANISH, 1, 0);
		addAbbr("Riv", "River", ENGLISH, 0, 1);
		addAbbr("Rd", "Road", ENGLISH, 1, 1);
		addAbbr("Roadway", "Roadway", ENGLISH, 0, 1);
		addAbbr("Rock", "Rock", ENGLISH, 1, 1);
		addAbbr("Romming Hse", "Rooming House", ENGLISH, 0, 1);
		addAbbr("Rte", "Route", ENGLISH, 1, 1);
		addAbbr("routi", "Route", ENGLISH, 1, 1);
		addAbbr("Row", "Row", ENGLISH, 1, 1);
		addAbbr("Rue", "Rue", ENGLISH, 1, 1);
		addAbbr("Run", "Run", ENGLISH, 0, 1);
		addAbbr("Runway", "Runway", ENGLISH, 1, 1);
		addAbbr("Ruta", "Ruta", SPANISH, 1, 0);
		addAbbr("RV Park", "RV Park", ENGLISH, 0, 1);
		addAbbr("Sanitarium", "Sanitarium", ENGLISH, 0, 1);
		addAbbr("Schl", "School", ENGLISH, 1, 1);
		addAbbr("Sea", "Sea", ENGLISH, 1, 1);
		addAbbr("Seashore", "Seashore", ENGLISH, 0, 1);
		addAbbr("Sec", "Sector", SPANISH, 1, 0);
		addAbbr("Smry", "Semindary", ENGLISH, 1, 1);
		addAbbr("Sendero", "Sendero", SPANISH, 1, 0);
		addAbbr("Svc Rd", "Service Road", ENGLISH, 1, 1);
		addAbbr("Shelter", "Shelter", ENGLISH, 0, 1);
		addAbbr("Shop", "Shop", ENGLISH, 0, 1);
		addAbbr("Shopping Ctr", "Shopping Center", ENGLISH, 0, 1);
		addAbbr("Shopping Mall", "Shopping Mall", ENGLISH, 0, 1);
		addAbbr("Shopping Plz", "Shopping Plaza", ENGLISH, 0, 1);
		addAbbr("Site", "Site", ENGLISH, 0, 1);
		addAbbr("Skwy", "Skyway", ENGLISH, 0, 1);
		addAbbr("Slough", "Slough", ENGLISH, 1, 1);
		addAbbr("Sonda", "Sonda", SPANISH, 1, 0);
		addAbbr("Sorority", "Sorority", ENGLISH, 1, 1);
		addAbbr("Snd", "Sound", ENGLISH, 1, 0);
		addAbbr("Spa", "Spa", ENGLISH, 1, 1);
		addAbbr("Speedway", "Speedway", ENGLISH, 1, 1);
		addAbbr("Spg", "Spring", ENGLISH, 0, 1);
		addAbbr("Spur", "Spur", ENGLISH, 1, 1);
		addAbbr("Sq", "Square", ENGLISH, 1, 1);
		addAbbr("State Beach", "State Beach", ENGLISH, 0, 1);
		addAbbr("State Forest", "State Forest", ENGLISH, 0, 1);
		addAbbr("St Beach", "State Beach", ENGLISH, 0, 1);
		addAbbr("St Forest", "State Forest", ENGLISH, 0, 1);
		addAbbr("St FS Rd", "State Forest Service Road", ENGLISH, 1, 1);
		addAbbr("St FS Rd", "State Forest Service Road", SPANISH, 1, 0);
		addAbbr("State Hwy", "State Highway", ENGLISH, 1, 1);
		addAbbr("State Hwy", "State Highway", SPANISH, 1, 0);
		addAbbr("St Hwy", "State Highway", ENGLISH, 1, 1);
		addAbbr("St Hwy", "State Highway", SPANISH, 1, 0);
		addAbbr("St Highway", "State Highway", ENGLISH, 1, 1);
		addAbbr("St Highway", "State Highway", SPANISH, 1, 0);
		addAbbr("State Hospital", "State Hospital", ENGLISH, 1, 1);
		addAbbr("St Hospital", "State Hospital", ENGLISH, 1, 1);
		addAbbr("State Loop", "State Loop", ENGLISH, 1, 0);
		addAbbr("St Loop", "State Loop", ENGLISH, 1, 0);
		addAbbr("State Park", "State Park", ENGLISH, 0, 1);
		addAbbr("St Park", "State Park", ENGLISH, 0, 1);
		addAbbr("State Prison", "State Prison", ENGLISH, 0, 1);
		addAbbr("St Prison", "State Prison", ENGLISH, 0, 1);
		addAbbr("State Rd", "State Road", ENGLISH, 1, 1);
		addAbbr("State Rd", "State Road", SPANISH, 1, 0);
		addAbbr("St Road", "State Road", ENGLISH, 1, 0);
		addAbbr("St Road", "State Road", SPANISH, 1, 0);
		addAbbr("St Rd", "State Road", ENGLISH, 1, 0);
		addAbbr("St Rd", "State Road", SPANISH, 1, 0);
		addAbbr("State Rte", "State Route", ENGLISH, 1, 1);
		addAbbr("State Rte", "State Route", SPANISH, 1, 0);
		addAbbr("St Rte", "State Route", ENGLISH, 1, 1);
		addAbbr("St Rte", "State Route", SPANISH, 1, 0);
		addAbbr("State Rt", "State Route", ENGLISH, 1, 1);
		addAbbr("State Rt", "State Route", SPANISH, 1, 0);
		addAbbr("St Rt", "State Route", ENGLISH, 1, 1);
		addAbbr("St Rt", "State Route", SPANISH, 1, 0);
		addAbbr("St Route", "State Route", ENGLISH, 1, 1);
		addAbbr("St Route", "State Route", SPANISH, 1, 0);
		addAbbr("State Spur", "State Spur", ENGLISH, 1, 0);
		addAbbr("St Spur", "State Spur", ENGLISH, 1, 0);
		addAbbr("St Spr", "State Spur", ENGLISH, 1, 1);
		addAbbr("St Trunk Hwy", "State Trunk Highway", ENGLISH, 1, 1);
		addAbbr("St Trunk Hwy", "State Trunk Highway", SPANISH, 1, 0);
		addAbbr("Sta", "Station", ENGLISH, 0, 1);
		addAbbr("Strait", "Strait", ENGLISH, 1, 1);
		addAbbr("Stra", "Stravenue", ENGLISH, 0, 1);
		addAbbr("Strm", "Stream", ENGLISH, 0, 1);
		addAbbr("St", "Street", ENGLISH, 0, 1, STANDARD_ABBR);
		addAbbr("St No", "Street No", ENGLISH, 1, 0);
		addAbbr("St Of", "Street Of", ENGLISH, 1, 0);
		addAbbr("St of", "Street of", ENGLISH, 1, 0);
		addAbbr("Strip", "Strip", ENGLISH, 1, 1);
		addAbbr("Swamp", "Swamp", ENGLISH, 0, 1);
		addAbbr("Synagogue", "Synagogue", ENGLISH, 1, 1);
		addAbbr("Tank", "Tank", ENGLISH, 0, 1);
		addAbbr("Tmpl", "Temple", ENGLISH, 1, 1);
		addAbbr("Trmnl", "Terminal", ENGLISH, 0, 1);
		addAbbr("Ter", "Terrace", ENGLISH, 1, 1);
		addAbbr("Thoroughfare", "Thoroughfare", ENGLISH, 0, 1);
		addAbbr("Toll Booth", "Toll Booth", ENGLISH, 1, 1);
		addAbbr("Toll Rd", "Toll Road", ENGLISH, 0, 1);
		addAbbr("Tollway", "Tollway", ENGLISH, 0, 1);
		addAbbr("Twr", "Tower", ENGLISH, 1, 1);
		addAbbr("Town Ctr", "Town Center", ENGLISH, 1, 1);
		addAbbr("Town Hall", "Town Hall", ENGLISH, 0, 1);
		addAbbr("Town Hwy", "Town Highway", ENGLISH, 1, 1);
		addAbbr("Town Hwy", "Town Highway", SPANISH, 1, 0);
		addAbbr("Town Rd", "Town Road", ENGLISH, 1, 1);
		addAbbr("Town Rd", "Town Road", SPANISH, 1, 0);
		addAbbr("Towne Ctr", "Towne Center", ENGLISH, 1, 1);
		addAbbr("Twp Hwy", "Township Highway", ENGLISH, 1, 1);
		addAbbr("Twp Hwy", "Township Highway", SPANISH, 1, 0);
		addAbbr("Twp Rd", "Township Road", ENGLISH, 1, 1);
		addAbbr("Twp Rd", "Township Road", SPANISH, 1, 0);
		addAbbr("Trce", "Trace", ENGLISH, 0, 1);
		addAbbr("Trak", "Track", ENGLISH, 1, 1);
		addAbbr("Trfy", "Trafficway", ENGLISH, 0, 1);
		addAbbr("Trl", "Trail", ENGLISH, 1, 1);
		addAbbr("Tr", "Trail", ENGLISH, 0, 1);
		addAbbr("Trailer Ct", "Trailer Court", ENGLISH, 0, 1);
		addAbbr("Trailer Park", "Trailer Park", ENGLISH, 0, 1);
		addAbbr("Trans Ln", "Transmission Line", ENGLISH, 0, 1);
		addAbbr("Trmt Plant", "Treatment Plant", ENGLISH, 1, 1);
		addAbbr("Tribal Rd", "Tribal Road", ENGLISH, 1, 1);
		addAbbr("Trolley", "Trolley", ENGLISH, 1, 1);
		addAbbr("Truck Trl", "Truck Trail", ENGLISH, 1, 1);
		addAbbr("Túnel", "Túnel", SPANISH, 1, 0);
		addAbbr("Tunl", "Tunnel", ENGLISH, 1, 1);
		addAbbr("Tpke", "Turnpike", ENGLISH, 0, 1);
		addAbbr("Upas", "Underpass", ENGLISH, 1, 1);
		addAbbr("Universidad", "Universidad", SPANISH, 1, 0);
		addAbbr("Univ", "University", ENGLISH, 1, 1);
		addAbbr("USFS Hwy", "US Forest Service Highway", ENGLISH, 1, 1);
		addAbbr("USFS Hwy", "US Forest Service Highway", SPANISH, 1, 0);
		addAbbr("USFS Rd", "US Forest Service Road", ENGLISH, 1, 1);
		addAbbr("USFS Rd", "US Forest Service Road", SPANISH, 1, 0);
		addAbbr("US Hwy", "US Highway", ENGLISH, 1, 1);
		addAbbr("US Hwy", "US Highway", SPANISH, 1, 1);
		addAbbr("USFS Rte", "US Forest Service Route", ENGLISH, 1, 1);
		addAbbr("USFS Rte", "US Forest Service Route", SPANISH, 1, 1);
		addAbbr("US Rte", "US Route", SPANISH, 1, 1);
		addAbbr("Vly", "Valley", ENGLISH, 0, 1);
		addAbbr("Ver", "Vereda", SPANISH, 1, 0);
		addAbbr("Via", "Via", SPANISH, 1, 0);
		addAbbr("Viaduct", "Viaduct", ENGLISH, 0, 1);
		addAbbr("Vw", "View", ENGLISH, 0, 1);
		addAbbr("Villa", "Villa", ENGLISH, 1, 1);
		addAbbr("Vlg", "Village", ENGLISH, 1, 1);
		addAbbr("Village Ctr", "Village Center", ENGLISH, 1, 1);
		addAbbr("Vineyard", "Vineyard", ENGLISH, 0, 1);
		addAbbr("Vineyards", "Vineyards", ENGLISH, 0, 1);
		addAbbr("Vis", "Vista", ENGLISH, 1, 1);
		addAbbr("Walk", "Walk", ENGLISH, 0, 1);
		addAbbr("Walkway", "Walkway", ENGLISH, 0, 1);
		addAbbr("Wash", "Wash", ENGLISH, 0, 1);
		addAbbr("Waterway", "Waterway", ENGLISH, 0, 1);
		addAbbr("Way", "Way", ENGLISH, 0, 1);
		addAbbr("Wharf", "Wharf", ENGLISH, 0, 1);
		addAbbr("Wld 0 Snc Riv", "Wild and Scenic River", ENGLISH, 0, 1);
		addAbbr("Wld 0 Scn Riv", "Wild and Scenic River", ENGLISH, 0, 1);
		addAbbr("Wld & Snc Riv", "Wild and Scenic River", ENGLISH, 0, 1);
		addAbbr("Wld & Scn Riv", "Wild and Scenic River", ENGLISH, 0, 1);
		addAbbr("Wild River", "Wild River", ENGLISH, 0, 1);
		addAbbr("Wilderness", "Wilderness", ENGLISH, 0, 1);
		addAbbr("Wilderness Pk", "Wilderness Park", ENGLISH, 0, 1);
		addAbbr("Wldlf Mgt Area", "Wildlife Management Area", ENGLISH, 0, 1);
		addAbbr("Winery", "Winery", ENGLISH, 1, 1);
		addAbbr("Yard", "Yard", ENGLISH, 0, 1);
		addAbbr("Yards", "Yards", ENGLISH, 1, 1);
		addAbbr("YMCA", "YMCA", ENGLISH, 1, 1);
		addAbbr("YWCA", "YWCA", ENGLISH, 1, 1);
		addAbbr("Zanja", "Zanja", SPANISH, 1, 0);
		addAbbr("Zoo", "Zoo", ENGLISH, 1, 1);
		addAbbr("vly", "Valley", ENGLISH, 1, 1);
	}

	private static String _getKey(String input, String state) {
		input = input.trim().toLowerCase();

		String key = input;

		if (isSpanish(state)) {
			key = key + "_" + SPANISH;
		}

		return key;
	}

	public static final int PREFIX = 1;
	public static final int SUFFIX = 2;

	public static String getFullName(String input, int where, String state)
			throws Exception {
		input = input.trim().toLowerCase();
		String key = _getKey(input, state);

		if (where == PREFIX) {
			String v = _prefixMap.get(key);
			return v == null ? input : v;
		}
		if (where == SUFFIX) {
			String v = _suffixMap.get(key);
			return v == null ? input : v;
		}
		throw new Exception("Wrong input");
	}

	public static String replaceLargePhrases(String input, String state)
			throws Exception {
		input = input.trim().toLowerCase();
		ArrayList<String[]> abbrArr = isSpanish(state) ? _SpanishAbbrWithSpaces
				: _EnglishEbbrWithSpaces;

		for (String[] arr : abbrArr) {

			if (input.contains(arr[0])) {

				input = input.replace(arr[0], arr[1]);

			}
		}
		return input.trim();

	}

	public static String getFullName(String input, String state)
			throws Exception {
		input = input.trim().toLowerCase();

		String key = _getKey(input, state);

		String v = _prefixMap.get(key);
		if (StrUtil.isEmpty(v)) {
			v = _suffixMap.get(key);
		}
		return StrUtil.isEmpty(v) ? input : v;
	}

	public static boolean isSpanish(String state) {
		return (state.equals("PR") || state.equals("72"));

	}

	public static String getAbbr(String input, String state) {
		String v = getAbbr(input, SUFFIX, state);
		if (StrUtil.isEmpty(v) || v.equals(input))
			v = getAbbr(input, PREFIX, state);
		return v;
	}

	public static String getAbbr(String input, int where, String state) {
		input = input.trim().toLowerCase();

		if (standardFull2AbbrMap.containsKey(input)) {
			return standardFull2AbbrMap.get(input);
		}

		if (isSpanish(state)) {
			return input;
		}

		Hashtable<String, String> map = null;
		if (where == PREFIX)
			map = _prefixMap;
		if (where == SUFFIX)
			map = _suffixMap;

		for (Entry<String, String> pair : map.entrySet()) {
			if (pair.getKey().endsWith("_" + SPANISH))
				continue;

			if (input.equals(pair.getValue())) {
				return pair.getKey();
			}
		}

		return input;
	}

	public static String getFullAddress(String inputAddress, String state) {
		inputAddress = inputAddress.toLowerCase();

		StringBuffer buf = new StringBuffer();
		for (String a : inputAddress.split(" ")) {

			String abr = (suffixFull2AbbrMap.containsKey(a)) ? suffixFull2AbbrMap
					.get(a) : null;
			if (StrUtil.isEmpty(abr)) {
				abr = (priffixFull2AbbrMap.containsKey(a)) ? priffixFull2AbbrMap
						.get(a) : null;
			}

			if(!StrUtil.isEmpty(abr)){
				abr = abr.replace("_"+SPANISH, "");
			}
			
			if (StrUtil.isEmpty(abr))
				buf.append(a).append(" ");
			else
				buf.append(abr).append(" ");
		}
		return buf.toString().trim().toUpperCase();
	}

	public static String abbrFullAddress(String inputAddress, String state) {
		inputAddress = inputAddress.toLowerCase();

		StringBuffer buf = new StringBuffer();
		for (String a : inputAddress.split(" ")) {
			String abr = (standardFull2AbbrMap.containsKey(a)) ? standardFull2AbbrMap
					.get(a) : null;

			if (StrUtil.isEmpty(abr))
				buf.append(a).append(" ");
			else
				buf.append(abr).append(" ");
		}
		return buf.toString().trim().toUpperCase();
	}

	public static String getsuffixFull(String input, String state) {

		input = input.trim().toLowerCase();
		String key = _getKey(input, state);
		String v = _suffixMap.get(key);

		return StrUtil.isEmpty(v) ? null : v;

	}
}
