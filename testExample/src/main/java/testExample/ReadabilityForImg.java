package testExample;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import casia.ibasic.fusionsight.extractor.util.Utils;

public class ReadabilityForImg {

    private static final String CONTENT_SCORE  = "readabilityContentScore";
    private static final String IMG_SCORE = "readabilityImgScore";

    private final Document mDocument;
    private String mBodyCache;
    private ArrayList<String> pictext = new ArrayList<String>();
    
    // 构造方法
    public ReadabilityForImg(String html) {
        super();
        mDocument = Jsoup.parse(html);
    }

    public ReadabilityForImg(String html, String baseUri) {
        super();
        mDocument = Jsoup.parse(html, baseUri);
    }

    public ReadabilityForImg(File in, String charsetName, String baseUri)
            throws IOException {
        super();
        mDocument = Jsoup.parse(in, charsetName, baseUri);
    }

    public ReadabilityForImg(URL url, int timeoutMillis) throws IOException {
        super();
        mDocument = Jsoup.parse(url, timeoutMillis);
    }

    public ReadabilityForImg(Document doc) {
        super();
        mDocument = doc;
    }
    
    /*
     * 获取图片列表
     */
    public List<String> getImgList(){
    	return pictext;
    }
    
    /*
     * 方案：抽取正文图片
     * 方案一： 先对正文评分，然后在此基础上对图片评分
     * 
     */

    // @formatter:off
    /**
     * Runs readability.
     * 
     * Workflow: 
     * 1. Prep the document by removing script tags, css, etc. 
     * 2. Build readability's DOM tree. 
     * 3. Grab the article content from the current dom tree. 
     * 4. Replace the current DOM tree with the new one. 
     * 5. Read peacefully.
     * 
     * @param preserveUnlikelyCandidates
     */
    // @formatter:on
    private void init(boolean preserveUnlikelyCandidates) {
        if (mDocument.body() != null && mBodyCache == null) {
            mBodyCache = mDocument.body().html();
        }

        prepDocument();

        /* Build readability's DOM tree */
        Element overlay = mDocument.createElement("div");
        Element innerDiv = mDocument.createElement("div");
        Element articleTitle = getArticleTitle();
        //System.out.println("after prepare Docement: "+mDocument.html());
        Element articleContent = grabArticle(preserveUnlikelyCandidates);
        return;
        

        /**
         * If we attempted to strip unlikely candidates on the first run
         * through, and we ended up with no content, that may mean we stripped
         * out the actual content so we couldn't parse it. So re-run init while
         * preserving unlikely candidates to have a better shot at getting our
         * content out properly.
         */
//        if (isEmpty(getInnerText(articleContent, false))) {
//            if (!preserveUnlikelyCandidates) {
//                mDocument.body().html(mBodyCache);
//                init(true);
//                return;
//            } else {
//                articleContent
//                        .html("<p>Sorry, readability was unable to parse this page for content.</p>");
//            }
//        }

        /* Glue the structure of our document together. */
//        innerDiv.appendChild(articleTitle);
//        innerDiv.appendChild(articleContent);
//        overlay.appendChild(innerDiv);
//
//        /* Clear the old HTML, insert the new content. */
//        mDocument.body().html("");
//        mDocument.body().prependChild(overlay);
    }

    /**
     * Runs readability.
     */
    public final void init() {
        init(false);
    }

    /**
     * Get the combined inner HTML of all matched elements.
     * 
     * @return
     */
    public final String html() {
        return mDocument.html();
    }

    /**
     * Get the combined outer HTML of all matched elements.
     * 
     * @return
     */
    public final String outerHtml() {
        return mDocument.outerHtml();
    }

    /**
     * Get the article title as an H1. Currently just uses document.title, we
     * might want to be smarter in the future.
     * 
     * @return
     */
    protected Element getArticleTitle() {
        Element articleTitle = mDocument.createElement("h1");
//        articleTitle.html(mDocument.title());
        articleTitle.html("");
        return articleTitle;
    }

    /**
     * Prepare the HTML document for readability to scrape it. This includes
     * things like stripping javascript, CSS, and handling terrible markup.
     */
    protected void prepDocument() {
        /**
         * In some cases a body element can't be found (if the HTML is totally
         * hosed for example) so we create a new body node and append it to the
         * document.
         */
        if (mDocument.body() == null) {
            mDocument.appendElement("body");
        }

        /* Remove all scripts */
        Elements elementsToRemove = mDocument.getElementsByTag("script");
        for (Element script : elementsToRemove) {
            script.remove();
        }

        /* Remove all stylesheets */
        elementsToRemove = getElementsByTag(mDocument.head(), "link");
        for (Element styleSheet : elementsToRemove) {
            if ("stylesheet".equalsIgnoreCase(styleSheet.attr("rel"))) {
                styleSheet.remove();
            }
        }

        /* Remove all style tags in head */
        elementsToRemove = mDocument.getElementsByTag("style");
        for (Element styleTag : elementsToRemove) {
            styleTag.remove();
        }
        
        /* TODO: Remove comments <!-- --> */
        

        /* Turn all double br's into p's */
        /*
         * TODO: this is pretty costly as far as processing goes. Maybe optimize
         * later.
         */
        mDocument.body().html(
                mDocument.body().html()
                        .replaceAll(Patterns.REGEX_REPLACE_BRS, "</p><p>")
                        .replaceAll(Patterns.REGEX_REPLACE_FONTS, "<$1span>"));
    }

    /**
     * 清理任何内联样式styles、iframes, forms, strip extraneous &lt;p&gt; tags标签等。
     * @param articleContent
     */
    private void prepArticle(Element articleContent) {
        cleanStyles(articleContent);
        killBreaks(articleContent);

        /* 清除文章内容中的垃圾  */
        clean(articleContent, "form");
        clean(articleContent, "object");
        clean(articleContent, "h1");
        
        /* 删除可能存在的副标题 H2 */
        if (getElementsByTag(articleContent, "h2").size() == 1) {
            clean(articleContent, "h2");
        }
        clean(articleContent, "iframe");

        cleanHeaders(articleContent);

        /*
         * 清除tag元素中可疑杂质节点
         */
        cleanConditionally(articleContent, "table");
        cleanConditionally(articleContent, "ul");
        cleanConditionally(articleContent, "div");

        /* 清除多余空白段落 */
        Elements articleParagraphs = getElementsByTag(articleContent, "p");
        for (Element articleParagraph : articleParagraphs) {
            int imgCount = getElementsByTag(articleParagraph, "img").size();
            int embedCount = getElementsByTag(articleParagraph, "embed").size();
            int objectCount = getElementsByTag(articleParagraph, "object")
                    .size();

            if (imgCount == 0 && embedCount == 0 && objectCount == 0
                    && isEmpty(getInnerText(articleParagraph, false))) {
                articleParagraph.remove();
            }
        }

        try {
            articleContent.html(articleContent.html().replaceAll(
                    "(?i)<br[^>]*>\\s*<p", "<p"));
        } catch (Exception e) {
            dbg("Cleaning innerHTML of breaks failed. This is an IE strict-block-elements bug. Ignoring.",
                    e);
        }
    }

