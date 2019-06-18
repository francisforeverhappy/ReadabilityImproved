package testExample;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 时间抽取类，从网页内容、url中等抽取时间
 * FUNCTIONs: <BR>
 * 1. get time from url <BR>
 * 2. get time from html <BR>
 * 3. get a net time <BR>
 * 4. get a local time <BR>
 * 
 * @author PengX
 * @author Ahui Wang
 * @author  Dong Qijun
 * 
 */
public class TimeUtil {

	/**
	 * 处理出错时返回的时间
	 */
	public static final String DEFAULT_ERROR_TIME = "2001-11-11 11:11:11";

	/**
	 * 运行出现的最早有效日期
	 */
	public static final String EARLIST_TIME = "1990-01-01 00:00:00";
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	/**
	 * get a local date
	 * 
	 * @return yyyy-mm-dd
	 */
	public String getLocalDate() {
		Calendar now = Calendar.getInstance();
		return sdf.format(now.getTime());
	}

	/**
	 * get a net date using http protocol
	 * 
	 * @param site
	 *            http server
	 * @return yyyy-mm-dd | null when exeption happens
	 * @throws IOException
	 */
	@SuppressWarnings("deprecation")
	public String getNetDate(String site) {
		Date d = new Date();
		try {
			URL url = new URL("http://" + site);
			URLConnection urlcon = url.openConnection();
			d.setTime(urlcon.getDate());
			if (d.toString().compareTo("Thu Jan 01 08:00:00 CST 1970") == 0) {
				throw new IOException("Failed to get a date form " + site
						+ " using http protocol");
			}
		} catch (IOException e) {
			return null;
		}
		return String.format("%04d-%02d-%02d", 1900 + d.getYear(), 1 + d
				.getMonth(), d.getDate());
	}

