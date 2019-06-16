package testExample;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Readability {

    private static final String CONTENT_SCORE  = "readabilityContentScore";

    private final Document mDocument;
    private String mBodyCache;

    public Readability(String html) {
        super();
        mDocument = Jsoup.parse(html);
    }

    public Readability(String html, String baseUri) {
        super();
        mDocument = Jsoup.parse(html, baseUri);
    }

    public Readability(File in, String charsetName, String baseUri)
            throws IOException {
        super();
        mDocument = Jsoup.parse(in, charsetName, baseUri);
    }

    public Readability(URL url, int timeoutMillis) throws IOException {
        super();
        mDocument = Jsoup.parse(url, timeoutMillis);
    }

    public Readability(Document doc) {
        super();
        mDocument = doc;
    }

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
        Element articleContent = grabArticle(preserveUnlikelyCandidates);

        /**
         * If we attempted to strip unlikely candidates on the first run
         * through, and we ended up with no content, that may mean we stripped
         * out the actual content so we couldn't parse it. So re-run init while
         * preserving unlikely candidates to have a better shot at getting our
         * content out properly.
         */
        if (isEmpty(getInnerText(articleContent, false))) {
            if (!preserveUnlikelyCandidates) {
                mDocument.body().html(mBodyCache);
                init(true);
                return;
            } else {
                articleContent
                        .html("<p>Sorry, readability was unable to parse this page for content.</p>");
            }
        }

        /* Glue the structure of our document together. */
        innerDiv.appendChild(articleTitle);
        innerDiv.appendChild(articleContent);
        overlay.appendChild(innerDiv);

        /* Clear the old HTML, insert the new content. */
        mDocument.body().html("");
        mDocument.body().prependChild(overlay);
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
                || "th".equalsIgnoreCase(tagName)) {
            incrementContentScore(node, -5);
        }

        incrementContentScore(node, getClassWeight(node));
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
            if ("div".equalsIgnoreCase(node.tagName())) {
                Matcher matcher = Patterns
                        .get(Patterns.RegEx.DIV_TO_P_ELEMENTS).matcher(
                                node.html());
                if (!matcher.find()) {
                    dbg("Alternating div to p: " + node);
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

        /* 根据标签class属性值计算得分: 匹配NEGATIVE减25分, 匹配POSITIVE 加25分*/
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
        private static final String REGEX_REPLACE_BRS = "(?i)(<br[^>]*>[ \n\r\t]*){2,}";
        private static final String REGEX_REPLACE_FONTS = "(?i)<(\\/?)font[^>]*>";// </font src=a >
        /* Java has String.trim() */
        // private static final String REGEX_TRIM = "^\\s+|\\s+$";
        private static final String REGEX_NORMALIZE = "\\s{2,}";
        private static final String REGEX_KILL_BREAKS = "(<br\\s*\\/?>(\\s|&nbsp;?)*){1,}";

        public enum RegEx {
            UNLIKELY_CANDIDATES, OK_MAYBE_ITS_A_CANDIDATE, POSITIVE, NEGATIVE, DIV_TO_P_ELEMENTS, VIDEO;
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
                                    "article|body|content|entry|hentry|page|pagination|post|text",
                                    Pattern.CASE_INSENSITIVE);
                }
                return sPositiveRe;
            }
            case NEGATIVE: {
                if (sNegativeRe == null) {
                    sNegativeRe = Pattern
                            .compile(
                                    "combx|comment|contact|foot|footer|footnote|link|media|meta|promo|related|scroll|shoutbox|sponsor|tags|widget",
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
            return Integer.parseInt(node.attr(CONTENT_SCORE));
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