    /**
     * 初始化Node元素的分数权重值,会根据class、id属性值进一步计算
     * 
     * @param node
     */
    private static void initializeNode(Element node) {
        node.attr(CONTENT_SCORE, Integer.toString(0));

        String tagName = node.tagName();
        if ("div".equalsIgnoreCase(tagName)) {
            incrementContentScore(node, 5);
        } else if ("pre".equalsIgnoreCase(tagName)
                || "td".equalsIgnoreCase(tagName)
                || "blockquote".equalsIgnoreCase(tagName)) {
            incrementContentScore(node, 3);
        } else if ("address".equalsIgnoreCase(tagName)
                || "ol".equalsIgnoreCase(tagName)
                || "ul".equalsIgnoreCase(tagName)
                || "dl".equalsIgnoreCase(tagName)
                || "dd".equalsIgnoreCase(tagName)
                || "dt".equalsIgnoreCase(tagName)
                || "li".equalsIgnoreCase(tagName)
                || "form".equalsIgnoreCase(tagName)) {
            incrementContentScore(node, -3);
        } else if ("h1".equalsIgnoreCase(tagName)
                || "h2".equalsIgnoreCase(tagName)
                || "h3".equalsIgnoreCase(tagName)
                || "h4".equalsIgnoreCase(tagName)
                || "h5".equalsIgnoreCase(tagName)
                || "h6".equalsIgnoreCase(tagName)
                || "th".equalsIgnoreCase(tagName)
                || "noscript".equalsIgnoreCase(tagName)) {
            incrementContentScore(node, -5);
        }
        incrementContentScore(node, getClassWeight(node));
    }
    /**
     * 初始化img标签父节点，祖父节点，曾祖父节点的分数
     * 评价维度：tag, style, class, id
     * 
     * @param node 
     */
    private static void initializeImgScore(Element node) {
        node.attr(IMG_SCORE, Integer.toString(0));
        /*
         * 根据标签给分，依照主流网站规律来看，一般p是img父标签，div是祖父标签
         * 根据经验测试，目前基本都要扣5分
         */
        String tagName = node.tagName();
        if ("p".equalsIgnoreCase(tagName)) {
            incrementImgScore(node, 7);
        } else if ("div".equalsIgnoreCase(tagName)
        		|| "span".equalsIgnoreCase(tagName)
        		|| "figure".equalsIgnoreCase(tagName)
        		|| "article".equalsIgnoreCase(tagName)) {
            incrementImgScore(node, 5);
        }else if ("address".equalsIgnoreCase(tagName)
                || "ol".equalsIgnoreCase(tagName)
                || "ul".equalsIgnoreCase(tagName)
                || "dl".equalsIgnoreCase(tagName)
                || "dd".equalsIgnoreCase(tagName)
                || "dt".equalsIgnoreCase(tagName)
                || "li".equalsIgnoreCase(tagName)
                || "form".equalsIgnoreCase(tagName)
                || "td".equalsIgnoreCase(tagName)
                || "blockquote".equalsIgnoreCase(tagName)
                || "pre".equalsIgnoreCase(tagName)
                || "h1".equalsIgnoreCase(tagName)
                || "h2".equalsIgnoreCase(tagName)
                || "h3".equalsIgnoreCase(tagName)
                || "h4".equalsIgnoreCase(tagName)
                || "h5".equalsIgnoreCase(tagName)
                || "h6".equalsIgnoreCase(tagName)
                || "th".equalsIgnoreCase(tagName)
                || "noscript".equalsIgnoreCase(tagName)) {
            incrementImgScore(node, -10);
        }else if ("a".equalsIgnoreCase(tagName)){
        	if(node.hasAttr("href") && !isEmpty(node.absUrl("href"))){
        		String imgName = node.attr("href");
        		/*
        		 * 如果a标签href与图片相同可以加分
        		 */
        		if(node.children().hasAttr("src") && node.getElementsByAttributeValue("src", imgName).size() != 0){
        			incrementImgScore(node, 20);
        			//System.out.println("img src and a href are same");
        		}
        		else if (imgName.endsWith(".jpg") || imgName.endsWith(".jpeg")  
        				|| imgName.endsWith(".gif")  
                        || imgName.endsWith(".png")){
        			incrementImgScore(node, 10);
        		}
        		else if(imgName.indexOf(".jpg?") > -1
        				|| imgName.indexOf(".jpeg?") > -1  
        				|| imgName.indexOf(".gif?") > -1
                        || imgName.indexOf(".png?") > -1){
        			incrementImgScore(node, 5);
        		}else{
        			incrementImgScore(node, -150);
        		}
        	}
        }else if("body".equalsIgnoreCase(tagName)){
        	return;
        }
        /*
         * 根据style属性“text-align: center”，align="center"，class属性， 正文属性给分, display:none 扣分
         */
        int attrScore = 0;
        String styleAttr = node.attr("style");
        if(!isEmpty(styleAttr)){
        	Matcher imgCandidatesMatcher = Patterns.get(
        		Patterns.RegEx.IMGPARENT_CANDIDATES).matcher(
        				styleAttr);
            if(imgCandidatesMatcher.find()){
                attrScore += 10;
            }
            Matcher imgUnlikelyCandidatesMatcher = Patterns.get(
                Patterns.RegEx.IMG_UNLIKELY_CANDIDATES).matcher(
                		styleAttr);
            if(imgUnlikelyCandidatesMatcher.find()){
                attrScore -= 200;
            }
        }
        
        String alignAttr = node.attr("align");
        if(!isEmpty(alignAttr)){
        	Matcher imgCandidatesMatcher1 = Patterns.get(
                    Patterns.RegEx.IMGPARENT_CANDIDATES).matcher(
                    		alignAttr);
            if(imgCandidatesMatcher1.find()){
            	attrScore += 10;
            }
            else{
            	attrScore -= 10;
            }
        }
        
        /* 根据class属性加减分数 */
        /* 根据标签class id属性值计算得分: 匹配NEGATIVE减15分, 匹配POSITIVE 加15分*/
        String className = node.className() + node.id();
        if (!isEmpty(className)) {
            Matcher negativeMatcher = Patterns.get(Patterns.RegEx.NEGATIVE_IMG)
                    .matcher(className);
            Matcher positiveMatcher = Patterns.get(Patterns.RegEx.POSITIVE_IMG)
                    .matcher(className);
            Matcher removeMatcher = Patterns.get(Patterns.RegEx.REMOVE_IMG)
                    .matcher(className);
            if (negativeMatcher.find()) {
            	attrScore -= 15;
            }
            if (positiveMatcher.find()) {
            	attrScore += 15;
            }
            if(removeMatcher.find()){
            	attrScore -= 40;
            }
        }
        
        int contentScore = getContentScore(node);
        incrementImgScore(node, attrScore + contentScore);
    }
    /**
     * 初始化img标签的分数
     * 评价维度：src, alt, title, width and height, align, href
     * 
     * @param node 
     */
    private static void initImgTagScore(Element node) {
    	/* 
         * 为img本身特点加分
         * 1. 为src属性加分
         * 2. alt，title评分
         * 3. 检查长宽属性
         * 4. 检查align属性
         * 5. href 减分
         */
        
        /*
         * 1. 获取现在时间,匹配年份月份，如果日期能匹配上，多加分
         * 2019-06 2019/06 201906 三种匹配方式
         * 2. negative： logo|weixin|sina|weibo|qq|flashplayer
         * 3. positive: content|photo
         * 
         */
    	
    	int imgScore = 0;
    	String srcImg = null;
    	if (node.hasAttr("data-src") && !isEmpty(node.attr("data-src"))){
    		srcImg = node.attr("abs:data-src");
    	}else if (!isEmpty(node.attr("src"))){
    		srcImg = node.attr("abs:src");
    	}
    	
    	String imgTime = new TimeUtil().getDateFromUrl(srcImg);
    	if (!isEmpty(srcImg)) {
            Matcher negativeMatcher = Patterns.get(Patterns.RegEx.NEGATIVE_IMG)
                    .matcher(srcImg);
            Matcher positiveMatcher = Patterns.get(Patterns.RegEx.POSITIVE_IMG)
                    .matcher(srcImg);
            Matcher removeMatcher = Patterns.get(Patterns.RegEx.REMOVE_IMG)
                    .matcher(srcImg);
            if (negativeMatcher.find()) {
            	imgScore -= 25;
            }
            if (positiveMatcher.find()) {
            	imgScore += 25;
            }
            if(removeMatcher.find()){
            	System.out.println(srcImg);
            	imgScore -= 60;
            }
            System.out.println("src attr score: " + imgScore);
            /*
             * 检测时间距离
             */
            if(!isEmpty(imgTime)){
            	//System.out.println("get img time: "+imgTime);
            	Date imgDate = string2Date(imgTime);
            	//System.out.println("url: " + node.root().baseUri());
            	String pubTime = new TimeUtil().getDateFromUrl(node.root().baseUri());
            	if(!isEmpty(pubTime)){
            		//System.out.println("get pub time: "+pubTime);
            		Date pubDate = string2Date(pubTime);
            		if(imgDate != null && pubDate != null){
            			int differentDays = timeDifference(imgDate, pubDate);
            			if(differentDays > 15 && differentDays < 30){
            				imgScore -= differentDays;
            			}else if(differentDays > 30){
            				imgScore -= 40;
            			}else if(differentDays >= 0 && differentDays < 1){
            				imgScore += 30;
            			}else if(differentDays >= 0 && differentDays < 3){
            				imgScore += 20;
            			}else if(differentDays >= 0 && differentDays < 7){
            				imgScore += 10;
            			}
            		}
            	}else{
            		
            		Date date = new Date();
            		if(imgDate != null && date != null){
            			int differentDays = timeDifference(imgDate, date);
            			if(differentDays > 15 && differentDays < 30){
            				imgScore -= differentDays / 3;
            			}else if(differentDays > 30){
            				imgScore -= 20;
            			}else if(differentDays >= 0 && differentDays < 1){
            				imgScore += 30;
            			}else if(differentDays >= 0 && differentDays < 3){
            				imgScore += 20;
            			}else if(differentDays >= 0 && differentDays < 7){
            				imgScore += 10;
            			}
            		}
            	}
            	System.out.println("src time score: " + imgScore);
            }
        }else{// 没有src属性
        	imgScore -= 500;
        }
    	System.out.println("src score: " + imgScore);
    	/*
    	 * TODO: 考虑title, alt包含标题的情况
    	 */
    	String altAttr = node.attr("alt") + node.attr("title");
    	if (!isEmpty(altAttr) && altAttr.length() < 30) {
            Matcher negativeMatcher = Patterns.get(Patterns.RegEx.NEGATIVE_IMG)
                    .matcher(altAttr);
            Matcher positiveMatcher = Patterns.get(Patterns.RegEx.POSITIVE_IMG)
                    .matcher(altAttr);
            Matcher removeMatcher = Patterns.get(Patterns.RegEx.REMOVE_IMG)
                    .matcher(srcImg);
            if (negativeMatcher.find()) {
            	imgScore -= 10;
            }
            if (positiveMatcher.find()) {
            	imgScore += 10;
            }
            if(removeMatcher.find()){
            	imgScore -= 30;
            }
        }
    	System.out.println("title and alt score: " + imgScore);
    	
    	/*
    	 * 长宽属性一般只减分（考虑到有可能噪声图片有长宽属性，但正文图片没有）
    	 * 当有属性特别大的时候酌情加分
    	 */
    	/*
    	 * 长宽有4种写法： 数字， 数字加px Px pX PX, 百分数，auto;
    	 * 是百分数不要，去掉px
    	 */
    	String width = node.attr("width").replaceAll("auto", "");
    	String height = node.attr("height").replaceAll("auto", "");
    	if(!isEmpty(width) && !isEmpty(height) && width.indexOf("%") == -1 && height.indexOf("%") == -1){
    		String widthReplace = width.replaceAll("[pP][xX]", "");
    		String heightReplace = height.replaceAll("[pP][xX]", "");
    		int widthLength = Integer.parseInt(widthReplace);
    		int heightLength = Integer.parseInt(heightReplace);
    		if(widthLength < 100 && heightLength < 100){
    			imgScore -= 40;
    		}else if(widthLength < 100 || heightLength < 100){
    			imgScore -= 25;
    		}else if(widthLength < 150 && heightLength < 150){
    			imgScore -= 20;
    		}else if(widthLength < 150 || heightLength < 150){
    			imgScore -= 10;
    		}else if(widthLength < 200 && heightLength < 200){
    			imgScore -= 10;
    		}else if(widthLength > 400 && heightLength > 400){
    			imgScore += 25;
    		}else if(widthLength > 350 && heightLength > 350){
    			imgScore += 20;
    		}else if(widthLength > 300 && heightLength > 300){
    			imgScore += 15;
    		}else if(widthLength > 300 || heightLength > 300){
    			imgScore += 10;
    		}
    	}
    	else if(!isEmpty(width) && width.indexOf("%") == -1){
    		String widthReplace = width.replaceAll("[pP][xX]", "");
    		int widthLength = Integer.parseInt(widthReplace);
    		if(widthLength < 100){
    			imgScore -= 25;
    		}else if(widthLength < 150){
    			imgScore -= 15;
    		}else if(widthLength > 400){
    			imgScore += 15;
    		}
    	}else if(!isEmpty(height) && height.indexOf("%") == -1){
    		String heightReplace = height.replaceAll("[pP][xX]", "");
    		int heightLength = Integer.parseInt(heightReplace);
    		if(heightLength < 100){
    			imgScore -= 25;
    		}else if(heightLength < 150){
    			imgScore -= 15;
    		}else if(heightLength > 400){
    			imgScore += 15;
    		}
    	}else if((!isEmpty(width) && width.indexOf("%") >= 0) || (!isEmpty(height) && height.indexOf("%") >= 0)){
    		imgScore -= 100;//基本判定不是图片
    	}
    	System.out.println("width and height score: " + imgScore);
    	 /*
         * 根据align="center"给分
         */
    	 String alignAttr = node.attr("align");
         if(!isEmpty(alignAttr)){
         	Matcher imgCandidatesMatcher1 = Patterns.get(
                     Patterns.RegEx.IMGPARENT_CANDIDATES).matcher(
                     		alignAttr);
             if(imgCandidatesMatcher1.find()){
            	 imgScore += 10;
             }
             else{
            	 imgScore -= 10;
             }
         }
         System.out.println("align score: " + imgScore);
         
         /*
          * href 减分
          */
         if(node.hasAttr("href") && !isEmpty(node.attr("href"))){
        	 imgScore -= 200;
         }
         System.out.println("href score: " + imgScore);
    	
         incrementImgScore(node, imgScore);
    }
    