	/**
	 * 时间是否正常
	 * @param year
	 * @param month
	 * @param day
	 * @return  boolean
	 */
	public boolean isNormalDate(String year, String month, String day){
		boolean result = true;
		Calendar now = Calendar.getInstance();
		Calendar date = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		
		if(month != null && month.length() == 1){
			month = "0" + month;
		}
		if(day != null && day.length() == 1){
			day = "0" + day;
		}
		
		try {
			date.setTime(sdf.parse(year + "-" + month + "-" + day));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		if(date.after(now)){
			result = false;
		}
		
		return result;
	}
	
	/**
	 * get first level 1 date from a string <BR>
	 * <BR>
	 * Date format: <BR>
	 * <P>
	 * yyyy-mm-dd yyyy_mm_dd yyyy.mm.dd yyyy/mm/dd <BR>
	 * yyyy-m-dd yyyy_m_dd yyyy.m.dd yyyy/m/dd <BR>
	 * yyyy-mm-d yyyy_mm_d yyyy.mm.d yyyy/mm/d <BR>
	 * yyyy-m-d yyyy_m_d yyyy.m.d yyyy/m/d <BR>
	 * mm/dd/yyyy mm/d/yyyy m/dd/yyyy m/d/yyyy <BR>
	 * </P>
	 * add yyyymm/dd
	 * 
	 * @param str
	 * @return yyyy-mm-dd OR null
	 */
	private String getFirstDateL1(String str) {
		Pattern p = null;
		Matcher m = null;
		String yyyy_mm_dd = "(?<!\\d)([12][09][0-9]{2}[-_./][01]?[0-9][-_./][0123]?[0-9])[^\\d]";
		p = Pattern.compile(yyyy_mm_dd);
		m = p.matcher(str);
		if (m.find()) {
			String[] subStrings = m.group(1).split("[-_./]");
			String yyyy = subStrings[0];
			int mm = Integer.parseInt(subStrings[1]);
			int dd = Integer.parseInt(subStrings[2]);
			if (mm > 0 && dd > 0 && mm <= 12 && dd <= 31 && Integer.parseInt(yyyy) > 1990) {
				return String.format("%s-%02d-%02d", yyyy, mm, dd);
			}
		}
		
		String yyyy_mm_dd2 = "(?<!\\d)([12][09][0-9]{2}[01]?[0-9][0123]?[0-9])";
		p = Pattern.compile(yyyy_mm_dd2);
		m = p.matcher(str);
		if (m.find()) {
			String subStrings = m.group(1);
			if(subStrings.length() == 8){
				String yyyy = subStrings.substring(0, 4);
				int mm = Integer.parseInt(subStrings.substring(4, 6));
				int dd = Integer.parseInt(subStrings.substring(6));
				if (mm > 0 && dd > 0 && mm <= 12 && dd <= 31 && Integer.parseInt(yyyy) > 1990) {
					if(isNormalDate(yyyy, subStrings.substring(4, 6), subStrings.substring(6))){
						return String.format("%s-%02d-%02d", yyyy, mm, dd);
					}
				}
			}
		}
		/*
		 * add in 2019/6/18: add format: yyyymm/dd yyyymm-dd
		 */
		String yyyy_mm_dd3 = "(?<!\\d)([12][09][0-9]{2}[01]?[0-9][-_./][0123]?[0-9])[^\\d]";
		p = Pattern.compile(yyyy_mm_dd3);
		m = p.matcher(str);
		if (m.find()) {
			String[] subStrings = m.group(1).split("[-_./]");
			if(subStrings.length == 2){
				String yyyy = Integer.parseInt(subStrings[0])/100 +"";
				int mm = Integer.parseInt(subStrings[0])%100;
				int dd = Integer.parseInt(subStrings[1]);
				if (mm > 0 && dd > 0 && mm <= 12 && dd <= 31 && Integer.parseInt(yyyy) > 1990) {
					return String.format("%s-%02d-%02d", yyyy, mm, dd);
				}
			}
		}

		String mm_dd_yyyy = "(?<!\\d)([0123]?[0-9]/[01]?[0-9]/[12][09][0-9]{2})[^\\d]";
		p = Pattern.compile(mm_dd_yyyy);
		m = p.matcher(str);
		if (m.find()) {
			String[] subStrings = m.group(1).split("[-_./]");
			String yyyy = subStrings[2];
			int mm = Integer.parseInt(subStrings[0]);
			int dd = Integer.parseInt(subStrings[1]);
			if (mm > 0 && dd > 0 && mm <= 12 && dd <= 31 && Integer.parseInt(yyyy) > 1990) {
				return String.format("%s-%02d-%02d", yyyy, mm, dd);
			}
		}

		// String yyyymmdd = "[12][09]{2}[0-9][01][0-9][0123][0-9]";
		// p = Pattern.compile(yyyymmdd);
		// m = p.matcher(str);
		// if (m.find()) {
		// String date = m.group();
		// String yyyy = date.substring(0, 4);
		// int mm = Integer.parseInt(date.substring(4, 6));
		// int dd = Integer.parseInt(date.substring(6, 8));
		// if (mm > 0 && dd > 0 && mm <= 12 && dd <= 31) {
		// return String.format("%s-%02d-%02d", yyyy, mm, dd);
		// }
		// }

		return null;
	}

	/**
	 * get first level 2 date from a string <BR>
	 * <BR>
	 * Date fomat: <BR>
	 * yy-mm-dd yy_mm_dd yy.mm.dd yy/mm/dd <BR>
	 * yy-m-dd yy_m_dd yy.m.dd yy/m/dd <BR>
	 * yy-mm-d yy_mm_d yy.mm.d yy/mm/d <BR>
	 * yy-m-d yy_m_d yy.m.d yy/m/d <BR>
	 * 
	 * @param str
	 * @return yyyy-mm-dd OR null
	 */
	private String getFirstDateL2(String str) {
		Pattern p = null;
		Matcher m = null;

		String yy_mm_dd = "(?<!\\d)([0-9][0-9][-_./][01]?[0-9][-_./][0123]?[0-9])[^\\d]";
		p = Pattern.compile(yy_mm_dd);
		m = p.matcher(str);
		if (m.find()) {
			String[] subStrings = m.group(1).split("[-_./]");
			String yy = subStrings[0];
			int mm = Integer.parseInt(subStrings[1]);
			int dd = Integer.parseInt(subStrings[2]);
			if (mm > 0 && dd > 0 && mm <= 12 && dd <= 31) {
				return String.format("20%s-%02d-%02d", yy, mm, dd);
//				if (yy.charAt(0) == '0') {
//					return String.format("20%s-%02d-%02d", yy, mm, dd);
//				} else {
//					return String.format("19%s-%02d-%02d", yy, mm, dd);
//				}
			}
		}
		return null;
	}

	/**
	 * get date from url string
	 * 
	 * @param url
	 * @return yyyy-mm-dd OR null
	 */
	public  String getDateFromUrl(String url) {
		if(url == null || "".equals(url.trim())){
			return null;
		}
		try {
			String date = null;
			if ((date = this.getFirstDateL1(url)) != null) {
				return date;
			}
			if ((date = this.getFirstDateL2(url)) != null) {
				return date;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) {
		TimeUtil time=new TimeUtil();
		String result=time.getDateFromUrl("http://www.blueridgenow.com/article/20150923/API/309239935");
		System.out.println(result);
	}
	/**
	 * get date from html string
	 * 
	 * @param html 网页字节数组
	 * @param charset 网页编码
	 * @param title 网页标题
	 * @return yyyy-mm-dd OR null
	 */
	
	public String getDateFromHtml(byte[]html,String charset,String title,String url){
		 charset=(charset == null ||"".equals(charset))? "utf-8":charset;
		 String dateTime = getDateFromUrl(url);
		 if(dateTime == null){
			 try {
				 dateTime= getDateFromHtml(new String(html,charset), title);
			 } catch (Exception e) {
				 // TODO Auto-generated catch block
				 e.printStackTrace();
			 }
		 }
		 if(dateTime == null){
			dateTime = sdf.format(new Date());
		 }
		 return dateTime;
	}
	/**
	 * get date from html string
	 * 
	 * @param html
	 * @return yyyy-mm-dd OR null
	 */
	public String getDateFromHtml(String html, String title) {
		Vector<WeightedDate> v = new Vector<WeightedDate>();
		this.loadHtml(html, title);
		String ecpochtimeDate = getEpochtimesDate();
		if(ecpochtimeDate != null){
			return ecpochtimeDate;
		}
		WeightedDate[] wds = new WeightedDate[10];
		wds[0] = this.getHtmlDateL1();
		wds[1] = this.getHtmlDateChL1();
		wds[2] = this.getHtmlDateEnL1();
		for (int i = 0; i < 3; i++) {
			if (wds[i] != null) {
				v.add(wds[i]);
			}
		}
		if (v.size() > 0) {
			this.unloadHtml();
			return this.getBest(v).date;
		}

		wds[3] = this.getHtmlDateL2();
		wds[4] = this.getHtmlDateChL2();
		wds[5] = this.getHtmlDateL3();
		wds[6] = this.getHtmlDateL4();
		wds[7] = this.getHtmlDateChL3();
		wds[8] = this.getHtmlDateChL4();
		for (int i = 3; i < 9; i++) {
			if (wds[i] != null) {
				v.add(wds[i]);
			}
		}
		if (v.size() > 0) {
			this.unloadHtml();
			return this.getBest(v).date;
		}

		wds[9] = this.getHtmlDateChL5();
		this.unloadHtml();
		if (wds[9] != null) {
			return wds[9].date;
		}
		return null;
	}
	
	/**
	 * 特殊处理大纪元网站的时间
	 * @return  String
	 */
	private String getEpochtimesDate(){
		if(this.html.indexOf("大纪元") < 0 || this.html.indexOf("美东时间:") < 0){
			return null;
		}
		Pattern p = null;
		Matcher m = null;

		String yyyy_mm_dd_hh_mm_ss = "[12][09][0-9]{2}-[01][0-9]-[0123]?[0-9]\\s+(?:[01]?[0-9]|2[0-4])[:：](?:[0-5]?[0-9]):[0-9]{1,2}";
		p = Pattern.compile(yyyy_mm_dd_hh_mm_ss);
		m = p.matcher(this.html);
		String subString = null;
		if (m.find()) {
			subString = m.group();
		}
		return subString;
	}
	
	/**
	 * 判断获取的日期是不是在JAVASCRIPT脚本里
	 * 如果在，则丢弃
	 * @param index
	 * @return  boolean
	 */
	private boolean isInScript(int index){
		String script = "script";
		int scriptEndIndex = this.html.indexOf("</script>", index);
		if(scriptEndIndex < 0){
			script = "SCRIPT";
			scriptEndIndex = this.html.indexOf("</" + script + ">", index);
			if(scriptEndIndex < 0){
				return false;
			}
		}
		else{
			int tmp = this.html.indexOf("</SCRIPT>", index);
			if(tmp > 0 && tmp < scriptEndIndex){
				script = "SCRIPT";
				scriptEndIndex = tmp;
			}
		}
		int scriptBeginIndex = this.html.indexOf("<" + script, index);
		if(scriptBeginIndex < 0){
			if(script.equals("SCRIPT")){
				scriptBeginIndex = this.html.indexOf("<" + script.toLowerCase(), index);
			}
			else{
				scriptBeginIndex = this.html.indexOf("<" + script.toUpperCase(), index);
			}
		}
		if(scriptBeginIndex < 0){
			scriptBeginIndex = this.html.length();
		}
		
		if(scriptBeginIndex < scriptEndIndex){
			return false;
		}
		else{
			return true;
		}
	}

	/**
	 * weighted date <BR>
	 * Used for extracting date from html
	 * 
	 * @author Ahui Wang
	 * 
	 */
	private class WeightedDate {

		int weight = Integer.MAX_VALUE;

		String date = null;

		void setWeight(int offset, int headPos, int weight) {
			if(weight == Weight.yyyy_mm_dd_hh_MM){
				this.weight = 1;
				return;
			}
			if (offset - headPos < 0 ) {
				return;
			}
			this.weight = offset - headPos + weight;
		}

		void setDate(String yyyy, int mm, int dd) {
			if (mm <= 0 || dd <= 0 || mm > 12 || dd > 31) {
				return;
			}

			if(yyyy.length() == 2 && (yyyy.charAt(0) == '0' || yyyy.charAt(0) == '1')){
				yyyy = "20" + yyyy;
			}
			else if(yyyy.length() == 2){
				yyyy = "19" + yyyy;
			}
			this.date = String.format("%s-%02d-%02d", yyyy, mm, dd);
		}

		void setDate(String yyyy, int mm, int dd, int hh, int MM, int ss) {
			if (mm <= 0 || dd <= 0 || mm > 12 || dd > 31 || hh > 24 || MM > 59 || ss > 59) {
				return;
			}
			if(yyyy.length() == 2 && (yyyy.charAt(0) == '0' || yyyy.charAt(0) == '1')){
				yyyy = "20" + yyyy;
			}
			else if(yyyy.length() == 2){
				yyyy = "19" + yyyy;
			}
			this.date = String.format("%s-%02d-%02d %02d:%02d:%02d", yyyy, mm, dd, hh, MM ,ss);
		}
	}

	/**
	 * get best WeightedDate from a WeightedDate Vector
	 * 
	 * @param wds
	 * @return best WeightedDate
	 */
	private WeightedDate getBest(Vector<WeightedDate> wds) {
		int size = wds.size();
		if (size == 0) {
			return null;
		}
		WeightedDate best = wds.get(0);
		for (WeightedDate wd : wds) {
			if (wd.date != null && wd.weight < best.weight) {
				best = wd;
			}
			else if(best.date == null){
				best = wd;
			}
		}
		return best;
	}

	/**
	 * position of lable H
	 */
	private int hPosition = 0;

	private String html = null;

	/**
	 * load html string <BR>
	 * caculate title position <BR>
	 * (parse h1 h2 h3)
	 * 
	 * @param html
	 */
	private void loadHtml(String html, String title) {
		this.html = html;
		for(int i = 1; i < 7; i++){
			String hi = "<h" + i + ".*?</h" + i + ">";
			if(regexH(hi)){
				return;
			}
		}
		if(title != null && this.getTitlePosition(title)){
			return;
		}
		if(regexH("<STRONG>")){
			return;
		}
		if(regexH("<B>")){
			return;
		}
		
		this.hPosition = this.html.indexOf("<body");
	}
	
	private boolean regexH(String regex){
		Pattern p = null;
		Matcher m = null;
		p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		m = p.matcher(this.html);
		int proIndex = 0;
		int hNum = 0;
		while (m.find()) {
			hNum++;
			if(proIndex != 0 && m.start() - proIndex > 1000){//处理频道链接使用H标签的情况
//				proIndex = m.start();
				break;
			}
			else{
				if(m.start() < this.html.length() / 8){
					proIndex = m.start();
				}
			}
		}
		if(hNum == 1 || proIndex != 0){
			this.hPosition = proIndex + 10;
			return true;
		}
		return false;
	}

	/**
	 * 定位标题位置
	 * @param title
	 * @return  boolean
	 */
	private boolean getTitlePosition(String title){
		int bodyIndex = this.html.indexOf("<body");
		if(bodyIndex == -1){
			bodyIndex = this.html.indexOf("<BODY");
		}
		int titleIndex = this.strIndex(title, bodyIndex);
		if(titleIndex == -1){
			return false;
		}
		
		int tmpIdex = -1;
		Pattern p = Pattern.compile("<([^>]*)>[^<]*" + title + "[^<]*</([^>]*)>", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(this.html);
		String nodeName = null;
		while(m.find()){
			nodeName = m.group(2);
			if(!nodeName.equalsIgnoreCase("A") && !nodeName.equalsIgnoreCase("TITLE")){
				tmpIdex = m.start(1);
				break;
			}
		}
		
		if(tmpIdex != -1){
			titleIndex = tmpIdex;
		}
		this.hPosition = titleIndex;
		return true;
	}
	
	private int strIndex(String title, int fromIndex){
		return this.html.indexOf(title, fromIndex);
	}
	
	/**
	 * release memory <BR>
	 * reset hPosition
	 */
	private void unloadHtml() {
		this.hPosition = 0;
		this.html = null;
	}

	/**
	 * get level 1 date from html <BR>
	 * <BR>
	 * Date format: <BR>
	 * <P>
	 * yyyy-mm-dd yyyy_mm_dd yyyy.mm.dd yyyy/mm/dd <BR>
	 * yyyy-m-dd yyyy_m_dd yyyy.m.dd yyyy/m/dd <BR>
	 * yyyy-mm-d yyyy_mm_d yyyy.mm.d yyyy/mm/d <BR>
	 * yyyy-m-d yyyy_m_d yyyy.m.d yyyy/m/d <BR>
	 * mm/dd/yyyy mm/d/yyyy m/dd/yyyy m/d/yyyy <BR>
	 * yyyymmdd <BR>
	 * </P>
	 * 
	 * @return the best level 1 weighted date
	 */
	private WeightedDate getHtmlDateL1() {
		Pattern p = null;
		Matcher m = null;
		Vector<WeightedDate> wds = new Vector<WeightedDate>();

		String yyyy_mm_dd_hh_mm_ss = "[12][09][0-9]{2}[-_./][01]?[0-9][-_./][0123]?[0-9]\\s+(?:[01]?[0-9]|2[0-4])[:：](?:[0-5]?[0-9])([:：][0-9]{1,2}){0,1}";
		p = Pattern.compile(yyyy_mm_dd_hh_mm_ss);
		m = p.matcher(this.html);
		while (m.find()) {
			WeightedDate wd = new WeightedDate();
			String[] subStrings = m.group().replaceAll("\\s{2,}", " ").split("[-_./ :：]");
			try {
				if (subStrings.length == 5) {
					wd.setDate(subStrings[0], Integer.parseInt(subStrings[1]),
							Integer.parseInt(subStrings[2]), Integer
									.parseInt(subStrings[3]), Integer
									.parseInt(subStrings[4]), 0);
				} else {
					wd.setDate(subStrings[0], Integer.parseInt(subStrings[1]),
							Integer.parseInt(subStrings[2]), Integer
									.parseInt(subStrings[3]), Integer
									.parseInt(subStrings[4]), Integer
									.parseInt(subStrings[5]));
				}
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
			if(this.isInScript(m.start()) || Integer.parseInt(subStrings[0]) < 1990){
				continue;
			}
			wd.setWeight(m.start(), this.hPosition, Weight.yyyy_mm_dd_hh_MM);
			wds.add(wd);
		}

		String yyyy_mm_dd = "(?<!\\d)([12][09][0-9]{2}[-_./][01]?[0-9][-_./][0123]?[0-9])";
		p = Pattern.compile(yyyy_mm_dd);
		m = p.matcher(this.html);
		while (m.find()) {
			if(m.group(1).split("-").length != 3 && m.group(1).split("_").length != 3 && m.group(1).split("/").length != 3 && m.group(1).split("\\.").length != 3){
				continue;
			}
			WeightedDate wd = new WeightedDate();
			String[] subStrings = m.group(1).split("[-_./]");
			if(this.isInScript(m.start()) || Integer.parseInt(subStrings[0]) < 1990){
				continue;
			}
			wd.setDate(subStrings[0], Integer.parseInt(subStrings[1]), Integer
					.parseInt(subStrings[2]));
			wd.setWeight(m.start(), this.hPosition, Weight.yyyy_mm_dd);
			wds.add(wd);
		}

		String mm_dd_yyyy = "(?<!\\d)([01]?[0-9][-_./][0123]?[0-9][-_./][12][09][0-9]{2})";
		p = Pattern.compile(mm_dd_yyyy);
		m = p.matcher(this.html);
		while (m.find()) {
			WeightedDate wd = new WeightedDate();
			String date = m.group(1);
			if(date.split("-").length != 3 && date.split(".").length != 3 && date.split("/").length != 3){
				continue;
			}
			String[] subStrings = date.split("[-_./]");
			if(this.isInScript(m.start()) || Integer.parseInt(subStrings[2]) < 1990){
				continue;
			}
			wd.setDate(subStrings[2], Integer.parseInt(subStrings[0]), Integer
					.parseInt(subStrings[1]));
			wd.setWeight(m.start(), this.hPosition, Weight.mm_dd_yyyy);
			wds.add(wd);
		}
		
		String dd_mm_yyyy = "(?<!\\d)([0123]?[0-9][-_./][01]?[0-9][-_./][12][09][0-9]{2})";
		p = Pattern.compile(dd_mm_yyyy);
		m = p.matcher(this.html);
		while (m.find()) {
			WeightedDate wd = new WeightedDate();
			String date = m.group(1);
			if(date.split("-").length != 3 && date.split(".").length != 3 && date.split("/").length != 3){
				continue;
			}
			String[] subStrings = date.split("[-_./]");
			if(this.isInScript(m.start()) || Integer.parseInt(subStrings[2]) < 1990){
				continue;
			}
			wd.setDate(subStrings[2], Integer.parseInt(subStrings[1]), Integer
					.parseInt(subStrings[0]));
			wd.setWeight(m.start(), this.hPosition, Weight.yyyy_mm_dd);
			wds.add(wd);
		}

		String yyyymmdd = "(?<!\\d)([12][09][0-9]{2}[01][0-9][0123][0-9])";
		p = Pattern.compile(yyyymmdd);
		m = p.matcher(this.html);
		while (m.find()) {
			String date = m.group(1);
			if(this.isInScript(m.start()) || Integer.parseInt(date.substring(0, 4)) < 1990){
				continue;
			}
			WeightedDate wd = new WeightedDate();
			if(Integer.parseInt(date.substring(0, 4)) < 1990){
				continue;
			}
			wd.setDate(date.substring(0, 4), Integer.parseInt(date.substring(4,
					6)), Integer.parseInt(date.substring(6, 8)));
			wd.setWeight(m.start(), this.hPosition, Weight.yyyymmdd);
			wds.add(wd);
		}

		return this.getBest(wds);
	}

	/**
	 * get best level 2 date from html <BR>
	 * <BR>
	 * Date fomat: <BR>
	 * yy-mm-dd yy_mm_dd yy.mm.dd yy/mm/dd <BR>
	 * yy-m-dd yy_m_dd yy.m.dd yy/m/dd <BR>
	 * yy-mm-d yy_mm_d yy.mm.d yy/mm/d <BR>
	 * yy-m-d yy_m_d yy.m.d yy/m/d <BR>
	 * yymmdd
	 * 
	 * @return best weighted date
	 */
	private WeightedDate getHtmlDateL2() {
		Pattern p = null;
		Matcher m = null;
		Vector<WeightedDate> wds = new Vector<WeightedDate>();

		String yy_mm_dd = "[^\\d]([0-9]{2}[-_./][01]?[0-9][-_./][0123]?[0-9])[^\\d]";
		p = Pattern.compile(yy_mm_dd);
		m = p.matcher(this.html);
		while (m.find()) {
			if(this.isInScript(m.start())){
				continue;
			}
			if(m.group(1).split("-").length != 3 && m.group(1).split("_").length != 3 && m.group(1).split("/").length != 3 && m.group(1).split("\\.").length != 3){
				continue;
			}
			WeightedDate wd = new WeightedDate();
			String[] subStrings = m.group(1).split("[-_./]");
			wd.setDate(subStrings[0], Integer.parseInt(subStrings[1]), Integer.parseInt(subStrings[2]));
			wd.setWeight(m.start(), this.hPosition, Weight.yy_mm_dd);
			wds.add(wd);
		}
		return this.getBest(wds);
	}

	/**
	 * get best level 3 date <BR>
	 * <BR>
	 * Date format: <BR>
	 * mm-dd mm_dd mm.dd mm/dd <BR>
	 * m-dd m_dd m.dd m/dd <BR>
	 * mm-d mm_d mm.d mm/d <BR>
	 * m-d m_d m.d m/d <BR>
	 * <BR>
	 * 
	 * @return best level 3 date with weight
	 */
	private WeightedDate getHtmlDateL3() {
		Pattern p = null;
		Matcher m = null;
		Vector<WeightedDate> wds = new Vector<WeightedDate>();
		String mm_dd = "(?<!\\d)[01]?[0-9][-_/][0123]?[0-9]";
		p = Pattern.compile(mm_dd);
		m = p.matcher(this.html);
		while (m.find()) {
			if(this.isInScript(m.start())){
				continue;
			}
			WeightedDate wd = new WeightedDate();
			String[] subStrings = m.group().split("[-_/]");
			wd.setDate(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)), Integer.parseInt(subStrings[0]), Integer
					.parseInt(subStrings[1]));
			wd.setWeight(m.start(), this.hPosition, Weight.mm_dd);
			wds.add(wd);
		}
		return this.getBest(wds);
	}

	/**
	 * get best level 4 date <BR>
	 * <BR>
	 * Date format: <BR>
	 * yyyy-mm yyyy.mm yyyy/mm yyyy_mm <BR>
	 * yyyy-m yyyy.m yyyy/m yyyy_m <BR>
	 * <BR>
	 * 
	 * @return best level 4 date with weight
	 */
	private WeightedDate getHtmlDateL4() {
		Pattern p = null;
		Matcher m = null;
		Vector<WeightedDate> wds = new Vector<WeightedDate>();
		String yyyy_mm = "[^\\d]([12][09][0-9]{2}[-_./][01]?[0-9])[^\\d]";
		p = Pattern.compile(yyyy_mm);
		m = p.matcher(this.html);
		while (m.find()) {
			if(this.isInScript(m.start())){
				continue;
			}
			WeightedDate wd = new WeightedDate();
			String[] subStrings = m.group(1).split("[-_./]");
			wd.setDate(subStrings[0], Integer.parseInt(subStrings[1]), 1);
			wd.setWeight(m.start(), this.hPosition, Weight.yyyy_mm);
			wds.add(wd);
		}
		return this.getBest(wds);
	}

	/**
	 * get level 1 date form chinese html <BR>
	 * <BR>
	 * Date fromat: <BR>
	 * <P>
	 * yyyy年mm月dd日 yyyy年m月dd日 yyyy年mm月d日 yyyy年m月d日 <BR>
	 * [汉语数字]年[汉语数字]月[汉语数字]日 <BR>
	 * </P>
	 * 
	 * @return best level 1 date with weight
	 */
	private WeightedDate getHtmlDateChL1() {
		Pattern p = null;
		Matcher m = null;
		Vector<WeightedDate> wds = new Vector<WeightedDate>();


		String yyyy_mm_dd_hh_mm = "[12][09][0-9]{2}年[01]?[0-9]月[0123]?[0-9]日\\s+(?:[01]?[0-9]|2[0-4])[:：](?:[0-5]?[0-9])([:：][0-9]{1,2}){0,1}";
		p = Pattern.compile(yyyy_mm_dd_hh_mm);
		m = p.matcher(this.html);
		while (m.find()) {
			String[] subStrings = m.group().replaceAll("\\s", "").split("[年月日:：]");
			if(this.isInScript(m.start()) || Integer.parseInt(subStrings[0]) < 1990){
				continue;
			}
			WeightedDate wd = new WeightedDate();
			try {
				if (subStrings.length == 5) {
					wd.setDate(subStrings[0], Integer.parseInt(subStrings[1]),
							Integer.parseInt(subStrings[2]), Integer
									.parseInt(subStrings[3]), Integer
									.parseInt(subStrings[4]), 0);
				} else {
					wd.setDate(subStrings[0], Integer.parseInt(subStrings[1]),
							Integer.parseInt(subStrings[2]), Integer
									.parseInt(subStrings[3]), Integer
									.parseInt(subStrings[4]), Integer
									.parseInt(subStrings[5]));
				}
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
			wd.setWeight(m.start(), this.hPosition, Weight.yyyy_mm_dd_hh_MM);
			wds.add(wd);
		}
		
		String yyyy_mm_dd = "[12][09][0-9]{2}年[01]?[0-9]月[0123]?[0-9]日";
		p = Pattern.compile(yyyy_mm_dd);
		m = p.matcher(this.html);
		while (m.find()) {
			String[] subStrings = m.group().split("[年月日]");
			if(this.isInScript(m.start()) || Integer.parseInt(subStrings[0]) < 1990){
				continue;
			}
			WeightedDate wd = new WeightedDate();
			wd.setDate(subStrings[0], Integer.parseInt(subStrings[1]), Integer
					.parseInt(subStrings[2]));
			wd.setWeight(m.start(), this.hPosition, Weight.yyyy_mm_dd);
			wds.add(wd);
		}

		yyyy_mm_dd = "[１２][0０９][0０１２３４５６７８９]{2}年[0０１]?[0０１２３４５６７８９]月[0０１２３]?[0０１２３４５６７８９]日";
		p = Pattern.compile(yyyy_mm_dd);
		m = p.matcher(this.html);
		while (m.find()) {
			String[] subStrings = this.replaceUpperCase(m.group()).split("[年月日]");
			if(this.isInScript(m.start()) || Integer.parseInt(subStrings[0]) < 1990){
				continue;
			}
			WeightedDate wd = new WeightedDate();
			wd.setDate(subStrings[0], Integer.parseInt(subStrings[1]), Integer
					.parseInt(subStrings[2]));
			wd.setWeight(m.start(), this.hPosition, Weight.yy_mm_dd);
			wds.add(wd);
		}

		yyyy_mm_dd = "([0〇零一二三四五六七八九十两千]{4})年[零0〇一二三四五六七八九十]{1,2}月[0〇零一二三四五六七八九十]{1,3}日";
		p = Pattern.compile(yyyy_mm_dd);
		m = p.matcher(this.html);
		while (m.find()) {
			String[] subStrings = this.replaceChinese(m.group()).split("[年月日]");
			if(this.isInScript(m.start()) || Integer.parseInt(subStrings[0]) < 1990){
				continue;
			}
			WeightedDate wd = new WeightedDate();
			wd.setDate(subStrings[0], Integer.parseInt(subStrings[1]), Integer
					.parseInt(subStrings[2]));
			wd.setWeight(m.start(), this.hPosition, Weight.yy_mm_dd_CN);
			wds.add(wd);
		}

		return this.getBest(wds);
	}

	/**
	 * get level 2 date from chinese html<BR>
	 * <P>
	 * Date format: <BR>
	 * yy年mm月dd日 yy年m月dd日 yy年mm月d日 yy年m月d日 <BR>
	 * [汉语数字]年[汉语数字]月[汉语数字]日 <BR>
	 * </P>
	 * 
	 * @return best level 1 date with weight
	 */
	private WeightedDate getHtmlDateChL2() {
		Pattern p = null;
		Matcher m = null;
		Vector<WeightedDate> wds = new Vector<WeightedDate>();

		String yy_mm_dd = "[0-9]{2}年[01]?[0-9]月[0123]?[0-9]日";
		p = Pattern.compile(yy_mm_dd);
		m = p.matcher(this.html);
		while (m.find()) {
			WeightedDate wd = new WeightedDate();
			String[] subStrings = m.group().split("[年月日]");
			wd.setDate(subStrings[0], Integer
					.parseInt(subStrings[1]), Integer
					.parseInt(subStrings[2]));
			wd.setWeight(m.start(), this.hPosition, Weight.yy_mm_dd);
			wds.add(wd);
		}

		yy_mm_dd = "[0０１２３４５６７８９]{2}年[0０１]?[0０１２３４５６７８９]月[0０１２３]?[0０１２３４５６７８９]日";
		p = Pattern.compile(yy_mm_dd);
		m = p.matcher(this.html);
		while (m.find()) {
			WeightedDate wd = new WeightedDate();
			String[] subStrings = this.replaceUpperCase(m.group()).split(
					"[年月日]");
			wd.setDate(subStrings[0], Integer
					.parseInt(subStrings[1]), Integer
					.parseInt(subStrings[2]));
			wd.setWeight(m.start(), this.hPosition, Weight.yy_mm_dd);
			wds.add(wd);
		}

		yy_mm_dd = "[0〇零一二三四五六七八九十两千]{2}年[0零〇一二三四五六七八九十]{1,2}月[0〇零一二三四五六七八九十]{1,3}日";
		p = Pattern.compile(yy_mm_dd);
		m = p.matcher(this.html);
		while (m.find()) {
			WeightedDate wd = new WeightedDate();
			String[] subStrings = this.replaceChinese(m.group()).split("[年月日]");
			if (subStrings[0].length() == 4) {
				wd.setDate(subStrings[0], Integer.parseInt(subStrings[1]),
						Integer.parseInt(subStrings[2]));
			} else if (subStrings[0].length() == 2) {
				wd.setDate(subStrings[0], Integer
						.parseInt(subStrings[1]), Integer
						.parseInt(subStrings[2]));
			}
			wd.setWeight(m.start(), this.hPosition, Weight.yy_mm_dd);
			wds.add(wd);
		}
		return this.getBest(wds);
	}

	/**
	 * get level 3 date from chinese html <BR>
	 * <P>
	 * Date format: <BR>
	 * mm月dd日 m月dd日 mm月d日 m月m日 <BR>
	 * [汉语数字]月[汉语数字]日 <BR>
	 * </P>
	 * 
	 * @return level 3 date with weight
	 */
	private WeightedDate getHtmlDateChL3() {
		Pattern p = null;
		Matcher m = null;
		Vector<WeightedDate> wds = new Vector<WeightedDate>();

		String mm_dd = "[01]?[0-9]月[0123]?[0-9]日";
		p = Pattern.compile(mm_dd);
		m = p.matcher(this.html);
		while (m.find()) {
			if(this.isInScript(m.start())){
				continue;
			}
			WeightedDate wd = new WeightedDate();
			String[] subStrings = m.group().split("[月日]");
			wd.setDate(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)), Integer.parseInt(subStrings[0]), Integer
					.parseInt(subStrings[1]));
			wd.setWeight(m.start(), this.hPosition, Weight.mm_dd);
			wds.add(wd);
		}

		mm_dd = "[0０１]?[0０１２３４５６７８９]月[0０１２３]?[0０１２３４５６７８９]日";
		p = Pattern.compile(mm_dd);
		m = p.matcher(this.html);
		while (m.find()) {
			if(this.isInScript(m.start())){
				continue;
			}
			WeightedDate wd = new WeightedDate();
			String[] subStrings = this.replaceUpperCase(m.group())
					.split("[月日]");
			wd.setDate(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)), Integer.parseInt(subStrings[0]), Integer
					.parseInt(subStrings[1]));

			wd.setWeight(m.start(), this.hPosition, Weight.mm_dd);
			wds.add(wd);
		}

		mm_dd = "[零0〇一二三四五六七八九十]{1,2}月[0〇零一二三四五六七八九十]{1,3}日";
		p = Pattern.compile(mm_dd);
		m = p.matcher(this.html);
		while (m.find()) {
			if(this.isInScript(m.start())){
				continue;
			}
			WeightedDate wd = new WeightedDate();
			String[] subStrings = this.replaceChinese(m.group()).split("[月日]");
			wd.setDate(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)), Integer.parseInt(subStrings[0]), Integer
					.parseInt(subStrings[1]));

