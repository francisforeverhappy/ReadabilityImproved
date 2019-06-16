package testExample;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Extractor {
	//载入文件
	public static Document getDocument(String news){
		try{
			//Document doc = Jsoup.parse(new URL(news).openStream(), "UTF-8", news);
			Document doc = (Document) Jsoup.connect(news).get();
			return doc;
		}catch(IOException e){
			e.printStackTrace();
		}		
		return null;
	}
	
	public static String getTitle(Document doc){
		/*
		 * 第一种方法： 基于标签title and h1
		 * TODO: 第二种方法： 最长公共子串算法
		 */
		// 获取 title and h1 tag
		Elements titleTag = doc.getElementsByTag("title");
		Elements H1Tag = doc.getElementsByTag("H1");
		
		String title_text = null;
		if(!titleTag.isEmpty() && !H1Tag.isEmpty()){//get title from tag title and h1
			title_text= titleTag.first().text().trim();
			// 按照字符串长度从大到小排序
			Collections.sort(H1Tag, new Comparator<Element>(){			
				@Override
				public int compare(Element o1, Element o2) {
					if(o1.text().length() > o2.text().length()){
						return -1;
					}else if(o1.text().length() < o2.text().length()){
						return 1;
					}else{
						return 0;
					}
				}			
			});
			for(Element h1tag : H1Tag){
				// 消除
				// 判断h1 标签是否和title标签有关： 相等，子集？
				if(h1tag.text().length() == 0){
					continue;
				}
				String h1text = h1tag.text().trim();
				if(h1text.equals(title_text) || 
						h1text.equals(title_text.substring(0, h1text.length()-1)) || 
						h1text.indexOf(title_text) != -1){
					return h1tag.text();
				}
			}
			// 此时H1标签与title无关
			System.out.println("title与  h1无关： "+H1Tag.html());
		}
		else if(H1Tag.isEmpty() && !titleTag.isEmpty()){//没有H1标签
			title_text= titleTag.first().text();
			System.out.println("No h1 tag: " + title_text);
			//TODO： 应使用最长公共子串方法
			return doc.title();
		}else if(titleTag.isEmpty() && !H1Tag.isEmpty()){//没有 title tag
			System.out.println("no title tag!");
		}else{//两种标签都没有
			//TODO: 可以根据keywords， 字符长度来思考
			System.out.println("no title and h1 tag!");
		}
		return null;
	}
	public static void main(String[] args){
		List<String> fileurl = null;
		try {
			fileurl = FileUtils.readLines(new File("test/url_image_test.txt"),"UTF-8");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(String news: fileurl){
			Document doc = getDocument(news);
			if(doc != null){
				System.out.println("succeed to get doc " + doc.title());
			}else{
				System.out.println("failed to get doc");
			}
			String title = getTitle(doc);
			if(title != null){
				System.out.println("title: " + title);
			}
			Document newDoc = doc.clone();
//			try {
//				File newsFile = new File("test/newsHTML.txt");
//				FileUtils.writeStringToFile(newsFile, newDoc.html(), false);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
			//System.out.println("未处理前html: " + newDoc.html());
			ReadabilityForImg imgRead = new ReadabilityForImg(newDoc);
			imgRead.init();
//			try {
//				File newsFile = new File("test/newsHTML.txt");
//				FileUtils.writeStringToFile(newsFile, newDoc.html(), false);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
			System.out.println("\n*****************************************************\n");
		}
	}
}