    /*
     * 将yyyy-mm-dd格式字符串转换成date
     */
    private static Date string2Date(String time){
    	SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
		Date date = null;
		try {
			date= sdf.parse(time);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return date;
    	
    }
    
    /*
     * 比较两个date的天数差距
     */
    private static int timeDifference(Date date1, Date date2){
    	int days = (int) ((date2.getTime() - date1.getTime()) / (1000*3600*24));
        return days;    	
    }
    
    private static Element checkStrong(Element node){
    	if(node.tagName().equalsIgnoreCase("strong")){
    		if(node.hasParent()){
    			return node.parent();
    		}
    	}
    	return node;
    }
    
    

    /**
     * 根据元素的内容得分、类名class,id 元素类型等计算正文
     * 
     * @param preserveUnlikelyCandidates
     * @return
     */
    protected Element grabArticle(boolean preserveUnlikelyCandidates) {
        /**
		 * 垃圾节点看起来很粗糙（就像类名称“comment”等），
		 * 并将div转换为p标记使用不当（例如，它们不包含其他块级元素。）
		 * 
         **/
        for (Element node : mDocument.getAllElements()) {
        	/* 删除RegEx中定义的标签class、id含有相关垃圾属性的标签  */
            if (!preserveUnlikelyCandidates) {
                String unlikelyMatchString = node.className() + node.id();
                Matcher unlikelyCandidatesMatcher = Patterns.get(
                        Patterns.RegEx.UNLIKELY_CANDIDATES).matcher(
                        unlikelyMatchString);
                Matcher maybeCandidateMatcher = Patterns.get(
                        Patterns.RegEx.OK_MAYBE_ITS_A_CANDIDATE).matcher(
                        unlikelyMatchString);
                if (unlikelyCandidatesMatcher.find()
                        && !maybeCandidateMatcher.find()
                        && !"body".equalsIgnoreCase(node.tagName())) {
                    node.remove();
                    dbg("Removing unlikely candidate - " + unlikelyMatchString);
                    continue;
                }
            }

            /*
             * 将除了RegEx.DIV_TO_P_ELEMENTS之外的标签元素全部改为P
             */
            // Q:为什么要把div转换成p啊？待思考
            if ("div".equalsIgnoreCase(node.tagName())) {
                Matcher matcher = Patterns
                        .get(Patterns.RegEx.DIV_TO_P_ELEMENTS).matcher(
                                node.html());
                if (!matcher.find()) {
                    //dbg("Alternating div to p: " + node);
                    try {
                        node.tagName("p");
                    } catch (Exception e) {
                        dbg("Could not alter div to p, probably an IE restriction, reverting back to div.",
                                e);
                    }
                }
            }
        }

        /**
         * 遍历所有段落标签(P),将计算的分数增加到各自父节点
         * 分数是由标签名称、class、id属性值、该node元素下文本的权重分数计算得出
         */
        Elements allParagraphs = mDocument.getElementsByTag("p");
        ArrayList<Element> candidates = new ArrayList<Element>();

        for (Element node : allParagraphs) {
            Element parentNode = node.parent();
            Element grandParentNode = parentNode.parent();
            String innerText = getInnerText(node, true);

            /*
             * 如果该node元素下纯文本(去除空白字符)长度小于25个,则跳过计算
             */
            if (innerText.length() < 25) {
                continue;
            }

            /* 初始化node元素的父级标签分数权重值  */
            if (!parentNode.hasAttr(CONTENT_SCORE)) {
                initializeNode(parentNode);
                candidates.add(parentNode);
            }

            /* 初始化node元素的父级的父级标签分数权重值  */
            if (!grandParentNode.hasAttr(CONTENT_SCORE)) {
                initializeNode(grandParentNode);
                candidates.add(grandParentNode);
            }

            int contentScore = 0;

            /* 为段落本身添加一个点作为基础分数(p标签) */
            contentScore++;

            /* 为本段落中的任何逗号增加与逗号个数相应的分数 */
            contentScore += innerText.split(",|，").length;

            /*
             * 为该node元素下每100个字符(包含空白字符)增加最多3分
             */
            contentScore += Math.min(Math.floor((double)innerText.length() / 100), 3);

            /* 将计算的父级元素权重分数添加至元素中, 祖父级元素获取父级元素1/2权重 */
            incrementContentScore(parentNode, contentScore);
            incrementContentScore(grandParentNode, contentScore / 2);
        }
        
        try {
			File newsFile = new File("test/newsHTML.txt");
			FileUtils.writeStringToFile(newsFile, mDocument.html(), false);
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        
        

        /**
         * 计算最终的比例因子的出正文权重分数,并得出分数最高的元素 
         */
        Element topCandidate = null;
        for (Element candidate : candidates) {
        	/**
        	 * 根据链接密度调整最终正文候选标签的权重分数
        	 * 内容应具有相对较小的链接密度（5%或更小）
        	 */
            scaleContentScore(candidate, 1 - getLinkDensity(candidate));
            dbg("Candidate: (" + candidate.className() + ":" + candidate.id()
                    + ") with score " + getContentScore(candidate));
            if (topCandidate == null
                    || getContentScore(candidate) > getContentScore(topCandidate)) {
                topCandidate = candidate;
            }
        }
        /*
         * grabImg function（获取URL）
         * 方案一：直接拿html里面所有的img标签，打分
         */
        this.grabImg(mDocument, topCandidate);

        /**
         * 如果计算最高得分元素为空或元素是Body块,则新创建一个块元素并将原始HTML放入，重新初始化节点分数
         */
        if (topCandidate == null
                || "body".equalsIgnoreCase(topCandidate.tagName())) {
            topCandidate = mDocument.createElement("div");
            topCandidate.html(mDocument.body().html());
            mDocument.body().html("");
            mDocument.body().appendChild(topCandidate);
            initializeNode(topCandidate);
        }

        /**
         * 遍历得分最高的元素, 判断该元素的同胞是否满足要求
         * 比如序言，内容
         */
        Element articleContent = mDocument.createElement("div");
        articleContent.attr("id", "readability-content");
        int siblingScoreThreshold = Math.max(10,
                (int) (getContentScore(topCandidate) * 0.2f));
        Elements siblingNodes = topCandidate.parent().children();
        for (Element siblingNode : siblingNodes) {
            boolean append = false;

            dbg("Looking at sibling node: (" + siblingNode.className() + ":"
                    + siblingNode.id() + ")" + " with score "
                    + getContentScore(siblingNode));

            if (siblingNode == topCandidate) {
                append = true;
            }

            if (getContentScore(siblingNode) >= siblingScoreThreshold) {
                append = true;
            }

            if ("p".equalsIgnoreCase(siblingNode.tagName())) {
                float linkDensity = getLinkDensity(siblingNode);
                String nodeContent = getInnerText(siblingNode, true);
                int nodeLength = nodeContent.length();

                if (nodeLength > 80 && linkDensity < 0.25f) {
                    append = true;
                } else if (nodeLength < 80 && linkDensity == 0.0f
                        && nodeContent.matches(".*\\.( |$).*")) {
                    append = true;
                }
            }

            if (append) {
                dbg("Appending node: " + siblingNode);
                articleContent.appendChild(siblingNode);
                continue;
            }
        }


        /*
         * 最后清理可疑节点
         */
        prepArticle(articleContent);

        return articleContent;
    }
    
    /**
     * 获取元素e下所有图片标签，打分, 根据分数确定正文图片，存取图片到pictext中
     * 
     * @param document
     * @param text
     */
    public void grabImg(Element document, Element text){
    	Elements imgTag = document.getElementsByTag("img");
    	int ImgNumber = imgTag.size();
    	dbg("HTML一共有"+ ImgNumber +"张图片");
    	if(ImgNumber == 0){
    		return;
    	}
    	int textScore = 1000;
    	if(text != null){
    		textScore = getContentScore(text);
    		if(textScore > 50 && text.hasParent() && getContentScore(text.parent()) > 0.7 * textScore){
            	text = text.parent();
            }
        	textScore = getContentScore(text);
    	}
    	dbg("正文节点: "+ text.id()+text.className() +"\n分数: " +textScore);
    	
    	/**
         * 遍历所有图片标签,将父节点的分数增加到自身身上
         * 分数是由class、id，src属性值
         */
        ArrayList<Element> candidates = new ArrayList<Element>();

        for (Element node : imgTag) {
        	/*
        	 * 判断img是不是base64，如果是,drop it
        	 */
        	if((node.hasAttr("src") && !isEmpty(node.attr("src")) && !node.attr("src").startsWith("data:image"))
        			|| (node.hasAttr("data-src") && !isEmpty(node.attr("data-src")) && !node.attr("data-src").startsWith("data:image"))){
        		candidates.add(node);
            	Element parentNode = null, grandParentNode = null, greatGrandParentNode = null;
            	/*
            	 * 检测标签是否为strong，如果是就忽略取父节点
            	 */
            	if(node.hasParent()){
            		parentNode = checkStrong(node.parent());
            	}
            	if(parentNode.hasParent())
            		grandParentNode = checkStrong(parentNode.parent());
                /*
                 * 检测两个标签属性是否相同，相同就忽略取父节点
                 */
                if(parentNode.tagName().equalsIgnoreCase(grandParentNode.tagName())
                		&& parentNode.attributes() != null 
                		&& parentNode.attributes().equals(grandParentNode.attributes())){
                	grandParentNode = grandParentNode.parent();
                }
                if(grandParentNode.hasParent())
                	greatGrandParentNode = grandParentNode.parent();
                
                if(greatGrandParentNode != null && greatGrandParentNode.tagName().equalsIgnoreCase(grandParentNode.tagName())
                		&& greatGrandParentNode.attributes() != null 
                		&& greatGrandParentNode.attributes().equals(grandParentNode.attributes())){
                	greatGrandParentNode = greatGrandParentNode.parent();
                }


                /* 初始化node元素的父级标签分数权重值  */
                if (!parentNode.hasAttr(IMG_SCORE)) {
                    initializeImgScore(parentNode);
                }

                /* 初始化node元素的父级的父级标签分数权重值  */
                if (!grandParentNode.hasAttr(IMG_SCORE)) {
                    initializeImgScore(grandParentNode);
                }

    			  /* 初始化node元素的父级的父级的父级标签分数权重值  */
          	  	if (!greatGrandParentNode.hasAttr(IMG_SCORE)) {
          	  		initializeImgScore(greatGrandParentNode);
          	  	}
          	  
          	  	/* 初始化node元素的分数 */
          	  	if(!node.hasAttr(IMG_SCORE)){
          	  		node.attr(IMG_SCORE, Integer.toString(0));
          	  		initImgTagScore(node);  
          	  	}
          	  	
          	  	System.out.println("init: "+node.attr("abs:src")+" : "+getImgScore(node));
                /*
                 * 反其道而行之，将父级元素权重分数添加至img元素中
                 */
                incrementImgScore(node, getImgScore(parentNode));
                incrementImgScore(node, getImgScore(grandParentNode));
                incrementImgScore(node, getImgScore(greatGrandParentNode) / 2);
                /*
                 * 考虑标签层级过多问题：
                 * 1. 当img标签分数较高，检测父节点的子节点个数，如果都为1或2，可以继续向上寻找正文节点
                 */
                if(getImgScore(node) >= 30 && getContentScore(greatGrandParentNode) + getContentScore(grandParentNode) == 0 
                		&& grandParentNode.siblingElements().size() + greatGrandParentNode.siblingElements().size() < 1){
                	if(getContentScore(greatGrandParentNode.parent()) > 0){ // 判断条件threshold可根据正文分数调整
                		incrementImgScore(node, getImgScore(greatGrandParentNode.parent()) / 2);
                	}else if(getContentScore(greatGrandParentNode.parent().parent()) > 0){
                		incrementImgScore(node, getImgScore(greatGrandParentNode.parent().parent()) / 2);
                	}
                }
                System.out.println("add parent score: "+node.attr("abs:src")+" : "+getImgScore(node));
        	}
        	
        }

        /*
         * 获取分数最高的标签topCandidate
         */
        Element topCandidate = null;
        for (Element candidate : candidates) {
//            dbg("Candidate: (" + candidate.attr("src")+ ":" + candidate.id()
//                    + ") with score " + getImgScore(candidate));
            if (topCandidate == null
                    || getImgScore(candidate) > getImgScore(topCandidate)) {
                topCandidate = candidate;
            }
        }
        int topScore = getImgScore(topCandidate);
        dbg("TopCandidate: (" + topCandidate.attr("src")+ ":" + topCandidate.id()
        	+ ") with score " + topScore);
        if(topScore < 30){// 可根据情况调整
        	return ;
        }
        
        /*
         * 如果topCandidate在正文节点下
         * 与topCandidate同一父节点，祖父节点，曾祖父节点，同一深度加分
         * 或者换成同一深度检测，视效果而定
         * 曾祖父节点加5分，祖父加10分（猜测正文图片在某个节点下有共同的深度）
         */
        
        if(text != null && text.getElementsByTag("img").contains(topCandidate)){
        	Element parentNode = topCandidate.parent();
            Element grandParentNode = parentNode.parent();
            
            if(parentNode.tagName().equalsIgnoreCase(grandParentNode.tagName())
            		&& parentNode.attributes() != null && parentNode.attributes().equals(grandParentNode.attributes())){
            	grandParentNode = grandParentNode.parent();
            }
            Element greatGrandParentNode = grandParentNode.parent();
            if(greatGrandParentNode.tagName().equalsIgnoreCase(grandParentNode.tagName())
            		&& greatGrandParentNode.attributes() != null && greatGrandParentNode.attributes().equals(grandParentNode.attributes())){
            	greatGrandParentNode = greatGrandParentNode.parent();
            }
            int addScoreTimes = 0;
            Elements greatGrandParentImgTag = greatGrandParentNode.getElementsByTag("img");
            for(Element node : greatGrandParentImgTag){
            	if(node.parent().parent().parent().equals(greatGrandParentNode)){
            		incrementImgScore(node, 5);
            		addScoreTimes++;
            		if(node.parent().parent().equals(grandParentNode)){
            			incrementImgScore(node, 10);
            		}
            	}
            }
            if(addScoreTimes == 1){
            	incrementImgScore(topCandidate, -15);
            }
        }else{
        	/*
        	 * 原因：
        	 * 1. 正文和图片分离
        	 * 2. 正文节点错误
        	 * 3. 正文无图片
        	 */
        	if(text != null)
        		dbg("警告：分数最高的节点不在正文节点下\n"
        			+"正文节点id, className： " + text.id() + " ; " + text.className());
        }
        /*
         * 根据分数确定正文图片        
         */
        /*
         * TODO： check if TopCandidate or selected candidates are the photos in the news
         * Method: 
         * 1. Whether img tag is between title and article and under same parent node
         * 2. if image score under low threshold, drop them
         * 3. if image score under high threshold, check the tag of topCandidate
         * 4. analyze the distribution of scores
         */
        topScore = getImgScore(topCandidate);
        if(topScore > 50){//根据经验确定，还可以调整
        	for(Element candidate : candidates){
        		String srcImg = null;
            	if (candidate.hasAttr("data-src") && !isEmpty(candidate.attr("data-src"))){
            		srcImg = candidate.attr("abs:data-src");
            	}else if (!isEmpty(candidate.attr("src"))){
            		srcImg = candidate.attr("abs:src");
            	}
        		if(topScore < 100){
        			if(getImgScore(candidate) > 0.75 * topScore && topScore - getImgScore(candidate) <= 20){
                		dbg("Candidate: (" + srcImg+ ":" + candidate.id()
                			+ ") with score " + getImgScore(candidate));
                			pictext.add(srcImg);       		
                	}
        		}else{
        			if(getImgScore(candidate) > topScore - Math.min(textScore/3, 0.2 * topScore)){
                		dbg("Candidate: (" + srcImg+ ":" + candidate.id()
                			+ ") with score " + getImgScore(candidate));
                		pictext.add(srcImg);        		
                	}
        		}
            }
        }
        
        
    }

    
	/**
     * 获取元素e下所有文本,包含空白字符
     * 
     * @param e
     * @param normalizeSpaces
     * @return
     */
    private static String getInnerText(Element e, boolean normalizeSpaces) {
        String textContent = e.text().trim();

        if (normalizeSpaces) {
            textContent = textContent.replaceAll(Patterns.REGEX_NORMALIZE, "");
        }

        return textContent;
    }

    /**
     * 获取字符串s在节点e中出现的次数
     * @param e
     * @param s
     * @return
     */
    private static int getCharCount(Element e, String s) {
        if (s == null || s.length() == 0) {
            s = ",";
        }
        return getInnerText(e, true).split(s).length;
    }

    /**
     * 递归清除元素中的 style 属性
     * @param e
     */
    private static void cleanStyles(Element e) {
        if (e == null) {
            return;
        }

        Element cur = e.children().first();
        e.removeAttr("style");
        while (cur != null) {
            cur.removeAttr("style");
            cleanStyles(cur);
            cur = cur.nextElementSibling();
        }
    }

    /**
     * 获取链接密度作为内容的百分比,就是链接内的文本量除以e节点文本量。
     * 
     * @param e
     * @return
     */
    private static float getLinkDensity(Element e) {
        Elements links = getElementsByTag(e, "a");
        int textLength = getInnerText(e, true).length();
        float linkLength = 0.0F;
        for (Element link : links) {
            linkLength += getInnerText(link, true).length();
        }
        return linkLength / textLength;
    }

    /**
     * 计算元素的class、id属性值计算权重
     * 
     * @param e
     * @return
     */
    private static int getClassWeight(Element e) {
        int weight = 0;

        /* 根据标签class属性值计算得分: 匹配NEGATIVE减25分, 匹配POSITIVE 加25分*/
        String className = e.className();
        if (!isEmpty(className)) {
            Matcher negativeMatcher = Patterns.get(Patterns.RegEx.NEGATIVE)
                    .matcher(className);
            Matcher positiveMatcher = Patterns.get(Patterns.RegEx.POSITIVE)
                    .matcher(className);
            if (negativeMatcher.find()) {
                weight -= 25;
            }
            if (positiveMatcher.find()) {
                weight += 25;
            }
        }

        /* 根据标签id属性值计算得分: 匹配NEGATIVE减25分, 匹配POSITIVE 加25分*/
        String id = e.id();
        if (!isEmpty(id)) {
            Matcher negativeMatcher = Patterns.get(Patterns.RegEx.NEGATIVE)
                    .matcher(id);
            Matcher positiveMatcher = Patterns.get(Patterns.RegEx.POSITIVE)
                    .matcher(id);
            if (negativeMatcher.find()) {
                weight -= 25;
            }
            if (positiveMatcher.find()) {
                weight += 25;
            }
        }

        return weight;
    }

    /**
     * 替换删除元素e中多个连续的br标签为1个
     * 
     * @param e
     */
    private static void killBreaks(Element e) {
        e.html(e.html().replaceAll(Patterns.REGEX_KILL_BREAKS, "<br />"));
    }

    /**
     * 清除所有tag元素,保留 Patterns.RegEx.VIDEO 元素
     * @param e
     * @param tag
     */
    private static void clean(Element e, String tag) {
        Elements targetList = getElementsByTag(e, tag);
        boolean isEmbed = "object".equalsIgnoreCase(tag)
                       || "embed".equalsIgnoreCase(tag)
                       || "iframe".equalsIgnoreCase(tag);

        for (Element target : targetList) {
            Matcher matcher = Patterns.get(Patterns.RegEx.VIDEO).matcher(
                    target.outerHtml());
            if (isEmbed && matcher.find()) {
                continue;
            }
            target.remove();
        }
    }

    /**
     * 根据内容长度、class属性、A链接密度、图片数量等判断tag元素的可能性,并进行清除
     * 
     * @param e
     * @param tag
     */
    private void cleanConditionally(Element e, String tag) {
        Elements tagsList = getElementsByTag(e, tag);

        /**
         * 计算节点中的权重进行删除
         */
        for (Element node : tagsList) {
            int weight = getClassWeight(node);

            dbg("Cleaning Conditionally (" + node.className() + ":" + node.id()
                    + ")" + getContentScore(node));

            if (weight < 0) {
                node.remove();
            } else if (getCharCount(node, ",") < 10) {
            	/**
            	 * 如果逗号不多，那么非段落元素多于段落或其他,则将其删除
            	*/
                int p = getElementsByTag(node, "p").size();
                int img = getElementsByTag(node, "img").size();
                int li = getElementsByTag(node, "li").size() - 100;
                int input = getElementsByTag(node, "input").size();

                int embedCount = 0;
                Elements embeds = getElementsByTag(node, "embed");
                for (Element embed : embeds) {
                    if (!Patterns.get(Patterns.RegEx.VIDEO)
                            .matcher(embed.absUrl("src")).find()) {
                        embedCount++;
                    }
                }

                float linkDensity = getLinkDensity(node);
                int contentLength = getInnerText(node, true).length();
                boolean toRemove = false;

                if (img > p) {
                    toRemove = true;
                } else if (li > p && !"ul".equalsIgnoreCase(tag)
                        && !"ol".equalsIgnoreCase(tag)) {
                    toRemove = true;
                } else if (input > Math.floor(p / 3)) {
                    toRemove = true;
                } else if (contentLength < 25 && (img == 0 || img > 2)) {
                    toRemove = true;
                } else if (weight < 25 && linkDensity > 0.2f) {
                    toRemove = true;
                } else if (weight > 25 && linkDensity > 0.5f) {
                    toRemove = true;
                } else if ((embedCount == 1 && contentLength < 75)
                        || embedCount > 1) {
                    toRemove = true;
                }

                if (toRemove) {
                    node.remove();
                }
            }
        }
    }

    /**
	 * 清除元素中的假标题
     * @param e
     */
    private static void cleanHeaders(Element e) {
        for (int headerIndex = 1; headerIndex < 7; headerIndex++) {
            Elements headers = getElementsByTag(e, "h" + headerIndex);
            for (Element header : headers) {
                if (getClassWeight(header) < 0
                        || getLinkDensity(header) > 0.33f) {
                    header.remove();
                }
            }
        }
    }

    /**
     * Print debug logs
     * 
     * @param msg
     */
    protected void dbg(String msg) {
        dbg(msg, null);
    }

    /**
     * Print debug logs with stack trace
     * 
     * @param msg
     * @param t
     */
    protected void dbg(String msg, Throwable t) {
        System.out.println(msg + (t != null ? ("\n" + t.getMessage()) : "")
                + (t != null ? ("\n" + t.getStackTrace()) : ""));
    }

    private static class Patterns {
        private static Pattern sUnlikelyCandidatesRe;
        private static Pattern sOkMaybeItsACandidateRe;
        private static Pattern sPositiveRe;
        private static Pattern sNegativeRe;
        private static Pattern sDivToPElementsRe;
        private static Pattern sVideoRe;
        private static Pattern sImgParentsCandidatesRe;
        private static Pattern sImgParentsUnlikelyCandidatesRe;
        private static Pattern sPositiveImgRe;
        private static Pattern sNegativeImgRe;
        private static Pattern sRemoveImgRe;
        
        
        private static final String REGEX_REPLACE_BRS = "(?i)(<br[^>]*>[ \n\r\t]*){2,}";
        private static final String REGEX_REPLACE_FONTS = "(?i)<(\\/?)font[^>]*>";// </font src=a >
        /* Java has String.trim() */
        // private static final String REGEX_TRIM = "^\\s+|\\s+$";
        private static final String REGEX_NORMALIZE = "\\s{2,}";
        private static final String REGEX_KILL_BREAKS = "(<br\\s*\\/?>(\\s|&nbsp;?)*){1,}";

        public enum RegEx {
            UNLIKELY_CANDIDATES, OK_MAYBE_ITS_A_CANDIDATE, POSITIVE, NEGATIVE, DIV_TO_P_ELEMENTS, VIDEO, 
            IMGPARENT_CANDIDATES, IMG_UNLIKELY_CANDIDATES, POSITIVE_IMG, NEGATIVE_IMG, REMOVE_IMG;
        }

        public static Pattern get(RegEx re) {
            switch (re) {
            case UNLIKELY_CANDIDATES: {
                if (sUnlikelyCandidatesRe == null) {
                    sUnlikelyCandidatesRe = Pattern
                            .compile(
                                    "combx|comment|disqus|foot|header|menu|meta|nav|rss|shoutbox|sidebar|sponsor",
                                    Pattern.CASE_INSENSITIVE);
                }
                return sUnlikelyCandidatesRe;
            }
            case OK_MAYBE_ITS_A_CANDIDATE: {
                if (sOkMaybeItsACandidateRe == null) {
                    sOkMaybeItsACandidateRe = Pattern.compile(
                            "and|article|body|column|main",
                            Pattern.CASE_INSENSITIVE);
                }
                return sOkMaybeItsACandidateRe;
            }
            case POSITIVE: {
                if (sPositiveRe == null) {
                    sPositiveRe = Pattern
                            .compile(
                                    "article|body|content|entry|hentry|page|pagination|post|text|main",
                                    Pattern.CASE_INSENSITIVE);
                }
                return sPositiveRe;
            }
            case NEGATIVE: {
                if (sNegativeRe == null) {
                    sNegativeRe = Pattern
                            .compile(
                                    "combx|comment|contact|foot|footer|footnote|link|media|meta|promo|related|scroll|shoutbox|sponsor|tags|widget|share|scan",
                                    Pattern.CASE_INSENSITIVE);
                }
                return sNegativeRe;
            }
            case DIV_TO_P_ELEMENTS: {
                if (sDivToPElementsRe == null) {
                    sDivToPElementsRe = Pattern.compile(
                            "<(a|blockquote|dl|div|img|ol|p|pre|table|ul)",
                            Pattern.CASE_INSENSITIVE);
                }
                return sDivToPElementsRe;
            }
            case VIDEO: {
                if (sVideoRe == null) {
                    sVideoRe = Pattern.compile(
                            "http:\\/\\/(www\\.)?(youtube|vimeo)\\.com",
                            Pattern.CASE_INSENSITIVE);
                }
                return sVideoRe;
            }
            case IMGPARENT_CANDIDATES: {
                if (sImgParentsCandidatesRe == null) {
                	sImgParentsCandidatesRe = Pattern.compile(
                			"center|text-align(\\s)?:(\\s)?center",
                            Pattern.CASE_INSENSITIVE);
                }
                return sImgParentsCandidatesRe;
            }
            case IMG_UNLIKELY_CANDIDATES: {
                if (sImgParentsUnlikelyCandidatesRe == null) {
                	sImgParentsUnlikelyCandidatesRe = Pattern.compile(
                			"display(\\s)?:(\\s)?none",
                            Pattern.CASE_INSENSITIVE);
                }
                return sImgParentsUnlikelyCandidatesRe;
            }
            case POSITIVE_IMG: {
                if (sPositiveImgRe == null) {
                    sPositiveImgRe = Pattern
                            .compile(
                                    "article|body|content|entry|hentry|image-container|main|news|page|photo|post|text",
                                    Pattern.CASE_INSENSITIVE);
                }
                return sPositiveImgRe;
            }
            case NEGATIVE_IMG: {
                if (sNegativeImgRe == null) {
                    sNegativeImgRe = Pattern
                            .compile(
                                    "bar|combx|cursor|link|pointer|promo|related|scroll|share|shoutbox|uppage|widget",
                                    Pattern.CASE_INSENSITIVE);
                }
                return sNegativeImgRe;
            }
            case REMOVE_IMG: {
                if (sRemoveImgRe == null) {
                	sRemoveImgRe = Pattern
                            .compile(
                                    "ad1|ad2|^[a-zA-Z]ads|advertise|comment|contact|flashplayer|foot|icon|logo"
                                    + "|meta|qq|qrcode|scan|sina|sponsor|tags|vote|weibo|weixin|点赞|分享",
                                    Pattern.CASE_INSENSITIVE);
                }
                return sRemoveImgRe;
            }
            
            }
            return null;
        }
    }

    /**
     * 获取元素节点分数
     * 
     * @param node
     * @return
     */
    private static int getContentScore(Element node) {
        try {
        	if(node != null && node.hasAttr(CONTENT_SCORE) && node.attr(CONTENT_SCORE) != null){
        		return Integer.parseInt(node.attr(CONTENT_SCORE));
        	}
        } catch (NumberFormatException e) {
            return 0;
        }
        return 0;
    }
    private static int getImgScore(Element node) {
        try {
            return Integer.parseInt(node.attr(IMG_SCORE));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
   


    /**
     * 增加或减少node元素的分数
     * 
     * @param node
     * @param increment
     * @return
     */
    private static Element incrementContentScore(Element node, int increment) {
        int contentScore = getContentScore(node);
        contentScore += increment;
        node.attr(CONTENT_SCORE, Integer.toString(contentScore));
        return node;
    }
    private static Element incrementImgScore(Element node, int increment) {
      	 int imgScore = getImgScore(node);
           imgScore += increment;
           node.attr(IMG_SCORE, Integer.toString(imgScore));
           return node;		
   	}

    /**
     * 根据比例因子 scale 计算元素node的正文权重分数
     * 
     * @param node
     * @param scale
     * @return
     */
    private static Element scaleContentScore(Element node, float scale) {
        int contentScore = getContentScore(node);
        contentScore *= scale;
        node.attr(CONTENT_SCORE, Integer.toString(contentScore));
        return node;
    }

    /**
     * 在元素e中获取标签tag元素,但是tag中不包含e元素
     * @param e
     * @param tag
     * @return
     */
    private static Elements getElementsByTag(Element e, String tag) {
        Elements es = e.getElementsByTag(tag);
        es.remove(e);
        return es;
    }

    /**
     * 参数字符串是否为空
     * 
     * @param s
     * @return
     */
    private static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

}