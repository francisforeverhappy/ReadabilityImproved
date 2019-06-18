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
		
		Elements titleTag = doc.getElementsByTag("title");
		Elements H1Tag = doc.getElementsByTag("H1");
		
		String title_text = null;
		if(!titleTag.isEmpty() && !H1Tag.isEmpty()){//get title from tag title and h1
			title_text= titleTag.first().text().trim();
			
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
				
				if(h1tag.text().length() == 0){
					continue;
				}
				String h1text = h1tag.text().trim();
				/*
				 * 有bug, http://fashion.huanqiu.com/pic/2019-06/2935738.html
				 */
				if(h1text.equals(title_text) || 
						h1text.equals(title_text.substring(0, h1text.length()-1)) || 
						h1text.indexOf(title_text) != -1){
					return h1tag.text();
				}
			}
			//System.out.println("title��  h1�޹أ� "+H1Tag.html());
		}
		else if(H1Tag.isEmpty() && !titleTag.isEmpty()){//û��H1��ǩ
			title_text= titleTag.first().text();
			//System.out.println("No h1 tag: " + title_text);
			
			return doc.title();
		}else if(titleTag.isEmpty() && !H1Tag.isEmpty()){//û�� title tag
			System.out.println("no title tag!");
		}else{
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
				System.out.println("succeed to get doc " + doc.title() + " ;\nurl: "+ news);
			}else{
				System.out.println("failed to get doc");
			}
//			String title = getTitle(doc);
//			if(title != null){
//				System.out.println("title: " + title);
//			}
			Document newDoc = doc.clone();
//			try {
//				File newsFile = new File("test/newsHTML.txt");
//				FileUtils.writeStringToFile(newsFile, newDoc.html(), false);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
			long startTime=System.currentTimeMillis();
			ReadabilityForImg imgRead = new ReadabilityForImg(newDoc);
			imgRead.init();
			long endTime=System.currentTimeMillis();

			System.out.println("当前程序耗时："+(endTime-startTime)+"ms");
//			try {
//				File newsFile = new File("test/newsHTML.txt");
//				FileUtils.writeStringToFile(newsFile, newDoc.html(), false);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
			System.out.println("\n*****************************************************\n");
			List<String> imgList = imgRead.getImgList();
			for(String img: imgList){
				System.out.println("picture: "+img);
			}
			System.out.println("\n*****************************************************\n");
			//break;
		}
	}
}