			wd.setWeight(m.start(), this.hPosition, Weight.mm_dd);
			wds.add(wd);
		}
		return this.getBest(wds);
	}

	/**
	 * get level 4 date from chinese html <BR>
	 * <P>
	 * Date format: <BR>
	 * yyyy年mm月 yyyy年m月 <BR>
	 * [汉语数字]年[汉语数字]月 <BR>
	 * </P>
	 * 
	 * @return level 4 date with weight
	 */
	private WeightedDate getHtmlDateChL4() {
		Pattern p = null;
		Matcher m = null;
		Vector<WeightedDate> wds = new Vector<WeightedDate>();

		String yyyy_mm = "[12][09][0-9]{2}年[01]?[0-9]月";
		p = Pattern.compile(yyyy_mm);
		m = p.matcher(this.html);
		while (m.find()) {
			if(this.isInScript(m.start())){
				continue;
			}
			WeightedDate wd = new WeightedDate();
			String[] subStrings = m.group().split("[年月]");
			wd.setDate(subStrings[0], Integer.parseInt(subStrings[1]), 1);
			wd.setWeight(m.start(), this.hPosition, Weight.yyyy_mm);
			wds.add(wd);
		}

		yyyy_mm = "[１２][0０９][0０１２３４５６７８９]{2}年[0０１]?[0０１２３４５６７８９]月";
		p = Pattern.compile(yyyy_mm);
		m = p.matcher(this.html);
		while (m.find()) {
			if(this.isInScript(m.start())){
				continue;
			}
			WeightedDate wd = new WeightedDate();
			String[] subStrings = this.replaceUpperCase(m.group())
					.split("[年月]");
			wd.setDate(subStrings[0], Integer.parseInt(subStrings[1]), 1);
			wd.setWeight(m.start(), this.hPosition, Weight.yyyy_mm);
			wds.add(wd);
		}

		yyyy_mm = "([0〇零一二三四五六七八九十两千]{4})年[零0〇一二三四五六七八九十]{1,2}月";
		p = Pattern.compile(yyyy_mm);
		m = p.matcher(this.html);
		while (m.find()) {
			if(this.isInScript(m.start())){
				continue;
			}
			WeightedDate wd = new WeightedDate();
			String[] subStrings = this.replaceChinese(m.group()).split("[年月]");
			wd.setDate(subStrings[0], Integer.parseInt(subStrings[1]), 1);
			wd.setWeight(m.start(), this.hPosition, Weight.yyyy_mm);
			wds.add(wd);
		}
		return this.getBest(wds);
	}

	/**
	 * get level 5 date from chinese html <BR>
	 * <P>
	 * Date format: <BR>
	 * yyyy年 [汉语数字]年 <BR>
	 * </P>
	 * 
	 * @return level 5 date with weight
	 */
	private WeightedDate getHtmlDateChL5() {
		Pattern p = null;
		Matcher m = null;
		Vector<WeightedDate> wds = new Vector<WeightedDate>();

		String yyyy = "[12][09][0-9]{2}年";
		p = Pattern.compile(yyyy);
		m = p.matcher(this.html);
		while (m.find()) {
			if(this.isInScript(m.start())){
				continue;
			}
			WeightedDate wd = new WeightedDate();
			wd.setDate(m.group().substring(0, 4), 1, 1);
			wd.setWeight(m.start(), this.hPosition, Weight.yyyy);
			wds.add(wd);
		}

		yyyy = "[１２][0０９][0０１２３４５６７８９]{2}年";
		p = Pattern.compile(yyyy);
		m = p.matcher(this.html);
		while (m.find()) {
			if(this.isInScript(m.start())){
				continue;
			}
			WeightedDate wd = new WeightedDate();
			String year = this.replaceUpperCase(m.group()).substring(0, 4);
			wd.setDate(year, 1, 1);
			wd.setWeight(m.start(), this.hPosition, Weight.yyyy);
			wds.add(wd);
		}

		yyyy = "([0〇零一二三四五六七八九十两千]{4})年";
		p = Pattern.compile(yyyy);
		m = p.matcher(this.html);
		while (m.find()) {
			if(this.isInScript(m.start())){
				continue;
			}
			WeightedDate wd = new WeightedDate();
			String year = this.replaceChinese(m.group()).substring(0, 4);
			wd.setDate(year, 1, 1);
			wd.setWeight(m.start(), this.hPosition, Weight.yyyy);
			wds.add(wd);
		}
		return this.getBest(wds);
	}
	
	/**
	 * get level 1 date from english html <BR>
	 * 
	 * @return level 1 date with weight
	 */
	private WeightedDate getHtmlDateEnL1() {
		Pattern p = null;
		Matcher m = null;
		this.hPosition -= 500;//针对英文新闻时间在标题前面情况
		Vector<WeightedDate> wds = new Vector<WeightedDate>();

		String hh_MM_mm_dd_yyyy = "(?:[01]?[0-9]|2[0-3]):[0-5][0-9]\\s*,?(Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Oct|Nov|Dec|January|February|March|April|May|June|July|August|September|October|November|December)\\s{0,5}\\d{1,2}(th|st|nd|rd){0,1},?\\s{0,5}\\d{4}";
		p = Pattern.compile(hh_MM_mm_dd_yyyy, Pattern.CASE_INSENSITIVE);
		m = p.matcher(this.html);
		while (m.find()) {
			WeightedDate wd = new WeightedDate();
			String date = m.group().replaceAll("[,:]", " ");
			String[] subStrings = date.split("\\s+");
			if (subStrings.length != 5) {
				continue;
			}
			int month = this.getEnMonth(subStrings[2]);
			if (month == -1) {
				continue;
			}
			if(this.isInScript(m.start()) || Integer.parseInt(subStrings[4]) < 1990){
				continue;
			}
			String dStr = subStrings[3].toLowerCase().replace("st", "")
					.replace("nd", "").replace("rd", "").replace("th", "");
			wd.setDate(subStrings[4], month, Integer.parseInt(dStr), Integer.parseInt(subStrings[0]), Integer.parseInt(subStrings[1]), 0);
			wd.setWeight(m.start(), this.hPosition, Weight.mm_dd_yyyy);
			wds.add(wd);
		}
		
		String mm_dd_yyyy = "(Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Oct|Nov|Dec|January|February|March|April|May|June|July|August|September|October|November|December)\\s{0,5}\\d{1,2}(th|st|nd|rd){0,1},{0,1}\\s{0,5}\\d{4}";
		p = Pattern.compile(mm_dd_yyyy, Pattern.CASE_INSENSITIVE);
		m = p.matcher(this.html);
		while (m.find()) {
			WeightedDate wd = new WeightedDate();
			String date = m.group().replace(",", " ");
			String[] subStrings = date.split("\\s+");
			if (subStrings.length != 3) {
				continue;
			}
			int month = this.getEnMonth(subStrings[0]);
			if (month == -1) {
				continue;
			}
			if(this.isInScript(m.start()) || Integer.parseInt(subStrings[2]) < 1990){
				continue;
			}
			String dStr = subStrings[1].toLowerCase().replace("st", "")
					.replace("nd", "").replace("rd", "").replace("th", "");
			wd.setDate(subStrings[2], month, Integer.parseInt(dStr));
			wd.setWeight(m.start(), this.hPosition, Weight.mm_dd_yyyy);
			wds.add(wd);
		}

		String dd_mm_yyyy = "\\d{1,2}(th|st|nd|rd)?\\s{0,5}(Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Oct|Nov|Dec|January|February|March|April|May|June|July|August|September|October|November|December),{0,1},?\\s{0,5}\\d{4}";
		p = Pattern.compile(dd_mm_yyyy, Pattern.CASE_INSENSITIVE);
		m = p.matcher(this.html);
		while (m.find()) {
			WeightedDate wd = new WeightedDate();
			String date = m.group().replace(",", " ");
			String[] subStrings = date.split("\\s+");
			if (subStrings.length != 3) {
				continue;
			}
			String dStr = subStrings[0].toLowerCase().replace("st", "")
					.replace("nd", "").replace("rd", "").replace("th", "");
			int month = this.getEnMonth(subStrings[1]);
			if (month == -1) {
				continue;
			}
			if(this.isInScript(m.start()) || Integer.parseInt(subStrings[2]) < 1990){
				continue;
			}
			wd.setDate(subStrings[2], month, Integer.parseInt(dStr));
			wd.setWeight(m.start(), this.hPosition, Weight.mm_dd_yyyy);
			wds.add(wd);
		}
		
		String dd_mm_yy = "(?<!\\d)\\d{1,2}(th|st|nd|rd)?,?-(Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Oct|Nov|Dec|January|February|March|April|May|June|July|August|September|October|November|December),{0,1}-\\d{2,4}";
		p = Pattern.compile(dd_mm_yy, Pattern.CASE_INSENSITIVE);
		m = p.matcher(this.html);
		while (m.find()) {
			WeightedDate wd = new WeightedDate();
			String date = m.group().replace(",", " ");
			String[] subStrings = date.split("\\s+|-");
			if (subStrings.length != 3) {
				continue;
			}
			String dStr = subStrings[0].toLowerCase().replace("st", "")
					.replace("nd", "").replace("rd", "").replace("th", "");
			int month = this.getEnMonth(subStrings[1]);
			if (month == -1) {
				continue;
			}
			if(this.isInScript(m.start()) || Integer.parseInt(subStrings[2]) < 1990){
				continue;
			}
			wd.setDate(subStrings[2], month, Integer.parseInt(dStr));
			wd.setWeight(m.start(), this.hPosition, Weight.yy_mm_dd);
			wds.add(wd);
		}
		
		String dd_mm_yy2 = "(?<!\\d)\\d{1,2}(th|st|nd|rd)?\\s{0,5}(Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Oct|Nov|Dec|January|February|March|April|May|June|July|August|September|October|November|December),{0,1},?\\s{0,5}\\d{2}";
		p = Pattern.compile(dd_mm_yy2, Pattern.CASE_INSENSITIVE);
		m = p.matcher(this.html);
		while (m.find()) {
			WeightedDate wd = new WeightedDate();
			String date = m.group().replace(",", " ");
			String[] subStrings = date.split("\\s+|-");
			if (subStrings.length != 3) {
				continue;
			}
			String dStr = subStrings[0].toLowerCase().replace("st", "")
					.replace("nd", "").replace("rd", "").replace("th", "");
			int month = this.getEnMonth(subStrings[1]);
			if (month == -1) {
				continue;
			}
			if(this.isInScript(m.start()) || Integer.parseInt(subStrings[2]) < 1990){
				continue;
			}
			wd.setDate(subStrings[2], month, Integer.parseInt(dStr));
			wd.setWeight(m.start(), this.hPosition, Weight.mm_dd_yyyy);
			wds.add(wd);
		}
		
		String mm_dd_hh_MM = "(Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Oct|Nov|Dec|January|February|March|April|May|June|July|August|September|October|November|December)\\s{0,5}\\d{1,2}(th|st|nd|rd){0,1}\\s*,?\\s*[01]?[0-9]:[0-5]?[0-9]\\s+(AM|PM)?";
		p = Pattern.compile(mm_dd_hh_MM, Pattern.CASE_INSENSITIVE);
		m = p.matcher(this.html);
		while (m.find()) {
			WeightedDate wd = new WeightedDate();
			String date = m.group().replaceAll("[,:]", " ").replaceAll("\\s{2,}", " ");
			String[] subStrings = date.split("\\s+");
			if (subStrings.length < 4) {
				continue;
			}
			int month = this.getEnMonth(subStrings[0]);
			if (month == -1) {
				continue;
			}
			if(this.isInScript(m.start())){
				continue;
			}
			int hour = Integer.parseInt(subStrings[2]);
			if(subStrings.length == 5){
				if(subStrings[4].equals("PM") && hour < 12){
					hour += 12;
				}
			}
			String dStr = subStrings[1].toLowerCase().replace("st", "")
					.replace("nd", "").replace("rd", "").replace("th", "");
			wd.setDate(Integer.toString(Calendar.getInstance().get(Calendar.YEAR)), month, 
					Integer.parseInt(dStr), hour, Integer.parseInt(subStrings[3]), 0);
			wd.setWeight(m.start(), this.hPosition, Weight.mm_dd_hh_MM);
			wds.add(wd);
		}
		
		String yyyy_mm_dd = "\\d{4}\\s*,?(Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Oct|Nov|Dec|January|February|March|April|May|June|July|August|September|October|November|December)\\s{0,5}\\d{1,2}(th|st|nd|rd){0,1},?";
		p = Pattern.compile(yyyy_mm_dd, Pattern.CASE_INSENSITIVE);
		m = p.matcher(this.html);
		while (m.find()) {
			WeightedDate wd = new WeightedDate();
			String date = m.group().replaceAll(",", " ");
			String[] subStrings = date.split("\\s+,");
			if(subStrings.length < 3){
				continue;
			}
			int month = this.getEnMonth(subStrings[1]);
			if (month == -1) {
				continue;
			}
			if(this.isInScript(m.start()) || Integer.parseInt(subStrings[0]) < 1990){
				continue;
			}
			String dStr = subStrings[2].toLowerCase().replace("st", "")
					.replace("nd", "").replace("rd", "").replace("th", "");
			wd.setDate(subStrings[0], month, Integer.parseInt(dStr));
			wd.setWeight(m.start(), this.hPosition, Weight.yyyy_mm_dd);
			wds.add(wd);
		}

		this.hPosition += 500;
		
		return this.getBest(wds);
	}

	/**
	 * replace Chinese number with Arabin number
	 * 
	 * @param text
	 * @return
	 */
	private String replaceChinese(String text) {
		if (text.indexOf("两千零") >= 0)
			text = text.replace("两千零", "200");
		if (text.indexOf("二千零") >= 0)
			text = text.replace("二千零", "200");
		if (text.indexOf("二千") >= 0)
			text = text.replace("二千", "2000");
		if (text.indexOf("两千") >= 0)
			text = text.replace("两千", "2000");
		if (text.indexOf("二十一") >= 0)
			text = text.replace("二十一", "21");
		if (text.indexOf("二十二") >= 0)
			text = text.replace("二十二", "22");
		if (text.indexOf("二十三") >= 0)
			text = text.replace("二十三", "23");
		if (text.indexOf("二十四") >= 0)
			text = text.replace("二十四", "24");
		if (text.indexOf("二十五") >= 0)
			text = text.replace("二十五", "25");
		if (text.indexOf("二十六") >= 0)
			text = text.replace("二十六", "26");
		if (text.indexOf("二十七") >= 0)
			text = text.replace("二十七", "27");
		if (text.indexOf("二十八") >= 0)
			text = text.replace("二十八", "28");
		if (text.indexOf("二十九") >= 0)
			text = text.replace("二十九", "29");
		if (text.indexOf("三十一") >= 0)
			text = text.replace("三十一", "31");
		if (text.indexOf("十一") >= 0)
			text = text.replace("十一", "11");
		if (text.indexOf("十二") >= 0)
			text = text.replace("十二", "12");
		if (text.indexOf("十三") >= 0)
			text = text.replace("十三", "13");
		if (text.indexOf("十四") >= 0)
			text = text.replace("十四", "14");
		if (text.indexOf("十五") >= 0)
			text = text.replace("十五", "15");
		if (text.indexOf("十六") >= 0)
			text = text.replace("十六", "16");
		if (text.indexOf("十七") >= 0)
			text = text.replace("十七", "17");
		if (text.indexOf("十八") >= 0)
			text = text.replace("十八", "18");
		if (text.indexOf("十九") >= 0)
			text = text.replace("十九", "19");
		if (text.indexOf("三十") >= 0)
			text = text.replace("三十", "30");
		if (text.indexOf("二十") >= 0)
			text = text.replace("二十", "20");
		if (text.indexOf("十") >= 0)
			text = text.replace("十", "10");
		if (text.indexOf("九") >= 0)
			text = text.replace("九", "9");
		if (text.indexOf("八") >= 0)
			text = text.replace("八", "8");
		if (text.indexOf("七") >= 0)
			text = text.replace("七", "7");
		if (text.indexOf("六") >= 0)
			text = text.replace("六", "6");
		if (text.indexOf("五") >= 0)
			text = text.replace("五", "5");
		if (text.indexOf("四") >= 0)
			text = text.replace("四", "4");
		if (text.indexOf("三") >= 0)
			text = text.replace("三", "3");
		if (text.indexOf("二") >= 0)
			text = text.replace("二", "2");
		if (text.indexOf("一") >= 0)
			text = text.replace("一", "1");
		if (text.indexOf("零") >= 0)
			text = text.replace("零", "0");
		if (text.indexOf("〇") >= 0)
			text = text.replace("〇", "0");

		return text;
	}

	private String replaceUpperCase(String text) {
		if (text.indexOf("０") >= 0)
			text = text.replace("０", "0");
		if (text.indexOf("１") >= 0)
			text = text.replace("１", "1");
		if (text.indexOf("２") >= 0)
			text = text.replace("２", "2");
		if (text.indexOf("３") >= 0)
			text = text.replace("３", "3");
		if (text.indexOf("４") >= 0)
			text = text.replace("４", "4");
		if (text.indexOf("６") >= 0)
			text = text.replace("６", "6");
		if (text.indexOf("７") >= 0)
			text = text.replace("７", "7");
		if (text.indexOf("８") >= 0)
			text = text.replace("８", "8");
		if (text.indexOf("９") >= 0)
			text = text.replace("９", "9");
		return text;
	}

	/**
	 * get english month <BR>
	 * 
	 * Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Oct|Nov|Dec <BR>
	 * January|February|March|April|May|June|July|August|September|October|November|December
	 * <BR>
	 * 
	 * @return int month | 0 if can't match
	 */
	private int getEnMonth(String monthStr) {
		String lMonth = monthStr.toLowerCase();
		if (lMonth.compareTo("january") == 0) {
			return 1;
		}
		if (lMonth.compareTo("february") == 0) {
			return 2;
		}
		if (lMonth.compareTo("march") == 0) {
			return 3;
		}
		if (lMonth.compareTo("april") == 0) {
			return 4;
		}
		if (lMonth.compareTo("may") == 0) {
			return 5;
		}
		if (lMonth.compareTo("june") == 0) {
			return 6;
		}
		if (lMonth.compareTo("july") == 0) {
			return 7;
		}
		if (lMonth.compareTo("august") == 0) {
			return 8;
		}
		if (lMonth.compareTo("september") == 0) {
			return 9;
		}
		if (lMonth.compareTo("october") == 0) {
			return 10;
		}
		if (lMonth.compareTo("november") == 0) {
			return 11;
		}
		if (lMonth.compareTo("december") == 0) {
			return 12;
		}

		// Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Oct|Nov|Dec|
		if (lMonth.indexOf("jan") >= 0) {
			return 1;
		}
		if (lMonth.indexOf("feb") >= 0) {
			return 2;
		}
		if (lMonth.indexOf("mar") >= 0) {
			return 3;
		}
		if (lMonth.indexOf("apr") >= 0) {
			return 4;
		}
		if (lMonth.indexOf("may") >= 0) {
			return 5;
		}
		if (lMonth.indexOf("jun") >= 0) {
			return 6;
		}
		if (lMonth.indexOf("jul") >= 0) {
			return 7;
		}
		if (lMonth.indexOf("aug") >= 0) {
			return 8;
		}
		if (lMonth.indexOf("sep") >= 0) {
			return 9;
		}
		if (lMonth.indexOf("oct") >= 0) {
			return 10;
		}
		if (lMonth.indexOf("nov") >= 0) {
			return 11;
		}

		if (lMonth.indexOf("dec") >= 0) {
			return 12;
		}

		return -1;
	}
	

}

/**
 * weight of each date format
 * 
 * @author Ahui Wang
 * 
 */
class Weight {

	public static int yyyy_mm_dd_hh_MM = 0;
	
	public static int yyyy_mm_dd = 10;

	public static int mm_dd_yyyy = 50;

	public static int yyyymmdd = 5000;

	public static int yy_mm_dd = 500;

	public static int yy_mm_dd_CN = 10000;

	public static int yymmdd = 10000;

	public static int mm_dd_hh_MM = 10;
	
	public static int mm_dd = 100000;

	public static int yyyy_mm = 20000;

	public static int yyyy = 200000;

}